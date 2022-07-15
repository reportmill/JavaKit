/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for DoStatement.
 */
public class JStmtDo extends JStmt {
    // The conditional expression
    JExpr _cond;

    // The statement to perform while conditional is true
    JStmt _stmt;

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
    public boolean isBlock()
    {
        return true;
    }

    /**
     * Returns the statement block.
     */
    public JStmtBlock getBlock()
    {
        if (_stmt instanceof JStmtBlock) return (JStmtBlock) _stmt;
        JStmtBlock sb = new JStmtBlock();
        if (_stmt != null) sb.addStatement(_stmt);
        setStatement(sb);
        return sb;
    }

    /**
     * Sets a block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        setStatement(aBlock);
    }

}