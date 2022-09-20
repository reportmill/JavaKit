/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import java.util.*;
import java.util.regex.Matcher;

import javakit.reflect.JavaDecl;

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
    protected JavaDecl getDeclForChildNode(JNode aNode)
    {
        // Get VarDecl for name from statements
        List<JStmt> statements = getStatements();
        JVarDecl varDecl = getVarDeclForNameFromStatements(aNode, statements);
        if (varDecl != null)
            return varDecl.getDecl();

        // Do normal version
        return super.getDeclForChildNode(aNode);
    }

    /**
     * Override to search VarDecl statements.
     */
    @Override
    public List<JVarDecl> getVarDeclsForMatcher(Matcher aMatcher, List<JVarDecl> varDeclList)
    {
        // Get statements
        List<JStmt> statements = getStatements();

        // Get VarDecls for prefix from statements
        getVarDeclsForMatcherFromStatements(aMatcher, statements, varDeclList);

        // Do normal version
        return super.getVarDeclsForMatcher(aMatcher, varDeclList);
    }

    /**
     * Finds JVarDecls for given Node.Name in given statements and adds them to given list.
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
     * Finds JVarDecls for given matcher in given statements and adds them to given list.
     */
    public static void getVarDeclsForMatcherFromStatements(Matcher aMatcher, List<JStmt> theStmts, List<JVarDecl> varDeclList)
    {
        // Iterate over statements and see if any JStmtVarDecl contains variable with that name
        for (JStmt stmt : theStmts) {

            // Skip non-VarDecl statements
            if (!(stmt instanceof JStmtVarDecl)) continue;

            // Get varDeclStmt.VarDecls
            JStmtVarDecl varDeclStmt = (JStmtVarDecl) stmt;
            List<JVarDecl> varDecls = varDeclStmt.getVarDecls();

            // Iterate over VarDecls and add those that match prefix
            for (JVarDecl varDecl : varDecls)
                if (aMatcher.reset(varDecl.getName()).lookingAt())
                    varDeclList.add(varDecl);
        }
    }
}