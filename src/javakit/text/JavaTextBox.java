/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import javakit.parse.JavaParser;
import snap.parse.*;
import snap.text.*;

/**
 * This TextBox subclass provides syntax coloring for Java code.
 */
public class JavaTextBox extends TextBox {

    // The JavaParser for this text
    protected JavaParser  _parser = new JavaParser();

    /**
     * Constructor.
     */
    public JavaTextBox()
    {
        super();
    }

    /**
     * Create and return TextBoxLine for given TextLine, start char index and line index.
     */
    protected TextBoxLine createTextBoxLine(TextLine aTextLine, int startCharIndex, int aLineIndex)
    {
        // Create new line (just return if last line in text)
        TextRun textRun = aTextLine.getRun(0);
        TextStyle textStyle = textRun.getStyle();
        TextBoxLine newBoxLine = new TextBoxLine(this, textStyle, aTextLine, startCharIndex);
        if (startCharIndex > 0) {
            newBoxLine.resetSizes();
            return newBoxLine;
        }

        // If this line is InMultilineComment (do this first, since it may require use of Text.Tokenizer)
        TextBoxLine prevBoxLine = aLineIndex > 0 ? getLine(aLineIndex - 1) : null;
        JavaTextBoxToken lastToken = prevBoxLine != null ? (JavaTextBoxToken) prevBoxLine.getTokenLast() : null;
        boolean isUnterminatedComment = isTextTokenUnterminatedMultilineComment(lastToken);

        // Get tokenizer
        CodeTokenizer tokenizer = _parser.getTokenizer();
        tokenizer.setInput(aTextLine);

        // Get first line token: Handle if already in Multi-line
        Exception exception = null;
        ParseToken parseToken = isUnterminatedComment ?
                tokenizer.getMultiLineCommentTokenMore(null) :
                tokenizer.getNextSpecialToken();
        if (parseToken == null) {
            try { parseToken = tokenizer.getNextToken(); }
            catch (Exception e) { exception = e; }
        }

        // Get iteration variables
        int charIndex = 0;
        double tokenX = 0;

        // Get line parse tokens and create TextTokens
        while (parseToken != null) {

            // Get token start/end
            int tokenStart = parseToken.getStartCharIndex();
            int tokenEnd = parseToken.getEndCharIndex();

            // Get token x
            while (charIndex < tokenStart) {
                char lineChar = aTextLine.charAt(charIndex);
                if (lineChar == '\t')
                    tokenX += textStyle.getCharAdvance(' ') * 4;
                else tokenX += textStyle.getCharAdvance(lineChar);
                charIndex++;
            }

            // Get token width
            double tokenW = 0;
            while (charIndex < tokenEnd) {
                char lineChar = aTextLine.charAt(charIndex);
                tokenW += textStyle.getCharAdvance(lineChar);
                charIndex++;
            }

            // Create TextToken
            TextBoxToken textBoxToken = new JavaTextBoxToken(newBoxLine, textStyle, tokenStart, tokenEnd, parseToken);
            textBoxToken.setX(tokenX);
            textBoxToken.setWidth(tokenW);
            tokenX += tokenW;

            // Add token
            newBoxLine.addToken(textBoxToken);

            // Get next token
            parseToken = tokenizer.getNextSpecialToken();
            if (parseToken == null) {
                try { parseToken = tokenizer.getNextToken(); }
                catch (Exception e) {
                    exception = e;
                    break;
                }
            }
        }

        // If exception was hit, create token for rest of line
        if (exception != null) {

            // Get token width
            double tokenW = 0;
            int tokenStart = charIndex;
            int tokenEnd = aTextLine.length();
            while (charIndex < aTextLine.length()) {
                char lineChar = aTextLine.charAt(charIndex);
                tokenW += textStyle.getCharAdvance(lineChar);
                charIndex++;
            }

            // Create TextToken
            TextBoxToken textBoxToken = new JavaTextBoxToken(newBoxLine, textStyle, tokenStart, tokenEnd, null);
            textBoxToken.setX(tokenX);
            textBoxToken.setWidth(tokenW);
            newBoxLine.addToken(textBoxToken);
        }

        // Return line
        newBoxLine.resetSizes();
        return newBoxLine;
    }

    /**
     * Returns whether given TextToken is an unterminated comment.
     */
    private boolean isTextTokenUnterminatedMultilineComment(ParseToken aTextToken)
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
}