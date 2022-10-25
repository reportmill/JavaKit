/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javakit.parse.*;
import snap.parse.ParseToken;
import snap.util.StringUtils;

/**
 * A class to provide code completion suggestions for a given JNode.
 */
public class NodeCompleter {

    // The node
    private JNode  _node;

    // The resolver
    private Resolver  _resolver;

    // The list of completions
    List<JavaDecl> _list = new ArrayList<>();

    /**
     * Constructor.
     */
    public NodeCompleter()
    {
        super();
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

        // Get receiving class and more than 10 items, filter out completions that don't apply (unless none do)
        JavaClass receivingClass = getReceivingClass(aNode);
        if (receivingClass != null && _list.size() > 10 && aNode.getName().length() < 5) {
            Stream<JavaDecl> sugsStream = _list.stream();
            Stream<JavaDecl> sugsStreamAssignable = sugsStream.filter(p -> isReceivingClassAssignable(p, receivingClass));
            List<JavaDecl> sugsListAssignable = sugsStreamAssignable.collect(Collectors.toList());
            if (sugsListAssignable.size() > 0)
                _list = sugsListAssignable;
        }

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
        // Get prefix from type name
        String prefix = aJType.getName();
        Matcher prefixMatcher = StringUtils.getSkipCharsMatcherForLiteralString(prefix);

        // Get class names for prefix
        ClassTreeMatcher classPathMatcher = getClassTreeMatcher();
        String[] classNamesForMatcher = classPathMatcher.getClassNamesForPrefixMatcher(prefix, prefixMatcher);

        // Handle JType as AllocExpr
        JNode typeParent = aJType.getParent();
        if (typeParent instanceof JExprAlloc) {

            // Iterate over classNames and add constructors
            for (String className : classNamesForMatcher) {

                // Get class (skip if not found or not public)
                JavaClass javaClass = _resolver.getJavaClassForName(className);
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
            for (String className : classNamesForMatcher) {
                JavaClass javaClass = _resolver.getJavaClassForName(className);
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
        Matcher prefixMatcher = StringUtils.getSkipCharsMatcherForLiteralString(prefix);

        // Handle parent is Package: Add packages and classes with prefix
        if (parExpr instanceof JExprId && ((JExprId) parExpr).isPackageName()) {

            // Get parent package name
            JExprId parId = (JExprId) parExpr;
            String parPkgName = parId.getPackageName();

            // Get class names for classes in parent package with prefix
            ClassTreeMatcher classPathMatcher = getClassTreeMatcher();
            String[] packageClassNames = classPathMatcher.getPackageClassNamesForMatcher(parPkgName, prefixMatcher);
            for (String className : packageClassNames) {
                JavaClass javaClass = _resolver.getJavaClassForName(className);
                if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers())) continue;
                addCompletionDecl(javaClass);
            }

            // Get package names for packages in parent package with prefix
            String[] packageChildNames = classPathMatcher.getPackageChildrenNamesForMatcher(parPkgName, prefixMatcher);
            for (String packageName : packageChildNames)
                addJavaPackageForName(packageName);
        }

        // Handle anything else with a parent class
        else if (parExpr.getEvalType() != null) {

            // Get
            JavaType parExprEvalType = parExpr.getEvalType();
            JavaClass parExprEvalClass = parExprEvalType.getEvalClass();

            // Get fields for prefix and add
            List<JavaField> fieldsForMatcher = JavaClassUtils.getFieldsForMatcher(parExprEvalClass, prefixMatcher);
            for (JavaField fieldDecl : fieldsForMatcher)
                addCompletionDecl(fieldDecl);

            // Get methods for prefix and add
            List<JavaMethod> methodsForMatcher = JavaClassUtils.getMethodsForMatcher(parExprEvalClass, prefixMatcher);
            for (JavaMethod method : methodsForMatcher)
                addCompletionDecl(method);
        }
    }

    /**
     * Find completions for any node (name/string)
     */
    private void getCompletionsForNodeString(JNode aNode)
    {
        // Get prefix
        String prefix = getNodeString(aNode);
        if (prefix == null)
            return;

        // Get prefix matcher
        Matcher prefixMatcher = StringUtils.getSkipCharsMatcherForLiteralString(prefix);

        // Get variables with prefix of name and add to completions
        List<JVarDecl> varDecls = aNode.getVarDeclsForMatcher(prefixMatcher, new ArrayList<>());
        for (JVarDecl varDecl : varDecls)
            addCompletionDecl(varDecl.getDecl());

        // Add methods of enclosing class
        JClassDecl enclosingClassDecl = aNode.getEnclosingClassDecl();
        JavaClass enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;
        while (enclosingClassDecl != null && enclosingClass != null) {
            List<JavaMethod> methodsForMatcher = JavaClassUtils.getMethodsForMatcher(enclosingClass, prefixMatcher);
            for (JavaMethod meth : methodsForMatcher)
                addCompletionDecl(meth);
            enclosingClassDecl = enclosingClassDecl.getEnclosingClassDecl();
            enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;
        }

        // If starts with upper case or is greater than 3 chars, add classes with prefix that are public
        ClassTreeMatcher classPathMatcher = getClassTreeMatcher();
        String[] classNamesForMatcher = classPathMatcher.getClassNamesForPrefixMatcher(prefix, prefixMatcher);
        for (String className : classNamesForMatcher) {
            JavaClass javaClass = _resolver.getJavaClassForName(className);
            if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers())) continue;
            addCompletionDecl(javaClass);
        }

        // Add packages with prefix
        String[] packageNamesForMatcher = classPathMatcher.getPackageNamesForMatcher(prefixMatcher);
        for (String packageName : packageNamesForMatcher)
            addJavaPackageForName(packageName);
    }

    /**
     * Returns a ClassTreeMatcher to find packages and classes for regex matcher.
     */
    private ClassTreeMatcher getClassTreeMatcher()
    {
        ClassPathInfo classPathInfo = _resolver.getClassPathInfo();
        return classPathInfo.getClassTreeMatcher();
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

        // Handle any node with only one token
        ParseToken startToken = aNode.getStartToken();
        if (startToken == aNode.getEndToken())
            return startToken.getString();

        // Return not found
        return null;
    }

    /**
     * Returns the assignable type of given node assuming it's the receiving expression of assign or a method arg.
     */
    private static JavaClass getReceivingClass(JNode aNode)
    {
        // If MethocCall arg, return arg class
        JavaType argType = getMethodCallArgType(aNode);
        if (argType != null)
            return argType.getEvalClass();

        // If node is Assign Right-Hand-Side, return assignment Left-Hand-Side class
        JExprAssign assExpr = aNode.getParent(JExprAssign.class);
        if (assExpr != null)
            return assExpr.getEvalClass();

        // If node is JVarDecl Initializer, return JVarDecl class
        JVarDecl initVarDecl = getVarDeclForInitializer(aNode);
        if (initVarDecl != null)
            return initVarDecl.getEvalClass();

        // If node is JExprMath, return op class
        JExprMath mathExpr = aNode.getParent(JExprMath.class);
        if (mathExpr != null) {
            switch (mathExpr.getOp()) {
                case Or:
                case And:
                case Not: return aNode.getJavaClassForClass(Boolean.class);
                default: return aNode.getJavaClassForClass(Double.class);
            }
        }

        // If node is expression and top parent is conditional statement, return boolean
        JExpr exp = aNode instanceof JExpr ? (JExpr) aNode : aNode.getParent(JExpr.class);
        if (exp != null) {
            while (exp.getParent() instanceof JExpr) exp = (JExpr) exp.getParent();
            JNode par = exp.getParent();
            if (par instanceof JStmtIf || par instanceof JStmtWhile || par instanceof JStmtDo)
                return aNode.getJavaClassForClass(Boolean.class);
        }

        // Return null since no assignment type found for class
        return null;
    }

    /**
     * Returns the method call parent of given node, if available.
     */
    private static JExprMethodCall getMethodCall(JNode aNode)
    {
        JNode node = aNode;
        while (node != null && !(node instanceof JStmt) && !(node instanceof JMemberDecl)) {
            if (node instanceof JExprMethodCall)
                return (JExprMethodCall) node;
            node = node.getParent();
        }
        return null;
    }

    /**
     * Return the method call arg class of node, if node is MethodCall arg.
     */
    private static JavaType getMethodCallArgType(JNode aNode)
    {
        // Get methodc all
        JExprMethodCall methodCall = getMethodCall(aNode);
        if (methodCall == null)
            return null;

        // Get Arg index for node
        int argIndex = getMethodCallArgIndex(methodCall, aNode);
        if (argIndex < 0)
            return null;

        // Get method
        JavaMethod method = methodCall.getDecl();
        if (method == null)
            return null;

        // Get arg type and return
        JavaType argType = argIndex < method.getParamCount() ? method.getParamType(argIndex) : null;
        return argType;
    }

    /**
     * Return the method call arg index of node.
     */
    private static int getMethodCallArgIndex(JExprMethodCall aMethodCall, JNode aNode)
    {
        // Get methodCall for node
        JExprMethodCall methodCall = aMethodCall != null ? aMethodCall : getMethodCall(aNode);
        if (methodCall == null) return -1;

        // Get args
        List<JExpr> args = methodCall.getArgs();

        // Iterate over args and return index if found
        JNode node = aNode;
        while (node != methodCall) {
            for (int i = 0, iMax = args.size(); i < iMax; i++)
                if (args.get(i) == node)
                    return i;
            node = node.getParent();
        }

        // Return not found
        return -1;
    }

    /**
     * Returns the JVarDecl for given node, if node is initializer.
     */
    private static JVarDecl getVarDeclForInitializer(JNode aNode)
    {
        JNode node = aNode;
        while (node != null && !(node instanceof JStmt) && !(node instanceof JMemberDecl)) {
            if (node instanceof JExpr) {
                JExpr expr = (JExpr) node;
                if (expr.getParent() instanceof JVarDecl) {
                    JVarDecl vd = (JVarDecl) expr.getParent();
                    if (vd.getInitializer() == expr)
                        return vd;
                }
            }
            node = node.getParent();
        }

        // Return not found
        return null;
    }

    /**
     * Returns whether completion is receiving class.
     */
    private static final boolean isReceivingClassAssignable(JavaDecl aJD, JavaClass aRC)
    {
        return getReceivingClassAssignableScore(aJD, aRC) > 0;
    }

    /**
     * Returns whether completion is receiving class.
     */
    private static final int getReceivingClassAssignableScore(JavaDecl aJD, JavaClass aRC)
    {
        // Ignore package or null
        if (aRC == null || aJD instanceof JavaPackage)
            return 0;

        // Get real class
        JavaClass evalClass = aJD.getEvalClass();
        if (evalClass == null)
            return 0;

        // If classes equal, return 2
        if (evalClass == aRC)
            return 2;

        // If assignable, return 1
        if (aRC.isAssignable(evalClass))
            return 1;

        // Return 0 since incompatible
        return 0;
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
            int recClassScore1 = getReceivingClassAssignableScore(decl1, _receivingClass);
            int recClassScore2 = getReceivingClassAssignableScore(decl2, _receivingClass);
            if (recClassScore1 != recClassScore2)
                return recClassScore1 > recClassScore2 ? -1 : 1;

            // If completion Types differ, return by type
            JavaDecl.DeclType declType1 = decl1.getType();
            JavaDecl.DeclType declType2 = decl2.getType();
            if (declType1 != declType2)
                return getOrder(declType1) < getOrder(declType2) ? -1 : 1;

            // Handle Class compare
            if (decl1 instanceof JavaClass) {

                // If either is member class, sort other first
                JavaClass class1 = (JavaClass) decl1;
                JavaClass class2 = (JavaClass) decl2;
                if (class1.isMemberClass() != class2.isMemberClass())
                    return class2.isMemberClass() ? -1 : 1;

                // Make certain packages get preference
                String className1 = class1.getClassName();
                String className2 = class2.getClassName();
                for (String prefPkg : PREF_PACKAGES) {
                    if (className1.startsWith(prefPkg) && !className2.startsWith(prefPkg)) return -1;
                    if (className2.startsWith(prefPkg) && !className1.startsWith(prefPkg)) return 1;
                }
            }

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
        public static final int getOrder(JavaDecl.DeclType aType)
        {
            switch (aType) {
                case VarDecl: return 0;
                case Field: return 1;
                case Method: return 2;
                case Class: return 3;
                case Package: return 4;
                default: return 5;
            }
        }
    }

    /**
     * List or preferred packages.
     */
    private static String PREF_PACKAGES[] = {"java.lang.", "java.util.", "snap.", "java."};

}