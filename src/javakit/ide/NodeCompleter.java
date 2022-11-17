/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.ide;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javakit.parse.*;
import javakit.resolver.*;
import snap.parse.ParseToken;

/**
 * A class to provide code completion suggestions for a given JNode.
 */
public class NodeCompleter {

    // The node
    private JNode  _node;

    // The resolver
    private Resolver _resolver;

    // An identifier matcher
    private static Matcher  _idMatcher;

    // The list of completions
    List<JavaDecl>  _list = new ArrayList<>();

    /**
     * Constructor.
     */
    public NodeCompleter()
    {
        super();

        // Create/set IdMatcher
        if (_idMatcher == null) {
            String regexStr = "[$_a-zA-Z][$\\w]*";
            Pattern pattern = Pattern.compile(regexStr);
            _idMatcher = pattern.matcher("");
        }
    }

    /**
     * Returns completion for JNode (should be JType or JIdentifier).
     */
    public JavaDecl[] getCompletionsForNode(JNode aNode)
    {
        // Set node
        _node = aNode;

        // Get SourceFile Project
        _resolver = aNode.getResolver();
        if (_resolver == null) {
            JFile jfile = aNode.getFile();
            JClassDecl classDecl = jfile.getClassDecl();
            String className = classDecl != null ? classDecl.getName() : "Unknown";
            System.err.println("JavaCompleter: No resolver for source file: " + className);
            return new JavaDecl[0];
        }

        // Add completions for node
        if (aNode instanceof JType)
            getCompletionsForType((JType) aNode);
        else if (aNode instanceof JExprId)
            getCompletionsForExprId((JExprId) aNode);
        else if (aNode.getStartToken() == aNode.getEndToken())
            getCompletionsForNodeString(aNode);
        else return new JavaDecl[0];

        // Get receiving class
        JavaClass receivingClass = ReceivingClass.getReceivingClass(aNode);

        // If receiving class and more than 10 items, filter out completions that don't apply (unless none do)
        if (receivingClass != null)
            _list = ReceivingClass.filterListForReceivingClass(_list, receivingClass);

        // Get array and sort
        JavaDecl[] decls = _list.toArray(new JavaDecl[0]);
        Arrays.sort(decls, new DeclCompare(receivingClass));
        return decls;
    }

    /**
     * Find completions for JType.
     */
    private void getCompletionsForType(JType aJType)
    {
        // Get prefix matcher from type name
        String prefix = aJType.getName();
        DeclMatcher prefixMatcher = new DeclMatcher(prefix);

        // Add word completions
        addWordCompletionsForMatcher(prefixMatcher);

        // Get all matching classes
        ClassTree classTree = getClassTree();
        ClassTree.ClassNode[] matchingClasses = prefixMatcher.getClassesForClassTree(classTree);

        // Handle JType as AllocExpr
        JNode typeParent = aJType.getParent();
        if (typeParent instanceof JExprAlloc) {

            // Iterate over classes and add constructors
            for (ClassTree.ClassNode matchingClass : matchingClasses) {

                // Get class (skip if not found or not public)
                JavaClass javaClass = _resolver.getJavaClassForName(matchingClass.fullName);
                if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers()))
                    continue;

                // Get Constructors
                List<JavaConstructor> constructors = javaClass.getConstructors();

                // Add constructors
                for (JavaConstructor constructor : constructors)
                    addCompletionDecl(constructor);
            }
        }

        // Handle normal JType
        else {
            for (ClassTree.ClassNode matchingClass : matchingClasses) {
                JavaClass javaClass = _resolver.getJavaClassForName(matchingClass.fullName);
                addCompletionDecl(javaClass);
            }
        }
    }

    /**
     * Find completions for JExprId.
     */
    private void getCompletionsForExprId(JExprId anId)
    {
        // Get parent expression - if none, forward to basic getCompletionsForNodeString()
        JExpr parExpr = anId.getParentExpr();
        if (parExpr == null) {
            getCompletionsForNodeString(anId);
            return;
        }

        // Get prefix string and matcher
        String prefix = anId.getName();
        DeclMatcher prefixMatcher = new DeclMatcher(prefix);

        // Add word completions
        addWordCompletionsForMatcher(prefixMatcher);

        // Handle parent is Package: Add packages and classes with prefix
        if (parExpr instanceof JExprId && ((JExprId) parExpr).isPackageName()) {

            // Get parent package name
            JExprId parId = (JExprId) parExpr;
            String parPkgName = parId.getPackageName();

            // Get matching classes for classes in parent package with prefix
            ClassTree classTree = getClassTree();
            ClassTree.ClassNode[] matchingClasses = prefixMatcher.getClassesForClassTreePackageName(classTree, parPkgName);

            // Iterate over matching classes and add public classes
            for (ClassTree.ClassNode matchingClass : matchingClasses) {
                JavaClass javaClass = _resolver.getJavaClassForName(matchingClass.fullName);
                if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers()))
                    continue;
                addCompletionDecl(javaClass);
            }

            // Get package names for packages in parent package with prefix
            ClassTree.PackageNode[] packageChildren = prefixMatcher.getChildPackagesForClassTreePackageName(classTree, parPkgName);
            for (ClassTree.PackageNode pkg : packageChildren)
                addJavaPackageForName(pkg.fullName);
        }

        // Handle anything else with a parent class
        else if (parExpr.getEvalType() != null) {

            // Get ParentExpr.EvalClass
            JavaType parExprEvalType = parExpr.getEvalType();
            JavaClass parExprEvalClass = parExprEvalType.getEvalClass();

            // Get matching fields for class and add
            List<JavaField> matchingFields = prefixMatcher.getFieldsForClass(parExprEvalClass);
            for (JavaField matchingField : matchingFields)
                addCompletionDecl(matchingField);

            // Get matching methods for class and add
            List<JavaMethod> matchingMethods = prefixMatcher.getMethodsForClass(parExprEvalClass);
            for (JavaMethod matchingMethod : matchingMethods)
                addCompletionDecl(matchingMethod);
        }
    }

    /**
     * Find completions for any node (name/string)
     */
    private void getCompletionsForNodeString(JNode aNode)
    {
        // Get prefix matcher
        String prefix = getNodeString(aNode);
        DeclMatcher prefixMatcher = prefix != null ? new DeclMatcher(prefix) : null;
        if (prefixMatcher == null)
            return;

        // Add word completions
        addWordCompletionsForMatcher(prefixMatcher);

        // Get variables with prefix of name and add to completions
        List<JVarDecl> varDecls = prefixMatcher.getVarDeclsForJNode(aNode, new ArrayList<>());
        for (JVarDecl varDecl : varDecls)
            addCompletionDecl(varDecl.getDecl());

        // Get enclosing class
        JClassDecl enclosingClassDecl = aNode.getEnclosingClassDecl();
        JavaClass enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;

        // Add methods of enclosing class
        while (enclosingClassDecl != null && enclosingClass != null) {
            List<JavaMethod> matchingMethods = prefixMatcher.getMethodsForClass(enclosingClass);
            for (JavaMethod matchingMethod : matchingMethods)
                addCompletionDecl(matchingMethod);
            enclosingClassDecl = enclosingClassDecl.getEnclosingClassDecl();
            enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;
        }

        // Get matching classes
        ClassTree classTree = getClassTree();
        ClassTree.ClassNode[] matchingClasses = prefixMatcher.getClassesForClassTree(classTree);

        // Iterate over classes and add if public
        for (ClassTree.ClassNode matchingClass : matchingClasses) {
            JavaClass javaClass = _resolver.getJavaClassForName(matchingClass.fullName);
            if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers())) continue;
            addCompletionDecl(javaClass);
        }

        // Get matching packages and add
        ClassTree.PackageNode[] matchingPackages = prefixMatcher.getPackagesForClassTree(classTree);
        for (ClassTree.PackageNode matchingPkg : matchingPackages)
            addJavaPackageForName(matchingPkg.fullName);
    }

    /**
     * Adds word completions for matcher.
     */
    private void addWordCompletionsForMatcher(DeclMatcher prefixMatcher)
    {
        // Add JavaWords
        for (JavaWord word : JavaWord.ALL)
            if (prefixMatcher.matchesString(word.getName()))
                addCompletionDecl(word);

        // Add Global Literals (true, false, null, this, super
        JavaLocalVar[] globalLiters = _resolver.getGlobalLiterals();
        for (JavaDecl literal : globalLiters)
            if (prefixMatcher.matchesString(literal.getName()))
                addCompletionDecl(literal);
    }

    /**
     * Returns the ClassTree for current Resolver.
     */
    private ClassTree getClassTree()
    {
        ClassPathInfo classPathInfo = _resolver.getClassPathInfo();
        return classPathInfo.getClassTree();
    }

    /**
     * Adds completion.
     */
    private void addCompletionDecl(JavaDecl aDecl)
    {
        if (aDecl == null) return;
        _list.add(aDecl);
    }

    /**
     * Adds a JavaDecl for object.
     */
    private void addJavaPackageForName(String aPackageName)
    {
        JavaDecl javaDecl = _node.getJavaPackageForName(aPackageName);
        addCompletionDecl(javaDecl);
    }

    /**
     * Returns a string for node.
     */
    private String getNodeString(JNode aNode)
    {
        // Handle simple Id node
        if (aNode instanceof JExprId)
            return aNode.getName();

        // Handle any node with only one token with id string
        ParseToken startToken = aNode.getStartToken();
        if (startToken == aNode.getEndToken()) {
            String str = startToken.getString();
            if (_idMatcher.reset(str).lookingAt())
                return str;
        }

        // Return not found
        return null;
    }

    /**
     * A Comparator to sort JavaDecls.
     */
    private static class DeclCompare implements Comparator<JavaDecl> {

        // The receiving class for completions
        private JavaClass  _receivingClass;

        /**
         * Creates a DeclCompare.
         */
        DeclCompare(JavaClass aRC)
        {
            _receivingClass = aRC;
        }

        /**
         * Standard compare to method.
         */
        public int compare(JavaDecl decl1, JavaDecl decl2)
        {
            // Get whether either completion is of Assignable to ReceivingClass
            int recClassScore1 = ReceivingClass.getReceivingClassAssignableScore(decl1, _receivingClass);
            int recClassScore2 = ReceivingClass.getReceivingClassAssignableScore(decl2, _receivingClass);
            if (recClassScore1 != recClassScore2)
                return recClassScore1 > recClassScore2 ? -1 : 1;

            // If order Types differ, return by type
            int declType1 = getOrder(decl1.getType());
            int declType2 = getOrder(decl2.getType());
            if (declType1 != declType2)
                return declType1 < declType2 ? -1 : 1;
//
//            // Handle Class compare
//            if (decl1 instanceof JavaClass) {
//
//                // If either is primitive, return that
//                JavaClass class1 = (JavaClass) decl1;
//                JavaClass class2 = (JavaClass) decl2;
//                if (class1.isPrimitive())
//                    return -1;
//                if (class2.isPrimitive())
//                    return 1;
//
//                // If either is member class, sort other first
//                if (class1.isMemberClass() != class2.isMemberClass())
//                    return class2.isMemberClass() ? -1 : 1;
//
//                // Make certain packages get preference
//                String className1 = class1.getClassName();
//                String className2 = class2.getClassName();
//                for (String prefPkg : PREF_PACKAGES) {
//                    if (className1.startsWith(prefPkg) && !className2.startsWith(prefPkg)) return -1;
//                    if (className2.startsWith(prefPkg) && !className1.startsWith(prefPkg)) return 1;
//                }
//            }

            // If simple names are unique, return order
            String simpleName1 = decl1.getSimpleName();
            String simpleName2 = decl2.getSimpleName();
            int simpleNameComp = simpleName1.compareToIgnoreCase(simpleName2);
            if (simpleNameComp != 0)
                return simpleNameComp;

            // Otherwise use full name
            String fullName1 = decl1.getFullName();
            String fullName2 = decl2.getFullName();
            return fullName1.compareToIgnoreCase(fullName2);
        }

        /**
         * Returns the type order.
         */
        public static int getOrder(JavaDecl.DeclType aType)
        {
            switch (aType) {
                case Word:
                case VarDecl: return 0;
                case Field:
                case Method: return 2;
                case Class: return 3;
                case Package: return 5;
                default: return 6;
            }
        }
    }

    /**
     * List or preferred packages.
     */
    private static String PREF_PACKAGES[] = {"java.lang.", "java.util.", "snap.", "java."};

}