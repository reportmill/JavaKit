/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.Resolver;
import javakit.ide.JavaTextUtils;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.parse.*;
import snap.props.PropChange;
import snap.text.*;
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

    // The Resolver
    private Resolver  _resolver;

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
     * Returns the Resolver that is attached to parsed Java file.
     */
    public Resolver getResolver()  { return _resolver; }

    /**
     * Sets the Resolver that is attached to parsed Java file.
     */
    public void setResolver(Resolver aResolver)  { _resolver = aResolver; }

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
        if (jfile == null)
            jfile = new JFile();

        // Set SourceFile
        WebFile sourceFile = getSourceFile();
        if (sourceFile != null)
            jfile.setSourceFile(sourceFile);

        // Set Resolver
        Resolver resolver = getResolver();
        if (resolver != null)
            jfile.setResolver(resolver);

        // This sucks
        JavaTextDocUtils.getStatementsForJavaNode(jfile);

        // Set, return
        return _jfile = jfile;
    }

    /**
     * Returns the parsed statements.
     */
    public JStmt[] getJFileStatements()
    {
        JFile jfile = getJFile();
        JClassDecl classDecl = jfile.getClassDecl();
        JMethodDecl bodyMethod = classDecl.getMethodDeclForNameAndTypes("body", null);

        return JavaTextDocUtils.getStatementsForJavaNode(bodyMethod);
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

        // Get tokenizer
        JavaParser javaParser = getJavaParser();
        CodeTokenizer tokenizer = javaParser.getTokenizer();

        // Get first token in line
        Exception exception = null;
        ParseToken parseToken = null;
        try { parseToken = getNextToken(tokenizer, aTextLine); }
        catch (Exception e) {
            exception = e;
            System.out.println("JavaTextDoc.createTokensForTextLine: Parse error: " + e);
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
            try { parseToken = getNextToken(tokenizer, null); }
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
     * Returns the next token.
     */
    private ParseToken getNextToken(CodeTokenizer aTokenizer, TextLine aTextLine)
    {
        // If TextLine provided, do set up
        if (aTextLine != null) {

            // If this line is InMultilineComment (do this first, since it may require use of Text.Tokenizer)
            TextLine prevTextLine = aTextLine.getPrevious();
            TextToken prevTextLineLastToken = prevTextLine != null ? prevTextLine.getLastToken() : null;
            boolean inUnterminatedComment = isTextTokenUnterminatedMultilineComment(prevTextLineLastToken);

            // Reset input for Tokenizer
            aTokenizer.setInput(aTextLine);

            // Get first line token: Handle if already in Multi-line
            if (inUnterminatedComment)
                return aTokenizer.getMultiLineCommentTokenMore();
        }

        // Return next token
        return aTokenizer.getNextSpecialTokenOrToken();
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
            boolean jfileUpdated = JavaTextDocUtils.updateJFileForChange(this, _jfile, charsChange);
            if (!jfileUpdated)
                _jfile = null;

            // This sucks!
            else JavaTextDocUtils.getStatementsForJavaNode(_jfile);
        }
    }

    /**
     * Returns a new JavaTextDoc from given source.
     */
    public static JavaTextDoc newFromSource(Object aSource)
    {
        // Create TextDoc
        JavaTextDoc javaTextDoc = new JavaTextDoc();
        javaTextDoc.setSource(aSource);

        // Return
        return javaTextDoc;
    }
}
