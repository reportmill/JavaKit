/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for SynchronizedStatement.
 */
public class JStmtSynchronized extends JStmt {
    // The synchronized expression
    JExpr _expr;

    // The statement block
    JStmtBlock _block;

    /**
     * Returns the expression.
     */
    public JExpr getExpression()
    {
        return _expr;
    }

    /**
     * Sets the expression.
     */
    public void setExpression(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Returns the statement block.
     */
    public JStmtBlock getBlock()
    {
        return _block;
    }

    /**
     * Sets the block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        replaceChild(_block, _block = aBlock);
    }

}