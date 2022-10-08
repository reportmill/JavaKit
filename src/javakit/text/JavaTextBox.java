/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import javakit.parse.JavaParser;
import javakit.shell.JavaTextDoc;
import snap.parse.*;
import snap.gfx.*;
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
        double tokenX = 0;

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
        CodeTokenizer tokenizer = _parser.getTokenizer();
        tokenizer.setInput(aTextLine);

        // Get first line token: Handle if already in Multi-line
        ParseToken parseToken = line.isUnterminatedComment() ?
                tokenizer.getMultiLineCommentTokenMore(null) :
                tokenizer.getNextSpecialToken();
        if (parseToken == null) {
            try { parseToken = tokenizer.getNextToken(); }
            catch (Exception e) { exception = e; }
        }

        // Get line parse tokens and create TextTokens
        while (parseToken != null) {

            // Get token start/end
            int tokenStart = parseToken.getStartCharIndex();
            int tokenEnd = parseToken.getEndCharIndex();

            // Get token x
            while (start < tokenStart) {
                char c = aTextLine.charAt(start); //if(start>run.getEnd()) run = run.getNext();
                if (c == '\t') tokenX += style.getCharAdvance(' ') * 4;
                else tokenX += style.getCharAdvance(c);
                start++;
            }

            // Get token width
            double tokenW = 0;
            while (start < tokenEnd) {
                char c = aTextLine.charAt(start); //if(start>run.getEnd()) run = run.getNext();
                tokenW += style.getCharAdvance(c);
                start++;
            }

            // Create TextToken
            TextBoxToken textBoxToken = new JavaTextBoxToken(line, style, tokenStart, tokenEnd, parseToken);
            textBoxToken.setX(tokenX);
            textBoxToken.setWidth(tokenW);
            tokenX += tokenW;

            // Get/set token color
            Color color = JavaTextDoc.getColorForParseToken(parseToken);
            if (color != null)
                textBoxToken.setColor(color);

            // Add token
            line.addToken(textBoxToken);

            // Update inMultilineComment for current token
            line._utermCmnt = parseToken.getName() == CodeTokenizer.MULTI_LINE_COMMENT && !parseToken.getString().endsWith("*/");

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
            int tokenStart = start;
            int tokenEnd = aTextLine.length();
            while (start < aTextLine.length()) {
                char lineChar = aTextLine.charAt(start); //if(start>run.getEnd()) run = run.getNext();
                tokenW += style.getCharAdvance(lineChar);
                start++;
            }

            // Create TextToken
            TextBoxToken textBoxToken = new JavaTextBoxToken(line, style, tokenStart, tokenEnd, null);
            textBoxToken.setX(tokenX);
            textBoxToken.setWidth(tokenW);
            line.addToken(textBoxToken);
        }

        // Return line
        line.resetSizes();
        return line;
    }
}