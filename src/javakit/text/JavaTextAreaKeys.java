/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import javakit.parse.JExprLiteral;
import javakit.parse.JNode;
import javakit.shell.JavaTextDoc;
import snap.text.TextBoxLine;
import snap.text.TextSel;
import snap.view.KeyCode;
import snap.view.ViewEvent;
import snap.view.ViewUtils;

/**
 * This class is a helper for JavaTextArea to handle key processing.
 */
public class JavaTextAreaKeys {

    // The JavaTextArea
    private JavaTextArea  _textArea;

    /**
     * Constructor.
     */
    public JavaTextAreaKeys(JavaTextArea aJTA)
    {
        _textArea = aJTA;
    }

    /** TextArea method. */
    public boolean isSelEmpty()  { return _textArea.isSelEmpty(); }

    /** TextArea method. */
    public TextSel getSel()  { return _textArea.getSel(); }

    /** TextArea method. */
    public void setSel(int charIndex)  { _textArea.setSel(charIndex); }

    /** TextArea method. */
    public int getSelStart()  { return _textArea.getSelStart(); }

    /** TextArea method. */
    public int length()  { return _textArea.length(); }

    /** TextArea method. */
    public char charAt(int charIndex)  { return _textArea.charAt(charIndex); }

    /**
     * Called when a key is pressed.
     */
    protected void keyPressed(ViewEvent anEvent)
    {
        // Get event info
        int keyCode = anEvent.getKeyCode();
        boolean commandDown = anEvent.isShortcutDown();
        boolean shiftDown = anEvent.isShiftDown();

        // Handle tab
        if (keyCode == KeyCode.TAB) {
            if (!anEvent.isShiftDown())
                _textArea.indentLines();
            else _textArea.outdentLines();
            anEvent.consume();
            return;
        }

        // Handle newline special
        if (keyCode == KeyCode.ENTER && isSelEmpty()) {
            if (anEvent.isShiftDown())
                _textArea.selectLineEnd();
            processNewline();
            anEvent.consume();
            return;
        }

        // Handle delete of adjacent paired chars (parens, quotes, square brackets) - TODO: don't do if in string/comment
        boolean isDelete = keyCode == KeyCode.BACK_SPACE || commandDown && !shiftDown && keyCode == KeyCode.X;
        if (isDelete && getSel().getSize() <= 1) {
            int start = getSelStart();
            if (isSelEmpty())
                start--;
            char char1 = start >= 0 && start + 1 < length() ? charAt(start) : 0;
            if (isPairedCharOpener(char1)) {
                char char2 = char1 != 0 ? charAt(start + 1) : 0;
                char closeChar = getPairedCharForOpener(char1);
                if (char2 == closeChar)
                    _textArea.delete(start + 1, start + 2, false);
            }
        }

        // Do normal version
        _textArea.keyPressedSuper(anEvent);
    }

    /**
     * Called when a key is typed.
     */
    protected void keyTyped(ViewEvent anEvent)
    {
        // Get event info
        char keyChar = anEvent.getKeyChar();
        if (keyChar == KeyCode.CHAR_UNDEFINED) return;
        boolean charDefined = !Character.isISOControl(keyChar);
        boolean commandDown = anEvent.isShortcutDown();
        boolean controlDown = anEvent.isControlDown();

        // Handle paired chars
        if (charDefined && !commandDown && !controlDown && isSelEmpty()) {

            // Handle closer char: If next char is identical closer, assume this char is redundant and just return
            // TODO: Don't do if in string/comment - but should also work if chars are no longer adjacent
            if (isPairedCharCloserRedundant(keyChar)) {
                setSel(getSelStart() + 1);
                anEvent.consume();
                return;
            }

            // Handle open bracket: If empty line, remove level of indent
            if (keyChar == '{') {
                TextBoxLine thisLine = getSel().getStartLine();
                String thisLineStr = thisLine.getString();
                if (thisLineStr.trim().length() == 0 && thisLineStr.length() >= 4) {
                    int start = getSelStart();
                    _textArea.delete(thisLine.getStartCharIndex(), thisLine.getStartCharIndex() + 4, false);
                    setSel(start - 4);
                }
            }
        }

        // Do normal version
        _textArea.keyTypedSuper(anEvent);

        // Handle paired chars
        if (charDefined && !commandDown && !controlDown) {

            // If opener char, insert closer char
            boolean isPairedOpener = isPairedCharOpener(keyChar);
            if (isPairedOpener)
                handlePairedCharOpener(keyChar);

            // Handle close bracket: Remove level of indent
            if (keyChar == '}') {

                // Get indent for this line and next
                TextBoxLine thisLine = getSel().getStartLine();
                TextBoxLine prevLine = thisLine.getPrevLine();
                int thisIndent = _textArea.getIndent(thisLine);
                int prevIndent = prevLine != null ? _textArea.getIndent(prevLine) : 0;

                // If this line starts with close bracket and indent is too much, remove indent level
                if (thisLine.getString().trim().startsWith("}") && thisIndent > prevIndent && thisIndent > 4) {
                    int thisLineStart = thisLine.getStartCharIndex();
                    int deleteIndentEnd = thisLineStart + (thisIndent - prevIndent);
                    _textArea.delete(thisLineStart, deleteIndentEnd, false);
                    setSel(getSelStart() - 4);
                }
            }

            // Activate PopupList
            if (!_textArea.getPopup().isShowing() && !anEvent.isSpaceKey())
                ViewUtils.runLater(() -> _textArea.activatePopupList());
        }
    }

    /**
     * Process newline key event.
     */
    protected void processNewline()
    {
        // Get line and its indent
        TextBoxLine line = getSel().getStartLine();
        int indent = _textArea.getIndent(line);

        // Determine if this line is start of code block and/or not terminated
        // TODO: Need real startOfMultilineComment and inMultilineComment
        String lineString = line.getString().trim();
        boolean isStartOfMultiLineComment = lineString.startsWith("/*") && !lineString.endsWith("*/");
        boolean isInMultiLineComment = lineString.startsWith("*") && !lineString.endsWith("*/");
        boolean isEndMultiLineComment = lineString.startsWith("*") && lineString.endsWith("*/");
        boolean isStartOfCodeBlock = lineString.endsWith("{");
        boolean isLineTerminated = lineString.endsWith(";") || lineString.endsWith("}") ||
                lineString.endsWith("*/") || lineString.indexOf("//") >= 0 || lineString.length() == 0;

        // Create indent string
        StringBuffer sb = new StringBuffer().append('\n');
        for (int i = 0; i < indent; i++)
            sb.append(' ');

        // If start of multi-line comment, add " * "
        if (isStartOfMultiLineComment)
            sb.append(" * ");

            // If in multi-line comment, add "* "
        else if (isInMultiLineComment)
            sb.append("* ");

            // If after multi-line comment, remove space from indent
        else if (isEndMultiLineComment) {
            if (sb.length() > 0)
                sb.delete(sb.length() - 1, sb.length());
        }

        // If line not terminated increase indent (not for REPL)
        else if (!isLineTerminated && _textArea.getTextDoc() instanceof JavaTextDoc)
            sb.append(_textArea.INDENT_STRING);

        // Do normal version
        _textArea.replaceChars(sb.toString());

        // If start of multi-line comment, append terminator
        if (isStartOfMultiLineComment) {
            int start = getSelStart();
            String str = sb.substring(0, sb.length() - 1) + "/";
            _textArea.replaceChars(str, null, start, start, false);
            setSel(start);
        }

        // If start of code block, append terminator
        else if (isStartOfCodeBlock && _textArea.getJFile().getException() != null) {
            int start = getSelStart();
            String str = sb.substring(0, sb.length() - 4) + "}";
            _textArea.replaceChars(str, null, start, start, false);
            setSel(start);
        }
    }

    /**
     * Returns whether given char is paired char opener.
     */
    public boolean isPairedCharOpener(char aChar)
    {
        return aChar == '\'' || aChar == '"' || aChar == '(' || aChar == '[';
    }

    /**
     * Returns whether given char is paired char closer.
     */
    public boolean isPairedCharCloser(char aChar)
    {
        return aChar == '\'' || aChar == '"' || aChar == ')' || aChar == ']';
    }

    /**
     * Returns the paired closer char for given opener char.
     */
    public char getPairedCharForOpener(char openerChar)
    {
        switch (openerChar) {
            case '\'': return openerChar;
            case '"': return openerChar;
            case '(': return ')';
            case '[': return ']';
            default: throw new IllegalArgumentException("JavaTextAreaKey.getPairedCharCloser: Illegal char: " + openerChar);
        }
    }
    /**
     * Returns the paired opener char for given closer char.
     */
    public char getPairedCharForCloser(char closerChar)
    {
        switch (closerChar) {
            case '\'': return closerChar;
            case '"': return closerChar;
            case ')': return '(';
            case ']': return '[';
            default: throw new IllegalArgumentException("JavaTextAreaKey.getPairedCharOpener: Illegal char: " + closerChar);
        }
    }

    /**
     * Handles paired char opener: Insert close char as convenience.
     */
    public void handlePairedCharOpener(char aChar)
    {
        String closer = String.valueOf(getPairedCharForOpener(aChar));

        // Add closer char
        int i = _textArea.getSelStart();
        _textArea.replaceChars(closer, null, i, i, false);
        _textArea.setSel(i);
    }

    /**
     * Handles paired char closer: Avoid redundancy of user closing already closed pair.
     */
    public boolean isPairedCharCloserRedundant(char keyChar)
    {
        // If not paired char closer, return false
        if (!isPairedCharCloser(keyChar))
            return false;

        // Get previous char (just return if not identical)
        int start = getSelStart();
        if (start >= length())
            return false;
        char nextChar = charAt(start);
        if (keyChar != nextChar)
            return false;

        // If quote char, return whether we are in literal
        if (keyChar == '\'' || keyChar == '"') {
            JNode selNode = _textArea.getSelNode();
            return selNode instanceof JExprLiteral;
        }

        // Return true
        return true;
    }
}
