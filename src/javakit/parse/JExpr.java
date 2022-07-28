/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaType;

/**
 * The JNode base class for Java expressions.
 * See: JLiteral, JIdentifier, JMethodCall, JOpExpr, ArrayIndex, Allocation, Cast
 */
public abstract class JExpr extends JNode {

    /**
     * Returns the node the expression should be evaluated against.
     */
    public JNode getScopeNode()
    {
        // Get parent expression, and if found, return its type class
        JExpr parentExpr = getParentExpr();
        if (parentExpr != null)
            return parentExpr;

        // Otherwise, return enclosing class
        return getEnclosingClassDecl();
    }

    /**
     * Returns the ScopeNode EvalType.
     */
    public JavaType getScopeNodeEvalType()
    {
        JNode scopeNode = getScopeNode();
        JavaType scopeType = scopeNode != null ? scopeNode.getEvalType() : null;
        return scopeType;
    }

    /**
     * Returns the ScopeNode EvalClass.
     */
    public Class getScopeNodeEvalClass()
    {
        JNode sn = getScopeNode();
        return sn != null ? sn.getEvalTypeRealClass() : null;
    }

    /**
     * Returns the expression prior to this expression, if parent is JExprChain and this expression isn't first.
     */
    public JExpr getParentExpr()
    {
        // If parent is JExprChain, iterate over expressions and return one before this expression
        JNode par = getParent();
        if (par instanceof JExprChain) {
            JExprChain echain = (JExprChain) par;
            for (int i = 0, iMax = echain.getExprCount(); i < iMax; i++) {
                JExpr expr = echain.getExpr(i);
                if (expr == this)
                    return i > 0 ? echain.getExpr(i - 1) : null;
            }
        }
        return null; // Return null, since no parent expression
    }

    /**
     * Joins two expressions together and returns the result.
     */
    public static JExpr join(JExpr e1, JExpr e2)
    {
        // Handle null expression: Just return second expression
        if (e1 == null) return e2;

        // Handle MethodCall or MethodRef with missing prefix expression: Set prefix expression and return
        if (e2 instanceof JExprMethodCall && ((JExprMethodCall) e2).getId() == null) return setExpr(e1, e2);
        if (e2 instanceof JExprMethodRef && ((JExprMethodRef) e2).getExpr() == null) return setExpr(e1, e2);

        // If ArrayIndex with missing ArrayExpr, set and return
        if (e2 instanceof JExprArrayIndex) {
            JExprArrayIndex aexpr = (JExprArrayIndex) e2;
            if (aexpr.getArrayExpr() != null) System.err.println("JExpr.join: ArrayIndex.ArrayExpr not null");
            aexpr.setArrayExpr(e1);
            return aexpr;
        }

        // Handle ExprChain
        if (e1 instanceof JExprChain) {
            ((JExprChain) e1).addExpr(e2);
            return e1;
        }

        // Handle two arbitrary expressions
        return new JExprChain(e1, e2);
    }

    /**
     * Sets a given expression in given MethodCall or MethodRef and returns the head expression.
     */
    private static JExpr setExpr(JExpr e1, JExpr e2)
    {
        // If given expression chain, pick off last expression, set it instead and return chain
        if (e1 instanceof JExprChain) {
            JExprChain ec = (JExprChain) e1;
            int ecnt = ec.getExprCount();
            JExpr e = (JExpr) ec._children.remove(ecnt - 1);
            setExpr(e, e2);
            ec.addExpr(e2);
            return e1;
        }

        // Set Expr in MethodCall or MethodRef and return
        if (e2 instanceof JExprMethodCall) {
            JExprMethodCall mc = (JExprMethodCall) e2;
            if (e1 instanceof JExprId) mc.setId((JExprId) e1);
        } else if (e2 instanceof JExprMethodRef) {
            JExprMethodRef mr = (JExprMethodRef) e2;
            mr.setExpr(e1);
        }
        return e2;
    }

    /**
     * A JExpr subclass for Cast expressions.
     */
    public static class CastExpr extends JExpr {

        // The cast type
        JType _type;

        // The real expression for cast
        JExpr _expr;

        /**
         * Returns the cast JType.
         */
        public JType getType()
        {
            return _type;
        }

        /**
         * Sets the cast JType.
         */
        public void setType(JType aType)
        {
            replaceChild(_type, _type = aType);
        }

        /**
         * Returns the expression being cast.
         */
        public JExpr getExpr()
        {
            return _expr;
        }

        /**
         * Sets the cast expression.
         */
        public void setExpr(JExpr anExpr)
        {
            replaceChild(_expr, _expr = anExpr);
        }

        /**
         * Returns the node name.
         */
        public String getNodeString()
        {
            return "Cast";
        }

        /**
         * Override to return declaration of type.
         */
        protected JavaDecl getDeclImpl()
        {
            return _type.getDecl();
        }
    }

    /**
     * A JExpr subclass for InstanceOf expressions.
     */
    public static class InstanceOfExpr extends JExpr {

        // The real expression for cast
        JExpr _expr;

        // The target type
        JType _type;

        /**
         * Returns the expression to be checked.
         */
        public JExpr getExpr()
        {
            return _expr;
        }

        /**
         * Sets the expression to be checked.
         */
        public void setExpr(JExpr anExpr)
        {
            replaceChild(_expr, _expr = anExpr);
        }

        /**
         * Returns the JType to be checked against.
         */
        public JType getType()
        {
            return _type;
        }

        /**
         * Sets the JType to be checked against.
         */
        public void setType(JType aType)
        {
            replaceChild(_type, _type = aType);
        }

        /**
         * Returns the node name.
         */
        public String getNodeString()
        {
            return "InstanceOf";
        }

        /**
         * Override to return declaration of type.
         */
        protected JavaDecl getDeclImpl()
        {
            return getJavaClassForClass(boolean.class);
        }
    }

}