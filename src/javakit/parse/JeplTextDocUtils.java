/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaType;
import javakit.resolver.Resolver;

/**
 * Utility methods and support for JeplTextDoc.
 */
public class JeplTextDocUtils {

    /**
     * Returns an array of statements for given JFile.
     */
    public static void findAndFixIncompleteVarDecls(JNode aJNode)
    {
        // Handle expression statement
        if (aJNode instanceof JStmtExpr) {
            JStmtExpr exprStmt = (JStmtExpr) aJNode;
            if (isIncompleteVarDecl(exprStmt))
                fixIncompleteVarDecl(exprStmt);
        }

        // Otherwise recurse
        else {
            for (int i = 0, iMax = aJNode.getChildCount(); i < iMax; i++) {
                JNode child = aJNode.getChild(i);
                findAndFixIncompleteVarDecls(child);
            }
        }
    }

    /**
     * Returns whether expression statement is really a variable decl without type.
     */
    private static boolean isIncompleteVarDecl(JStmtExpr exprStmt)
    {
        // Get expression
        JExpr expr = exprStmt.getExpr();

        // If assignment, check for undefined 'AssignTo' type
        if (expr instanceof JExprAssign) {
            JExprAssign assignExpr = (JExprAssign) expr;
            JExpr assignTo = assignExpr.getIdExpr();
            if (assignTo instanceof JExprId && assignTo.getDecl() == null && assignExpr.getValueExpr() != null)
                return true;
        }

        // Return
        return false;
    }

    /**
     * Fixes incomplete VarDecl.
     */
    private static void fixIncompleteVarDecl(JStmtExpr exprStmt)
    {
        // Get expr statement, assign expression and assign-to expression
        JExprAssign assignExpr = (JExprAssign) exprStmt.getExpr();
        JExprId assignTo = (JExprId) assignExpr.getIdExpr();

        // Create VarDecl from Id and initializer
        JVarDecl varDecl = new JVarDecl();
        varDecl.setId(assignTo);
        JExpr initializer = assignExpr.getValueExpr();
        varDecl.setInitializer(initializer);

        // Create VarDeclStatement and add VarDecl
        JStmtVarDecl varDeclStmt = new JStmtVarDecl();
        varDeclStmt.addVarDecl(varDecl);

        // Swap VarDecl statement in for expr statement
        JStmtBlock blockStmt = exprStmt.getParent(JStmtBlock.class);
        int index = blockStmt.removeStatement(exprStmt);
        blockStmt.addStatement(varDeclStmt, index);

        // Get initializer type
        JavaType initType = initializer.getEvalType();
        if (initType == null) {
            System.out.println("JeplTextDocUtils.fixIncompleteVarDecl: Failed to get init type for " + initializer.getString());
            Resolver resolver = exprStmt.getResolver();
            initType = resolver.getJavaClassForClass(Object.class);
        }

        // Create bogus type from initializer
        JType type = new JType.Builder().token(assignTo.getStartToken()).type(initType).build();
        varDecl.setType(type);
    }
}
