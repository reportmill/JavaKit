/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import snap.parse.*;
import snap.gfx.*;
import snap.text.*;
import javakit.parse.JavaParser.JavaTokenizer;

/**
 * This TextBox subclass provides syntax coloring for Java code.
 */
public class JavaTextBox extends TextBox {

    // The JavaParser for this text
    protected JavaTextBoxParser  _parser = new JavaTextBoxParser(this);

    // Constants for Syntax Coloring
    private static Color COMMENT_COLOR = new Color("#3F7F5F"); //336633
    private static Color RESERVED_WORD_COLOR = new Color("#660033");
    private static Color STRING_LITERAL_COLOR = new Color("#C80000"); // CC0000

    /**
     * Constructor.
     */
    public JavaTextBox()
    {
        super();
    }

    /**
     * Override to return JavaTextLine.
     */
    @Override
    public JavaTextBoxLine getLine(int anIndex)
    {
        return (JavaTextBoxLine) super.getLine(anIndex);
    }

    /**
     * Override to return JavaTextLine.
     */
    @Override
    public JavaTextBoxLine getLineForCharIndex(int anIndex)
    {
        return (JavaTextBoxLine) super.getLineForCharIndex(anIndex);
    }

    /**
     * Override to adjust build issues start/end.
     */
    @Override
    public void updateLines(int aStart, int endOld, int endNew)
    {
        // Get whether update is Add or Remove
        boolean isAdd = endNew > endOld;
        int length = boxlen();

        // Get whether last line in update range has unterminated comment
        JavaTextBoxLine textBoxLine = getLineCount() > 0 ? getLineForCharIndex(endOld) : null;
        boolean utermComment = textBoxLine != null && textBoxLine.isUnterminatedComment();

        // Do normal version (just return if setting everything)
        super.updateLines(aStart, endOld, endNew);
        if (isAdd && length == 0)
            return;

        // If unterminated comment state changed, update successive lines until it stops
        JavaTextBoxLine newEndLine = getLineForCharIndex(endNew);
        if (utermComment != newEndLine.isUnterminatedComment()) {
            int start = newEndLine.getEnd();
            int end = getTextDoc().indexOf("*/", start);
            if (end < 0)
                end = length();
            super.updateLines(start, end, end);
        }
    }

    /**
     * Create and return TextBoxLine for given TextLine, start char index and line index.
     */
    protected TextBoxLine createTextBoxLine(TextLine aTextLine, int aStart, int aLineIndex)
    {
        // Get iteration variables
        TextStyle style = aTextLine.getRun(0).getStyle();
        Exception exception = null;
        int start = 0;
        double x = 0;

        // Create new line (just return if last line in text)
        JavaTextBoxLine line = new JavaTextBoxLine(this, style, aTextLine, aStart);
        if (aStart > 0) {
            line.resetSizes();
            return line;
        }

        // If this line is InMultilineComment (do this first, since it may require use of Text.Tokenizer)
        JavaTextBoxLine prevTextBoxLine = aLineIndex > 0 ? getLine(aLineIndex - 1) : null;
        if (prevTextBoxLine != null && prevTextBoxLine.isUnterminatedComment())
            line._utermCmnt = true;

        // Get tokenizer
        JavaTokenizer tokenizer = _parser.getRealTokenizer();
        tokenizer.setInput(aTextLine);

        // Get first line token: Handle if already in Multi-line
        ParseToken token = line.isUnterminatedComment() ?
                tokenizer.getMultiLineCommentTokenMore(null) :
                tokenizer.getNextSpecialToken();
        if (token == null) {
            try { token = tokenizer.getNextToken(); }
            catch (Exception e) { exception = e; }
        }

        // Get line parse tokens and create TextTokens
        while (token != null) {

            // Get token start/end
            int tokenStart = token.getStartCharIndex();
            int tokenEnd = token.getEndCharIndex();

            // Get token x
            while (start < tokenStart) {
                char c = aTextLine.charAt(start); //if(start>run.getEnd()) run = run.getNext();
                if (c == '\t') x += style.getCharAdvance(' ') * 4;
                else x += style.getCharAdvance(c);
                start++;
            }

            // Get token width
            double w = 0;
            while (start < tokenEnd) {
                char c = aTextLine.charAt(start); //if(start>run.getEnd()) run = run.getNext();
                w += style.getCharAdvance(c);
                start++;
            }

            // Create TextToken
            TextBoxToken textBoxToken = _parser.createJavaTextBoxToken(token, line, style, tokenStart, tokenEnd);
            textBoxToken.setX(x);
            textBoxToken.setWidth(w);
            x += w;
            w = 0;

            // Get/set token color
            Color color = getColor(token);
            if (color != null)
                textBoxToken.setColor(color);

            // Add token
            line.addToken(textBoxToken);

            // Update inMultilineComment for current token
            line._utermCmnt = token.getName() == "MultiLineComment" && !token.getString().endsWith("*/");

            // Get next token
            token = tokenizer.getNextSpecialToken();
            if (token == null) {
                try { token = tokenizer.getNextToken(); }
                catch (Exception e) {
                    exception = e;
                    break;
                }
            }
        }

        // If exception was hit, create token for rest of line
        if (exception != null) {

            // Get token width
            double w = 0;
            int tokenStart = start;
            int tokenEnd = aTextLine.length();
            while (start < aTextLine.length()) {
                char c = aTextLine.charAt(start); //if(start>run.getEnd()) run = run.getNext();
                w += style.getCharAdvance(c);
                start++;
            }

            // Create TextToken
            TextBoxToken textBoxToken = _parser.createJavaTextBoxToken(null, line, style, tokenStart, tokenEnd);
            textBoxToken.setX(x);
            textBoxToken.setWidth(w);
            x += w;
            w = 0;
            line.addToken(textBoxToken);
        }

        // Return line
        line.resetSizes();
        return line;
    }

    /**
     * Checks the given token for syntax coloring.
     */
    private static Color getColor(ParseToken aToken)
    {
        // Handle comments
        String tokenName = aToken.getName();
        if (tokenName == "SingleLineComment" || tokenName == "MultiLineComment")
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
}