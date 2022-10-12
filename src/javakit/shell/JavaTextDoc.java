/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import javakit.parse.*;
import javakit.text.JavaTextUtils;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.parse.*;
import snap.props.PropChange;
import snap.text.*;
import snap.util.StringUtils;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.List;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextDoc extends TextDoc {

    // The parsed Java file
    private JFile  _jfile;

    // The parser to parse Java
    private JavaParser  _javaParser;

    // Constants for Syntax Coloring
    private static Color COMMENT_COLOR = new Color("#3F7F5F"); //336633
    private static Color RESERVED_WORD_COLOR = new Color("#660033");
    private static Color STRING_LITERAL_COLOR = new Color("#C80000"); // CC0000

    /**
     * Constructor.
     */
    public JavaTextDoc()
    {
        super();

        // Reset default TextStyle for code
        TextStyle textStyle = getDefaultStyle();
        Font codeFont = JavaTextUtils.getCodeFont();
        TextStyle codeTextStyle = textStyle.copyFor(codeFont);
        setDefaultStyle(codeTextStyle);

        // Reset default LineStyle for code
        TextLineStyle lineStyle = getDefaultLineStyle();
        TextLineStyle lineStyleSpaced = lineStyle.copyFor(TextLineStyle.SPACING_KEY, 4);
        //double tabW = codeTextStyle.getCharAdvance(' ') * 4;
        //lineStyleSpaced.setTabs(new double[] { tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW });
        setDefaultLineStyle(lineStyleSpaced);
    }

    /**
     * Returns the parser to parse java file.
     */
    public JavaParser getJavaParser()
    {
        // If already set, just return
        if (_javaParser != null) return _javaParser;

        // Create, set, return
        JavaParser javaParser = JavaParser.getShared();
        return _javaParser = javaParser;
    }

    /**
     * Sets the parser to parse java file.
     */
    public void setJavaParser(JavaParser aJavaParser)  { _javaParser = aJavaParser; }

    /**
     * Returns the JFile (parsed Java file).
     */
    public JFile getJFile()
    {
        // If already set, just return
        if (_jfile != null) return _jfile;

        // Get parsed java file
        JavaParser javaParser = getJavaParser();
        String javaStr = getString();
        JFile jfile = javaParser.getJavaFile(javaStr);

        // Set SourceFile
        WebFile sourceFile = getSourceFile();
        if (sourceFile != null)
            jfile.setSourceFile(sourceFile);

        // This sucks
        getStatementsForJavaNode(jfile);

        // Set, return
        return _jfile = jfile;
    }

    /**
     * Override to clear JFile.
     */
    @Override
    public void setString(String aString)
    {
        // Do normal version
        super.setString(aString);
        _jfile = null;
    }

    /**
     * Override to create tokens.
     */
    @Override
    protected TextToken[] createTokensForTextLine(TextLine aTextLine)
    {
        // Get iteration vars
        List<TextToken> tokens = new ArrayList<>();
        TextRun textRun = aTextLine.getRun(0);

        // If this line is InMultilineComment (do this first, since it may require use of Text.Tokenizer)
        TextLine prevTextLine = aTextLine.getPrevious();
        TextToken prevTextLineLastToken = prevTextLine != null ? prevTextLine.getLastToken() : null;
        boolean inUnterminatedComment = isTextTokenUnterminatedMultilineComment(prevTextLineLastToken);

        // Get tokenizer
        JavaParser javaParser = getJavaParser();
        CodeTokenizer tokenizer = javaParser.getTokenizer();
        tokenizer.setInput(aTextLine);

        // Get first line token: Handle if already in Multi-line
        Exception exception = null;
        ParseToken parseToken = null;
        if (inUnterminatedComment)
            parseToken = tokenizer.getMultiLineCommentTokenMore(null);
        else {
            try { parseToken = tokenizer.getNextSpecialTokenOrToken(); }
            catch (Exception e) {
                exception = e;
                System.out.println("JavaTextDoc.createTokensForTextLine: Parse error: " + e);
            }
        }

        // Get line parse tokens and create TextTokens
        while (parseToken != null) {

            // Get token start/end
            int tokenStart = parseToken.getStartCharIndex();
            int tokenEnd = parseToken.getEndCharIndex();

            // Create TextToken
            TextToken textToken = new TextToken(aTextLine, tokenStart, tokenEnd, textRun);
            textToken.setName(parseToken.getName());
            tokens.add(textToken);

            // Get/set token color
            Color color = getColorForParseToken(parseToken);
            if (color != null)
                textToken.setTextColor(color);

            // Get next token
            try { parseToken = tokenizer.getNextSpecialTokenOrToken(); }
            catch (Exception e) {
                exception = e;
                parseToken = null;
                System.out.println("JavaTextDoc.createTokensForTextLine: Parse error: " + e);
            }
        }

        // If exception was hit, create token for rest of line
        if (exception != null) {
            int tokenStart = tokenizer.getCharIndex();
            int tokenEnd = aTextLine.length();
            TextToken textToken = new TextToken(aTextLine, tokenStart, tokenEnd, textRun);
            tokens.add(textToken);
        }

        // Return
        return tokens.toArray(new TextToken[0]);
    }

    /**
     * Returns whether given TextToken is an unterminated comment.
     */
    private boolean isTextTokenUnterminatedMultilineComment(TextToken aTextToken)
    {
        if (aTextToken == null)
            return false;
        String name = aTextToken.getName();
        if (name != Tokenizer.MULTI_LINE_COMMENT)
            return false;
        String tokenStr = aTextToken.getString();
        if (tokenStr.endsWith("*/"))
            return false;
        return true;
    }

    /**
     * Returns an array of statements for given JFile.
     */
    public JStmt[] getStatementsForJavaNode(JNode aJNode)
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
            if (JavaShellUtils.isIncompleteVarDecl(stmt)) {
                JStmtBlock blockStmt = stmt.getParent(JStmtBlock.class);
                JavaShellUtils.fixIncompleteVarDecl(stmt, blockStmt);
            }
        }

        // Return
        return stmtArray;
    }

    /**
     * Returns an array of statements for given JFile.
     */
    private void getStatementsForJavaNode(JNode aJNode, JStmt[] stmtArray)
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
     * Checks the given token for syntax coloring.
     */
    public static Color getColorForParseToken(ParseToken aToken)
    {
        // Handle comments
        String tokenName = aToken.getName();
        if (tokenName == CodeTokenizer.SINGLE_LINE_COMMENT || tokenName == CodeTokenizer.MULTI_LINE_COMMENT)
            return COMMENT_COLOR;

        // Handle reserved words
        char firstPatternChar = aToken.getPattern().charAt(0);
        if (Character.isLetter(firstPatternChar))
            return RESERVED_WORD_COLOR;

        // Handle string literals
        if (tokenName == "StringLiteral" || tokenName == "CharacterLiteral")
            return STRING_LITERAL_COLOR;

        // Return none
        return null;
    }

    /**
     * Override to detect char changes.
     */
    @Override
    protected void firePropChange(PropChange aPC)
    {
        // Do normal version
        super.firePropChange(aPC);

        // Get PropName
        String propName = aPC.getPropName();

        // Handle CharsChange: Try to update JFile with partial parse
        if (propName == Chars_Prop && _jfile != null) {

            // If partial parse fails, clear JFile for full reparse
            TextDocUtils.CharsChange charsChange = (TextDocUtils.CharsChange) aPC;
            boolean jfileUpdated = updateJFileForChange(charsChange);
            if (!jfileUpdated)
                _jfile = null;

            // This sucks!
            else getStatementsForJavaNode(_jfile);
        }
    }

    /**
     * Updates JFile for given range change.
     */
    private boolean updateJFileForChange(TextDocUtils.CharsChange aCharsChange)
    {
        // If no JFile, just bail
        if (_jfile == null) return true;

        // Get CharsChange and charIndex
        CharSequence addChars = aCharsChange.getNewValue();
        CharSequence removeChars = aCharsChange.getOldValue();
        int startCharIndex = aCharsChange.getIndex();
        int endOldCharIndex = startCharIndex + (addChars != null ? 0 : removeChars.length());

        // If change is more than 50 chars or contains newline, just reparse all
        CharSequence changeChars = addChars != null ? addChars : removeChars;
        if (changeChars.length() > 50 || StringUtils.indexOfNewline(changeChars, 0) >= 0)
            return false;

        // Get outer statement enclosing range
        JNode jnode = _jfile.getNodeAtCharIndex(startCharIndex);
        JStmtBlock oldStmt = jnode instanceof JStmtBlock ? (JStmtBlock) jnode : jnode.getParent(JStmtBlock.class);
        while (oldStmt != null && oldStmt.getEnd() < endOldCharIndex)
            oldStmt = oldStmt.getParent(JStmtBlock.class);

        // If enclosing statement not found, just reparse all
        if (oldStmt == null)
            return false;

        // Parse new JStmtBlock (create empty one if there wasn't enough in block to create it)
        if (_stmtParser == null)
            _stmtParser = new StmtParser();
        _stmtParser.setInput(JavaTextDoc.this);
        _stmtParser.setCharIndex(oldStmt.getStart());

        // Parse new statement
        JStmtBlock newStmt = null;
        try { newStmt = _stmtParser.parseCustom(JStmtBlock.class); }
        catch (Exception e) { }

        // If parse failed, just create empty StmtBlock
        if (newStmt == null) {
            newStmt = new JStmtBlock();
            newStmt.setStartToken(oldStmt.getStartToken());
        }

        // Set EndToken to oldStmt.EndToken
        newStmt.setEndToken(oldStmt.getEndToken());

        // Replace old statement with new statement
        JNode stmtParent = oldStmt.getParent();
        stmtParent.setBlock(newStmt);
        return true;
    }

    // Special statement parser
    private StmtParser  _stmtParser;

    /**
     * A Parser for JavaText modified statements.
     */
    public class StmtParser extends Parser {

        /** Constructor. */
        StmtParser()
        {
            super(getJavaParser().getRule("Statement"));
        }

        /** Override to use JavaParser.Tokenizer. */
        public Tokenizer getTokenizer()
        {
            return getJavaParser().getTokenizer();
        }

        /** Override to ignore exception. */
        protected void parseFailed(ParseRule aRule, ParseHandler aHandler)  { }
    }
}
