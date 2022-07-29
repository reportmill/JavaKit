/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javakit.parse.*;
import javakit.reflect.*;
import snap.web.WebFile;

/**
 * A class to provide code completion suggestions for a given JNode.
 */
public class JavaCompleter {

    // The node
    private JNode  _node;

    // The project
    private Project  _proj;

    // The list of suggestions
    List<JavaDecl> _list = new ArrayList<>();

    /**
     * Returns completion for JNode (should be JType or JIdentifier).
     */
    public JavaDecl[] getSuggestions(JNode aNode)
    {
        // Set node
        _node = aNode;

        // Get Node JFile, SourceFile
        JFile nodeFile = aNode.getFile();
        WebFile sourceFile = nodeFile.getSourceFile();
        if (sourceFile == null) {
            System.err.println("JavaCompleter: No SourceFile for node"); return new JavaDecl[0]; }

        // Get SourceFile Project
        _proj = Project.get(sourceFile);
        _proj = _proj != null ? _proj.getRootProject() : null;
        if (_proj == null) {
            System.err.println("JavaCompleter: No project for node"); return new JavaDecl[0]; }

        // Add suggestions for node
        if (aNode instanceof JType)
            getSuggestions((JType) aNode);
        else if (aNode instanceof JExprId)
            getSuggestions((JExprId) aNode);

        // Get receiving class and, if 2 letters or less, filter out suggestions that don't apply (unless none do)
        JavaClass receivingClass = getReceivingClass(aNode);
        if (receivingClass != null && _list.size() > 10 && aNode.getName().length() <= 2) {
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
     * Adds suggestions.
     */
    private void addDecl(JavaDecl aDecl)
    {
        if (aDecl == null)
            return;
        _list.add(aDecl);
    }

    /**
     * Adds a JavaDecl for object.
     */
    private void addJavaPackageForName(String aPackageName)
    {
        JavaDecl javaDecl = _node.getJavaDecl(aPackageName);
        addDecl(javaDecl);
    }

    /**
     * Find suggestions for JType.
     */
    private void getSuggestions(JType aJType)
    {
        // Get prefix from type name
        String prefix = aJType.getName();

        // Get class names for prefix
        ClassPathInfo classPathInfo = _proj.getClassPathInfo();
        List<String> classNamesForPrefix = classPathInfo.getClassNamesForPrefix(prefix);

        // Handle JType as AllocExpr
        JNode typeParent = aJType.getParent();
        if (typeParent instanceof JExprAlloc) {

            // Iterate over classNames and add constructors
            for (String className : classNamesForPrefix) {

                // Get class (skip if not found or not public)
                JavaClass javaClass = _proj.getJavaClassForName(className);
                if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers()))
                    continue;

                // Get Constructors
                List<JavaConstructor> constructors = javaClass.getConstructors();

                // Add constructors
                for (JavaConstructor constructor : constructors)
                    addDecl(constructor);
            }
        }

        // Handle normal JType
        else {
            for (String className : classNamesForPrefix) {
                JavaClass javaClass = _proj.getJavaClassForName(className);
                addDecl(javaClass);
            }
        }
    }

    /**
     * Find suggestions for JExprId.
     */
    private void getSuggestions(JExprId anId)
    {
        // Get prefix string
        String prefix = anId.getName();
        ClassPathInfo classPathInfo = _proj.getClassPathInfo();

        // If there is a parent expression, work from it
        JExpr parExpr = anId.getParentExpr();
        if (parExpr != null) {

            // Handle parent is Package: Add packages and classes with prefix
            if (parExpr instanceof JExprId && ((JExprId) parExpr).isPackageName()) {

                // Get parent package name
                JExprId parId = (JExprId) parExpr;
                String parPkgName = parId.getPackageName();

                // Get class names for classes in parent package with prefix
                List<String> packageClassNames = classPathInfo.getPackageClassNamesForPrefix(parPkgName, prefix);
                for (String className : packageClassNames) {
                    JavaClass javaClass = _proj.getJavaClassForName(className);
                    if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers())) continue;
                    addDecl(javaClass);
                }

                // Get package names for packages in parent package with prefix
                List<String> packageChildNames = classPathInfo.getPackageChildrenNamesForPrefix(parPkgName, prefix);
                for (String packageName : packageChildNames)
                    addJavaPackageForName(packageName);
            }

            // Handle anything else with a parent class
            else if (parExpr.getEvalType() != null) {

                // Get
                JavaType parExprEvalType = parExpr.getEvalType();
                JavaClass parExprEvalClass = parExprEvalType.getEvalClass();

                // Get fields for prefix and add
                List<JavaField> fieldsForPrefix = JavaClassUtils.getPrefixFields(parExprEvalClass, prefix);
                for (JavaField fieldDecl : fieldsForPrefix)
                    addDecl(fieldDecl);

                // Get methods for prefix and add
                List<JavaMethod> methodsForPrefix = JavaClassUtils.getPrefixMethods(parExprEvalClass, prefix);
                for (JavaMethod method : methodsForPrefix)
                    addDecl(method);
            }
        }

        // If no JExpr prefix, get variables with prefix
        else {

            // Get variables with prefix of name and add to suggestions
            List<JVarDecl> varDecls = anId.getVarDecls(prefix, new ArrayList<>());
            for (JVarDecl varDecl : varDecls)
                addDecl(varDecl.getDecl());

            // Add methods of enclosing class
            JClassDecl enclosingClassDecl = anId.getEnclosingClassDecl();
            JavaClass enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;
            while (enclosingClassDecl != null && enclosingClass != null) {
                List<JavaMethod> methodsForPrefix = JavaClassUtils.getPrefixMethods(enclosingClass, prefix);
                for (JavaMethod meth : methodsForPrefix)
                    addDecl(meth);
                enclosingClassDecl = enclosingClassDecl.getEnclosingClassDecl();
                enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;
            }

            // If starts with upper case or is greater than 3 chars, add classes with prefix that are public
            List<String> classNamesForPrefix = classPathInfo.getClassNamesForPrefix(prefix);
            for (String className : classNamesForPrefix) {
                JavaClass javaClass = _proj.getJavaClassForName(className);
                if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers())) continue;
                addDecl(javaClass);
            }

            // Add packages with prefix
            List<String> packageNamesForPrefix = classPathInfo.getPackageNamesForPrefix(prefix);
            for (String packageName : packageNamesForPrefix)
                addJavaPackageForName(packageName);
        }
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
        JExprMath assExpr = getExpression(aNode, JExprMath.Op.Assign);
        JExpr leftHandSide = assExpr != null ? assExpr.getOperand(0) : null;
        if (leftHandSide != null)
            return leftHandSide.getEvalClass();

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
     * Returns the expression for given node with given op, if available.
     */
    private static JExprMath getExpression(JNode aNode, JExprMath.Op anOp)
    {
        for (JNode n = aNode; n != null && !(n instanceof JStmt) && !(n instanceof JMemberDecl); n = n.getParent()) {
            if (n instanceof JExprMath) {
                JExprMath expr = (JExprMath) n;
                if (expr.op == anOp)
                    return expr;
            }
        }

        // Return not found
        return null;
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
     * Returns whether suggestion is receiving class.
     */
    private static final boolean isReceivingClassAssignable(JavaDecl aJD, JavaClass aRC)
    {
        return getReceivingClassAssignableScore(aJD, aRC) > 0;
    }

    /**
     * Returns whether suggestion is receiving class.
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

        // The receiving class for suggestions
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
        public int compare(JavaDecl o1, JavaDecl o2)
        {
            // Get whether either suggestion is of Assignable to ReceivingClass
            int rca1 = getReceivingClassAssignableScore(o1, _receivingClass);
            int rca2 = getReceivingClassAssignableScore(o2, _receivingClass);
            if (rca1 != rca2) return rca1 > rca2 ? -1 : 1;

            // If Suggestion Types differ, return by type
            if (o1.getType() != o2.getType())
                return getOrder(o1.getType()) < getOrder(o2.getType()) ? -1 : 1;

            // Handle Class compare
            if (o1 instanceof JavaClass) {

                // If either is member class, sort other first
                JavaClass class1 = (JavaClass) o1;
                JavaClass class2 = (JavaClass) o2;
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
            int c = o1.getSimpleName().compareToIgnoreCase(o2.getSimpleName());
            if (c != 0) return c;

            // Otherwise use full name
            return o1.getFullName().compareToIgnoreCase(o2.getFullName());
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