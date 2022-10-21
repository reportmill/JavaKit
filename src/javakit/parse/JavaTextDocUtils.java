/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaType;
import javakit.resolver.Resolver;
import snap.parse.*;
import snap.text.TextDocUtils;
import snap.util.CharSequenceUtils;
import java.util.List;

/**
 * Utility methods and support for JavaShell.
 */
public class JavaTextDocUtils {

    /**
     * Returns an array of statements for given JFile.
     */
    public static JStmt[] getStatementsForJavaNode(JNode aJNode)
    {
        // Get last line for node
        int childCount = aJNode.getChildCount();
        if (childCount == 0) // Can happen if Parse totally fails (e.g., tokenizer fails)
            return new JStmt[0];
        JNode lastChild = aJNode.getChild(childCount - 1);
        int lastLineIndex = lastChild.getEndToken().getLineIndex();

        // Create statement array and load
        JStmt[] stmtArray = new JStmt[lastLineIndex + 2];
        getStatementsForJavaNode(aJNode, stmtArray);

        // Iterate over statement array and if partial VarDecl if needed
        for (JStmt stmt : stmtArray) {
            if (JavaTextDocUtils.isIncompleteVarDecl(stmt)) {
                JStmtBlock blockStmt = stmt.getParent(JStmtBlock.class);
                JavaTextDocUtils.fixIncompleteVarDecl(stmt, blockStmt);
            }
        }

        // Return
        return stmtArray;
    }

    /**
     * Returns an array of statements for given JFile.
     */
    public static void getStatementsForJavaNode(JNode aJNode, JStmt[] stmtArray)
    {
        // Handle statement node (but not block), get line index and set in array
        if (aJNode instanceof JStmt && !(aJNode instanceof JStmtBlock)) {
            JStmt stmt = (JStmt) aJNode;
            int lineIndex = stmt.getLineIndex();
            stmtArray[lineIndex] = (JStmt) aJNode;
            return;
        }

        // Handle any node: Iterate over children and recurse
        List<JNode> children = aJNode.getChildren();
        for (JNode child : children)
            getStatementsForJavaNode(child, stmtArray);
    }

    /**
     * Returns whether expression statement is really a variable decl without type.
     */
    public static boolean isIncompleteVarDecl(JStmt aStmt)
    {
        // If expression statement, check for assignment
        if (aStmt instanceof JStmtExpr) {

            // Get expression
            JStmtExpr exprStmt = (JStmtExpr) aStmt;
            JExpr expr = exprStmt.getExpr();

            // If assignment, check for undefined 'AssignTo' type
            if (expr instanceof JExprMath && ((JExprMath) expr).getOp() == JExprMath.Op.Assign) {
                JExprMath assignExpr = (JExprMath) expr;
                JExpr assignTo = assignExpr.getOperand(0);
                if (assignTo.getDecl() == null && assignExpr.getOperandCount() > 1 && assignExpr.getOperand(0) instanceof JExprId)
                    return true;
            }
        }

        // Return
        return false;
    }

    /**
     * Fixes incomplete VarDecl.
     */
    public static void fixIncompleteVarDecl(JStmt aStmt, JStmtBlock stmtBlock)
    {
        // Get expr statement, assign expression and assign-to expression
        JStmtExpr exprStmt = (JStmtExpr) aStmt;
        JExprMath assignExpr = (JExprMath) exprStmt.getExpr();
        JExpr assignTo = assignExpr.getOperand(0);

        // Create VarDecl from Id and initializer
        JVarDecl varDecl = new JVarDecl();
        varDecl.setId((JExprId) assignTo);
        JExpr initializer = assignExpr.getOperand(1);
        varDecl.setInitializer(initializer);

        // Create VarDeclStatement and add VarDecl
        JStmtVarDecl varDeclStmt = new JStmtVarDecl();
        varDeclStmt.addVarDecl(varDecl);

        // Swap VarDecl statement in for expr statement
        int index = stmtBlock.removeStatement(aStmt);
        stmtBlock.addStatement(varDeclStmt, index);

        // Get initializer type
        JavaType initType = initializer.getEvalType();
        if (initType == null) {
            System.out.println("JSParser.fixIncompleteVarDecl: Failed to get init type for " + initializer.getString());
            Resolver resolver = aStmt.getResolver();
            initType = resolver.getJavaClassForClass(Object.class);
        }

        // Create bogus type from initializer
        JType type = new JType();
        type.setName(initType.getName());
        type.setStartToken(assignTo.getStartToken());
        type.setEndToken(assignTo.getEndToken());
        type.setPrimitive(initType.isPrimitive());
        type.setParent(varDecl);
        varDecl.setType(type);
    }

    /**
     * Updates JFile for given range change.
     */
    public static boolean updateJFileForChange(JavaTextDoc javaTextDoc, JFile aJFile, TextDocUtils.CharsChange aCharsChange)
    {
        // If no JFile, just bail
        if (aJFile == null) return true;

        // Get CharsChange and charIndex
        CharSequence addChars = aCharsChange.getNewValue();
        CharSequence removeChars = aCharsChange.getOldValue();
        int startCharIndex = aCharsChange.getIndex();
        int endOldCharIndex = startCharIndex + (addChars != null ? 0 : removeChars.length());

        // If change is more than 50 chars or contains newline, just reparse all
        CharSequence changeChars = addChars != null ? addChars : removeChars;
        if (changeChars.length() > 50 || CharSequenceUtils.indexOfNewline(changeChars, 0) >= 0)
            return false;

        // Get outer statement enclosing range
        JNode jnode = aJFile.getNodeAtCharIndex(startCharIndex);
        JStmtBlock oldStmt = jnode instanceof JStmtBlock ? (JStmtBlock) jnode : jnode.getParent(JStmtBlock.class);
        while (oldStmt != null && oldStmt.getEndCharIndex() < endOldCharIndex)
            oldStmt = oldStmt.getParent(JStmtBlock.class);

        // If enclosing statement not found, just reparse all
        if (oldStmt == null)
            return false;

        // Parse new JStmtBlock (create empty one if there wasn't enough in block to create it)
        if (_stmtParser == null)
            _stmtParser = new StmtParser(javaTextDoc.getJavaParser());
        _stmtParser.setInput(javaTextDoc);
        _stmtParser.setCharIndex(oldStmt.getStartCharIndex());
        _stmtParser.getTokenizer().setLineIndex(oldStmt.getLineIndex());

        // Parse new statement
        JStmtBlock newStmt = null;
        try { newStmt = _stmtParser.parseCustom(JStmtBlock.class); }
        catch (Exception e) { }

        // If parse failed, return failed
        ParseToken endToken = newStmt != null ? newStmt.getEndToken() : null;
        if (endToken == null || !endToken.getPattern().equals(oldStmt.getEndToken().getPattern()))
            return false;

        // Replace old statement with new statement
        JNode stmtParent = oldStmt.getParent();
        stmtParent.setBlock(newStmt);

        // Extend ancestor ends if needed
        JNode ancestor = stmtParent.getParent();
        while (ancestor != null) {
            if (ancestor.getEndToken() == null || ancestor.getEndCharIndex() < endToken.getEndCharIndex()) {
                ancestor.setEndToken(endToken);
                ancestor = ancestor.getParent();
            }
            else break;
        }

        // Return success
        return true;
    }


    // Special statement parser
    private static StmtParser _stmtParser;

    /**
     * A Parser for JavaText modified statements.
     */
    private static class StmtParser extends Parser {

        private JavaParser _javaParser;

        /** Constructor. */
        StmtParser(JavaParser javaParser)
        {
            super(javaParser.getRule("Statement"));
            _javaParser = javaParser;
        }

        /** Override to use JavaParser.Tokenizer. */
        public Tokenizer getTokenizer()
        {
            return _javaParser.getTokenizer();
        }

        /** Override to ignore exception. */
        protected void parseFailed(ParseRule aRule, ParseHandler aHandler)  { }
    }
}
