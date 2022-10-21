/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;

/**
 * A Java statement for statements that are just expressions (increment, decrement, assignment).
 */
public class JStmtExpr extends JStmt {

    // The expression
    protected JExpr  _expr;

    /**
     * Returns the expression.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Tries to resolve the class name for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        return _expr.getDecl();
    }
}