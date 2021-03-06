/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import java.util.*;

import javakit.reflect.JavaDecl;
import snap.util.SnapUtils;

/**
 * A JStatement for for() statements.
 */
public class JStmtFor extends JStmt {
    // Whether this for statement is really ForEach
    boolean _forEach = true;

    // The for-init declaration (if declaration)
    JStmtVarDecl _initDecl;

    // The conditional
    JExpr _cond;

    // The update
    List<JStmtExpr> _updateStmts = new ArrayList();

    // The for-init List of StatementExpressions (if statement expressions)
    List<JStmtExpr> _initStmts = new ArrayList();

    // The statement to perform while conditional is true
    JStmt _stmt;

    /**
     * Returns whether for statement is ForEach.
     */
    public boolean isForEach()
    {
        return _forEach;
    }

    /**
     * Returns the init declaration.
     */
    public JStmtVarDecl getInitDecl()
    {
        return _initDecl;
    }

    /**
     * Sets the init declaration.
     */
    public void setInitDecl(JStmtVarDecl aVD)
    {
        replaceChild(_initDecl, _initDecl = aVD);
    }

    /**
     * Returns the conditional.
     */
    public JExpr getConditional()
    {
        return _cond;
    }

    /**
     * Sets the conditional.
     */
    public void setConditional(JExpr anExpr)
    {
        replaceChild(_cond, _cond = anExpr);
    }

    /**
     * Returns the update statements.
     */
    public List<JStmtExpr> getUpdateStmts()
    {
        return _updateStmts;
    }

    /**
     * Add an update statements.
     */
    public void addUpdateStmt(JStmtExpr aStmtExpr)
    {
        _updateStmts.add(aStmtExpr);
        addChild(aStmtExpr, -1);
    }

    /**
     * Returns the init statements.
     */
    public List<JStmtExpr> getInitStmts()
    {
        return _initStmts;
    }

    /**
     * Adds an init statements.
     */
    public void addInitStmt(JStmtExpr aStmtExpr)
    {
        _initStmts.add(aStmtExpr);
        addChild(aStmtExpr, -1);
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

    /**
     * Override to check init declaration.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        // Get node info
        String name = aNode.getName();
        boolean isType = aNode instanceof JExprType;

        // Check init declaration
        if (!isType && _initDecl != null) for (JVarDecl vd : _initDecl.getVarDecls())
            if (SnapUtils.equals(vd.getName(), name))
                return vd.getDecl();

        // Do normal version
        return super.getDeclImpl(aNode);
    }

}