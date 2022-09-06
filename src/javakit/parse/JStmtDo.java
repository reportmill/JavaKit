/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for DoStatement.
 */
public class JStmtDo extends JStmt {

    // The conditional expression
    protected JExpr  _cond;

    // The statement to perform while conditional is true
    protected JStmt  _stmt;

    /**
     * Returns the conditional expression.
     */
    public JExpr getConditional()
    {
        return _cond;
    }

    /**
     * Sets the conditional expression.
     */
    public void setConditional(JExpr anExpr)
    {
        replaceChild(_cond, _cond = anExpr);
    }

    /**
     * Returns the statement.
     */
    public JStmt getStatement()
    {
        return _stmt;
    }

    /**
     * Sets the statement.
     */
    public void setStatement(JStmt aStmt)
    {
        replaceChild(_stmt, _stmt = aStmt);
    }

    /**
     * Returns whether statement has a block associated with it.
     */
    public boolean isBlock()  { return true; }

    /**
     * Returns the statement block.
     */
    public JStmtBlock getBlock()
    {
        // If statement is block, return it
        if (_stmt instanceof JStmtBlock)
            return (JStmtBlock) _stmt;

        // Otherwise create new statement block and swap it in for statement
        JStmtBlock stmtBlock = new JStmtBlock();
        if (_stmt != null)
            stmtBlock.addStatement(_stmt);
        setStatement(stmtBlock);
        return stmtBlock;
    }

    /**
     * Sets a block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        setStatement(aBlock);
    }
}