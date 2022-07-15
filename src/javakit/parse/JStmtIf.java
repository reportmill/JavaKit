/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A JStatement for if() statements.
 */
public class JStmtIf extends JStmt {
    // The conditional expression
    JExpr _cond;

    // The statement to perform if conditional is true
    JStmt _stmt;

    // The else clause
    JStmt _elseStmt;

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
     * Returns the else statement.
     */
    public JStmt getElseStatement()
    {
        return _elseStmt;
    }

    /**
     * Sets the else statement.
     */
    public void setElseStatement(JStmt aStmt)
    {
        replaceChild(_elseStmt, _elseStmt = aStmt);
    }

    /**
     * Returns whether statement has a block associated with it.
     */
    public boolean isBlock()
    {
        return true;
    }

    /**
     * Returns the statement block.
     */
    public JStmtBlock getBlock()
    {
        return _stmt instanceof JStmtBlock ? (JStmtBlock) _stmt : null;
    }

//public JStmtBlock getBlock(boolean doCreate) {
//    JStmtBlock sb = _stmt instanceof JStmtBlock? (JStmtBlock)_stmt : null;
//    if(sb==null && doCreate) { sb = new JStmtBlock(); if(_stmt!=null) sb.addStatement(_stmt); setStatement(sb); }
//    return sb; }

    /**
     * Sets a block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        setStatement(aBlock);
    }

}