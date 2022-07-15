/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import java.util.*;

import snap.util.*;

/**
 * A Java statement for a block of statements.
 */
public class JStmtBlock extends JStmt {

    /**
     * Returns the list of statements.
     */
    public List<JStmt> getStatements()
    {
        return (List) _children;
    }

    /**
     * Adds a statement.
     */
    public void addStatement(JStmt aStmt)
    {
        addChild(aStmt, -1);
    }

    /**
     * Removes a statement.
     */
    public int removeStatement(JStmt aStmt)
    {
        return removeChild(aStmt);
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
        return this;
    }

    /**
     * Override to check inner variable declaration statements.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        JavaDecl decl = getDeclImpl(aNode, getStatements());
        if (decl != null)
            return decl;
        return super.getDeclImpl(aNode);
    }

    /**
     * Override to check inner variable declaration statements.
     */
    public static JavaDecl getDeclImpl(JNode aNode, List<JStmt> theStmts)
    {
        // Get node info
        String name = aNode.getName();
        boolean isType = aNode instanceof JExprType;

        // Iterate over statements and see if any contains variable
        if (!isType) for (JStmt s : theStmts) {
            if (s instanceof JStmtLabeled) {
                JStmtLabeled ls = (JStmtLabeled) s;
                if (SnapUtils.equals(ls.getLabelName(), name))
                    return ls.getLabelVarDecl().getDecl();
            }
            if (s.getStart() > aNode.getStart()) break;
            if (!(s instanceof JStmtVarDecl)) continue;
            JStmtVarDecl vds = (JStmtVarDecl) s;
            for (JVarDecl vd : vds.getVarDecls())
                if (SnapUtils.equals(vd.getName(), name) && vd.getStart() < aNode.getStart())
                    return vd.getDecl();
        }

        // Do normal version
        return null;
    }

    /**
     * Returns a variable with given name.
     */
    @Override
    public List<JVarDecl> getVarDecls(String aPrefix, List<JVarDecl> theVDs)
    {
        // Iterate over statements and see if any JStmtVarDecl contains variable with that name
        for (JStmt s : getStatements()) {
            if (!(s instanceof JStmtVarDecl)) continue;
            JStmtVarDecl vds = (JStmtVarDecl) s;
            for (JVarDecl vd : vds.getVarDecls())
                if (StringUtils.startsWithIC(vd.getName(), aPrefix))
                    theVDs.add(vd);
        }

        // Do normal version
        return super.getVarDecls(aPrefix, theVDs);
    }

}