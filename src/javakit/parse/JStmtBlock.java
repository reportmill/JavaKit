/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.JavaDecl;

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
     * Adds a statement.
     */
    public void addStatement(JStmt aStmt, int anIndex)
    {
        addChild(aStmt, anIndex);
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
    public boolean isBlock()  { return true; }

    /**
     * Returns the statement block.
     */
    @Override
    public JStmtBlock getBlock()  { return this; }

    /**
     * Override to check inner variable declaration statements.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // Get VarDecl for name from statements
        List<JStmt> statements = getStatements();
        JVarDecl varDecl = getVarDeclForNameFromStatements(anExprId, statements);
        if (varDecl != null)
            return varDecl.getDecl();

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }

    /**
     * Finds JVarDecls for given Node.Name in given statements and adds them to given list.
     */
    public static JVarDecl getVarDeclForNameFromStatements(JExprId anExprId, List<JStmt> theStmts)
    {
        // Get node info
        String name = anExprId.getName();
        if (name == null)
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
            if (stmt.getStartCharIndex() > anExprId.getStartCharIndex())
                break;

            // Handle VarDecl
            if (stmt instanceof JStmtVarDecl) {
                JStmtVarDecl varDeclStmt = (JStmtVarDecl) stmt;
                List<JVarDecl> varDecls = varDeclStmt.getVarDecls();
                for (JVarDecl varDecl : varDecls) {
                    if (name.equals(varDecl.getName())) {
                        if (varDecl.getStartCharIndex() < anExprId.getStartCharIndex())
                            return varDecl;
                    }
                }
            }
        }

        // Return not found
        return null;
    }
}