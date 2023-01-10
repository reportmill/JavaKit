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
import snap.web.WebURL;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextDoc extends TextDoc {

    // The parsed Java file
    protected JFile  _jfile;

    // The parser to parse Java
    private JavaParser  _javaParser;

    // The Resolver
    private Resolver  _resolver;

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
        JavaParser javaParser = getJavaParserImpl();
        return _javaParser = javaParser;
    }

    /**
     * Returns the parser to parse java file.
     */
    protected JavaParser getJavaParserImpl()  { return JavaParser.getShared(); }

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

        // Create, Set, return
        JFile jfile = createJFile();
        return _jfile = jfile;
    }

    /**
     * Parses and returns JFile.
     */
    protected JFile createJFile()
    {
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
        jfile.setJavaFileString(javaStr);

        // Return
        return jfile;
    }

    /**
     * Returns the parsed statements.
     */
    public JStmt[] getJFileStatements()
    {
        // Get main method
        JFile jfile = getJFile();
        JClassDecl classDecl = jfile.getClassDecl();
        JMethodDecl bodyMethod = classDecl.getMethodDeclForNameAndTypes("body", null);

        // Get statements from main method
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
        try { parseToken = JavaTextDocUtils.getNextToken(tokenizer, aTextLine); }
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
            Color color = JavaTextDocUtils.getColorForParseToken(parseToken);
            if (color != null)
                textToken.setTextColor(color);

            // Get next token
            try { parseToken = JavaTextDocUtils.getNextToken(tokenizer, null); }
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
            TextDocUtils.CharsChange charsChange = (TextDocUtils.CharsChange) aPC;
            updateJFileForChange(charsChange);
        }
    }

    /**
     * Updates JFile incrementally if possible.
     */
    protected void updateJFileForChange(TextDocUtils.CharsChange charsChange)
    {
        // If partial parse fails, clear JFile for full reparse
        boolean jfileUpdated = JavaTextDocUtils.updateJFileForChange(this, _jfile, charsChange);
        if (!jfileUpdated)
            _jfile = null;
    }

    /**
     * Returns a new JavaTextDoc from given source.
     */
    public static JavaTextDoc newFromSource(Object aSource)
    {
        // Get Source URL
        WebURL url = WebURL.getURL(aSource);

        // Create TextDoc and read from URL
        JavaTextDoc javaTextDoc = new JavaTextDoc();
        javaTextDoc.readFromSourceURL(url);

        // Return
        return javaTextDoc;
    }
}
