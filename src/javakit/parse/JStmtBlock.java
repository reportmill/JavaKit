/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import java.util.*;

import javakit.reflect.JavaDecl;
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
        // Get VarDecl for name from statements
        List<JStmt> statements = getStatements();
        JVarDecl varDecl = getVarDeclForNameFromStatements(aNode, statements);
        if (varDecl != null)
            return varDecl.getDecl();

        // Do normal version
        return super.getDeclImpl(aNode);
    }

    /**
     * Override to check inner variable declaration statements.
     */
    public static JVarDecl getVarDeclForNameFromStatements(JNode aNode, List<JStmt> theStmts)
    {
        // Get node info
        String name = aNode.getName(); if (name == null) return null;

        // This shouldn't happen
        boolean isType = aNode instanceof JExprType;
        if (isType)
            return null;

        // Iterate over statements and see if any contains variable
        for (JStmt stmt : theStmts) {

            // Handle Label statement
            if (stmt instanceof JStmtLabeled) {
                JStmtLabeled labeledStmt = (JStmtLabeled) stmt;
                if (name.equals(labeledStmt.getLabelName()))
                    return labeledStmt.getLabelVarDecl();
            }

            // If block statement is past id reference, break
            if (stmt.getStart() > aNode.getStart())
                break;

            // Handle VarDecl
            if (stmt instanceof JStmtVarDecl) {
                JStmtVarDecl varDeclStmt = (JStmtVarDecl) stmt;
                List<JVarDecl> varDecls = varDeclStmt.getVarDecls();
                for (JVarDecl varDecl : varDecls) {
                    if (name.equals(varDecl.getName())) {
                        if (varDecl.getStart() < aNode.getStart())
                            return varDecl;
                    }
                }
            }
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