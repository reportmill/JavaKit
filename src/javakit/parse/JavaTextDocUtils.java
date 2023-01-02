/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.*;
import snap.text.TextDocUtils;
import snap.util.CharSequenceUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods and support for JavaTextDoc.
 */
public class JavaTextDocUtils {

    /**
     * Returns an array of statements in given node.
     */
    public static JStmt[] getStatementsForJavaNode(JNode aJNode)
    {
        List<JStmt> stmtsList = new ArrayList<>();
        findStatementsForJavaNode(aJNode, stmtsList);
        return stmtsList.toArray(new JStmt[0]);
    }

    /**
     * Recursively finds all statements in node and adds to given list.
     */
    private static void findStatementsForJavaNode(JNode aJNode, List<JStmt> theStmtsList)
    {
        // Handle statement node (but not block), get line index and set in array
        if (aJNode instanceof JStmt && !(aJNode instanceof JStmtBlock)) {
            theStmtsList.add((JStmt) aJNode);
            return;
        }

        // Handle any node: Iterate over children and recurse
        List<JNode> children = aJNode.getChildren();
        for (JNode child : children)
            findStatementsForJavaNode(child, theStmtsList);
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
        if (stmtParent instanceof WithBlockStmt)
            ((WithBlockStmt) stmtParent).setBlock(newStmt);
        else System.err.println("JavaTextDocUtils.updateJFileForChange: Parent not WithBlockStmt: " + stmtParent);

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
