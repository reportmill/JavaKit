/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for While statement.
 */
public class JStmtWhile extends JStmt {

    // The conditional expression
    protected JExpr  _cond;

    // The statement to perform while conditional is true
    protected JStmt  _stmt;

    /**
     * Returns the conditional.
     */
    public JExpr getConditional()  { return _cond; }

    /**
     * Sets the conditional.
     */
    public void setConditional(JExpr aCond)
    {
        replaceChild(_cond, _cond = aCond);
    }

    /**
     * Returns the statement.
     */
    public JStmt getStmt()  { return _stmt; }

    /**
     * Sets the statement.
     */
    public void setStmt(JStmt aStmt)
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
        // If already set, just return
        if (_stmt instanceof JStmtBlock)
            return (JStmtBlock) _stmt;

        // Create StmtBlock, add statement and replace
        JStmtBlock stmtBlock = new JStmtBlock();
        if (_stmt != null)
            stmtBlock.addStatement(_stmt);
        setStmt(stmtBlock);

        // Return
        return stmtBlock;
    }

    /**
     * Sets a block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        setStmt(aBlock);
    }
}