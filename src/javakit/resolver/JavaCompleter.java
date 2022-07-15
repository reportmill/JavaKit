/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import javakit.parse.*;
import snap.util.*;

/**
 * A class to provide code completion suggestions for a given JNode.
 */
public class JavaCompleter {

    // The node
    JNode _node;

    // The list of suggestions
    List<JavaDecl> _list = new ArrayList();

    /**
     * Returns completion for JNode (should be JType or JIdentifier).
     */
    public JavaDecl[] getSuggestions(JNode aNode)
    {
        // Set node
        _node = aNode;

        // Add suggestions for node
        if (aNode instanceof JType)
            getSuggestions((JType) aNode);
        else if (aNode instanceof JExprId)
            getSuggestions((JExprId) aNode);

        // Get receiving class and, if 2 letters or less, filter out suggestions that don't apply (unless none do)
        Class reccls = getReceivingClass(aNode);
        if (reccls != null && _list.size() > 10 && aNode.getName().length() <= 2) {
            List l2 = _list.stream().filter(p -> isRecivingClassAssignable(p, reccls)).collect(Collectors.toList());
            if (l2.size() > 0) _list = l2;
        }

        // Get array and sort
        JavaDecl decls[] = _list.toArray(new JavaDecl[0]);
        Arrays.sort(decls, new DeclCompare(reccls));
        return decls;
    }

    /**
     * Adds a JavaDecl for object.
     */
    private void addDecl(Object anObj)
    {
        JavaDecl jd = _node.getJavaDecl(anObj);
        addDecl(jd);
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
     * Find suggestions for JType.
     */
    private void getSuggestions(JType aJType)
    {
        ClassPathInfo cpinfo = ClassPathInfo.get(aJType);
        String prefix = aJType.getName();

        List<String> cnames = prefix.length() > 3 ? cpinfo.getAllClassNames(prefix) : cpinfo.getCommonClassNames(prefix);
        for (String cname : cnames) {
            if (aJType.getParent() instanceof JExprAlloc) {
                Class cls = cpinfo.getClass(cname);
                if (cls == null || !Modifier.isPublic(cls.getModifiers())) continue;
                Constructor cstrs[] = null;
                try {
                    cstrs = cls.getConstructors();
                } catch (Throwable t) {
                }
                if (cstrs != null) for (Constructor cstr : cstrs)
                    if (!cstr.isSynthetic()) addDecl(cstr);
            } else addDecl(cname);
        }
    }

    /**
     * Find suggestions for JExprId.
     */
    private void getSuggestions(JExprId anId)
    {
        // Get prefix string
        String prefix = anId.getName();
        ClassPathInfo cpinfo = ClassPathInfo.get(anId);

        // If there is a parent expression, work from it
        JExpr parExpr = anId.getParentExpr();
        if (parExpr != null) {

            // Handle parent is Package: Add packages and classes with prefix
            if (parExpr instanceof JExprId && ((JExprId) parExpr).isPackageName()) {
                JExprId parId = (JExprId) parExpr;
                String parPkg = parId.getPackageName();
                for (String cname : cpinfo.getPackageClassNames(parPkg, prefix))
                    addDecl(cname);
                for (String pname : cpinfo.getPackageChildrenNames(parPkg, prefix))
                    addDecl(pname);
            }

            // Handle anything else with a parent class
            else if (parExpr.getEvalType() != null) {
                JavaDeclClass parDecl = parExpr.getEvalType().getClassType();
                List<JavaDecl> fields = parDecl.getPrefixFields(prefix);
                for (JavaDecl fd : fields) addDecl(fd);
                List<JavaDecl> meths = parDecl.getPrefixMethods(prefix);
                for (JavaDecl md : meths) addDecl(md);
            }
        }

        // If no JExpr prefix, get variables with prefix
        else {

            // Get variables with prefix of name and add to suggestions
            List<JVarDecl> varDecls = anId.getVarDecls(prefix, new ArrayList());
            for (JVarDecl vdecl : varDecls)
                addDecl(vdecl.getDecl());

            // Add methods of enclosing class
            JClassDecl ecd = anId.getEnclosingClassDecl();
            Class ec = ecd != null ? ecd.getEvalClass() : null;
            while (ecd != null && ec != null) {
                for (Method meth : ClassExtras.getMethods(ec, prefix))
                    addDecl(meth);
                ecd = ecd.getEnclosingClassDecl();
                ec = ecd != null ? ecd.getEvalClass() : null;
            }

            // If starts with upper case or is greater than 3 chars, add classes with prefix that are public
            List<String> cnames = prefix.length() > 3 ? cpinfo.getAllClassNames(prefix) : cpinfo.getCommonClassNames(prefix);
            for (String cname : cnames) {
                Class cls = cpinfo.getClass(cname);
                if (cls == null || !Modifier.isPublic(cls.getModifiers())) continue;
                addDecl(cname);
            }

            // Add packages with prefix
            List<String> pnames = cpinfo.getAllPackageNames(prefix);
            for (String name : pnames)
                addDecl(name);
        }
    }

    /**
     * Returns the assignable type of given node assuming it's the receiving expression of assign or a method arg.
     */
    private static Class getReceivingClass(JNode aNode)
    {
        // If MethocCall arg, return arg class
        JavaDecl argType = getMethodCallArgType(aNode);
        if (argType != null)
            return argType.getEvalClass();

        // If node is Assign Right-Hand-Side, return assignment Left-Hand-Side class
        JExprMath assExpr = getExpression(aNode, JExprMath.Op.Assign);
        JExpr lhs = assExpr != null ? assExpr.getOperand(0) : null;
        if (lhs != null)
            return lhs.getEvalClass();

        // If node is JVarDecl Initializer, return JVarDecl class
        JVarDecl vd = getVarDeclForInitializer(aNode);
        if (vd != null)
            return vd.getEvalClass();

        // If node is JExprMath, return op class
        JExprMath me = aNode.getParent(JExprMath.class);
        if (me != null) {
            switch (me.getOp()) {
                case Or:
                case And:
                case Not:
                    return Boolean.class;
            }
            return Double.class;
        }

        // If node is expression and top parent is conditional statement, return boolean
        JExpr exp = aNode instanceof JExpr ? (JExpr) aNode : aNode.getParent(JExpr.class);
        if (exp != null) {
            while (exp.getParent() instanceof JExpr) exp = (JExpr) exp.getParent();
            JNode par = exp.getParent();
            if (par instanceof JStmtIf || par instanceof JStmtWhile || par instanceof JStmtDo)
                return Boolean.class;
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
    private static JavaDecl getMethodCallArgType(JNode aNode)
    {
        JExprMethodCall methodCall = getMethodCall(aNode);
        if (methodCall == null) return null;
        int argIndex = getMethodCallArgIndex(methodCall, aNode);
        if (argIndex < 0) return null;
        JavaDecl mdecl = methodCall.getDecl();
        if (mdecl == null) return null;
        return argIndex < mdecl.getParamCount() ? mdecl.getParamType(argIndex) : null;
    }

    /**
     * Return the method call arg index of node.
     */
    private static int getMethodCallArgIndex(JExprMethodCall aMethodCall, JNode aNode)
    {
        JExprMethodCall methodCall = aMethodCall != null ? aMethodCall : getMethodCall(aNode);
        if (methodCall == null) return -1;
        List<JExpr> args = methodCall.getArgs();
        JNode node = aNode;
        while (node != methodCall) {
            for (int i = 0, iMax = args.size(); i < iMax; i++)
                if (args.get(i) == node)
                    return i;
            node = node.getParent();
        }
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
        return null;
    }

    /**
     * Returns whether suggestion is receiving class.
     */
    private static final boolean isRecivingClassAssignable(JavaDecl aJD, Class aRC)
    {
        return getRecivingClassAssignableScore(aJD, aRC) > 0;
    }

    /**
     * Returns whether suggestion is receiving class.
     */
    private static final int getRecivingClassAssignableScore(JavaDecl aJD, Class aRC)
    {
        if (aRC == null || aJD.isPackage()) return 0;
        Class dcls = aJD.getEvalClass();
        if (dcls == null) return 0;
        return aRC == dcls ? 2 : ClassUtils.isAssignable(aRC, dcls) ? 1 : 0;
    }

    /**
     * A Comparator to sort JavaDecls.
     */
    private static class DeclCompare implements Comparator<JavaDecl> {

        // The receiving class for suggestions
        Class _rclass = null;

        /**
         * Creates a DeclCompare.
         */
        DeclCompare(Class aRC)
        {
            _rclass = aRC;
        }

        /**
         * Standard compare to method.
         */
        public int compare(JavaDecl o1, JavaDecl o2)
        {
            // Get whether either suggestion is of Assignable to ReceivingClass
            int rca1 = getRecivingClassAssignableScore(o1, _rclass);
            int rca2 = getRecivingClassAssignableScore(o2, _rclass);
            if (rca1 != rca2) return rca1 > rca2 ? -1 : 1;

            // If Suggestion Types differ, return by type
            if (o1.getType() != o2.getType()) return getOrder(o1.getType()) < getOrder(o2.getType()) ? -1 : 1;

            // If either is member class, sort other first
            if (o1.isMemberClass() != o2.isMemberClass()) return o2.isMemberClass() ? -1 : 1;

            // Make certain packages get preference
            if (o1.isClass()) {
                String s1 = o1.getClassName(), s2 = o2.getClassName();
                for (String pp : PREF_PACKAGES) {
                    if (s1.startsWith(pp) && !s2.startsWith(pp)) return -1;
                    if (s2.startsWith(pp) && !s1.startsWith(pp)) return 1;
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
                case VarDecl:
                    return 0;
                case Field:
                    return 1;
                case Method:
                    return 2;
                case Class:
                    return 3;
                case Package:
                    return 4;
                default:
                    return 5;
            }
        }
    }

    /**
     * List or preferred packages.
     */
    private static String PREF_PACKAGES[] = {"java.lang.", "java.util.", "snap.", "java."};

}