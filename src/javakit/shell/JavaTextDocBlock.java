package javakit.shell;
import javakit.parse.JStmtBlock;
import javakit.text.JavaTextArea;
import snap.text.SubText;
import snap.text.TextDoc;
import snap.text.TextLine;
import snap.view.Clipboard;

/**
 * This class represents a block of code in the JavaTextDoc.
 */
public class JavaTextDocBlock {

    // The JavaTextDoc that contains this object
    private JavaTextDoc  _javaDoc;

    // The SubText of full Java text for code block
    private SubText  _subText;

    // The JavaTextArea
    protected JavaTextArea  _textArea;

    /**
     * Constructor.
     */
    public JavaTextDocBlock(JavaTextDoc aJavaDoc, JStmtBlock blockStmt)
    {
        // Set doc
        _javaDoc = aJavaDoc;

        // Set start/end char indexes for code block ( " { ... } ")
        int blockStart = blockStmt.getStart();
        int blockEnd = blockStmt.getEnd();

        // Get start/end lines indexes
        TextLine startLine = _javaDoc.getLineForCharIndex(blockStart);
        TextLine endLine = _javaDoc.getLineForCharIndex(blockEnd);

        // Get code start/end char index (inside of brackets)
        int codeStartIndex = startLine.getEnd();
        int codeEndIndex = endLine.getStart();

        // Create subText
        _subText = new SubText(_javaDoc, codeStartIndex, codeEndIndex);
    }

    /**
     * Returns the start line.
     */
    public TextLine getStartLine()
    {
        int startCharIndex = _subText.getStartCharIndex();
        TextLine startLine = _subText.getTextDoc().getLineForCharIndex(startCharIndex);
        return startLine;
    }

    /**
     * Returns the end line.
     */
    public TextLine getEndLine()
    {
        int endCharIndex = _subText.getEndCharIndex();
        TextLine endLine = _subText.getTextDoc().getLineForCharIndex(endCharIndex);
        return endLine;
    }

    /**
     * Returns the start char index.
     */
    public int getStartCharIndex()  { return _subText.getStartCharIndex(); }

    /**
     * Returns the end char index.
     */
    public int getEndCharIndex()  { return _subText.getEndCharIndex(); }

    /**
     * Returns the length.
     */
    public int length()
    {
        int startCharIndex = getStartCharIndex();
        int endCharIndex = getEndCharIndex();
        return endCharIndex - startCharIndex;
    }

    /**
     * Returns the text doc.
     */
    public SubText getTextDoc()  { return _subText; }

    /**
     * Returns a JavaTextArea.
     */
    public JavaTextArea getTextArea()
    {
        // If already set, just return
        if (_textArea != null) return _textArea;

        // Create/config
        _textArea = new JavaTextAreaX();
        _textArea.setBorderRadius(4);
        _textArea.setShowPrintMargin(false);
        _textArea.setFocusPainted(true);
        _textArea.setTextDoc(_subText);

        // Return
        return _textArea;
    }

    /**
     * Returns the string for block.
     */
    public String getString()
    {
        return _subText.getString();
    }

    /**
     * Removes the trailing newline.
     */
    public void removeTrailingNewline()
    {
        // If empty, just return
        if (length() == 0) return;

        // If last char not newline, just return
        TextDoc textDoc = _javaDoc;
        int endCharIndex = getEndCharIndex() - 1;
        char endChar = _javaDoc.charAt(endCharIndex);
        if (endChar != '\n')
            return;

        // Remove last char
        textDoc.removeChars(endCharIndex, endCharIndex + 1);
    }

    /**
     * Returns whether block is empty.
     */
    public boolean isEmpty()
    {
        String string = getString().trim();
        return string.length() == 0;
    }

    /**
     * This JavaTextArea subclass is slightly modified for working with code snippets.
     */
    private static class JavaTextAreaX extends JavaTextArea {

        /**
         * Override to get string first.
         */
        @Override
        protected Object getClipboardContent(Clipboard clipboard)
        {
            // Try String first
            if (clipboard.hasString()) {
                String str = clipboard.getString();
                if (str != null && str.length() > 0)
                    return str;
            }

            // Do normal version
            return super.getClipboardContent(clipboard);
        }

        /**
         * Override to remove extra indent from pasted strings.
         */
        @Override
        protected void replaceCharsWithContent(Object theContent)
        {
            // If String, trim extra indent
            if (theContent instanceof String)
                theContent = removeExtraIndentFromString((String) theContent);

            // Do normal version
            super.replaceCharsWithContent(theContent);
        }
    }

    /**
     * Removes superfluous indent from a string.
     */
    private static String removeExtraIndentFromString(String str)
    {
        // Get string as lines
        String[] lines = str.split("\n");
        int minIndent = 99;

        // Get minimum indent for given lines
        for (String line : lines) {
            int indent = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ')
                    indent++;
                else break;
            }
            minIndent = Math.min(minIndent, indent);
        }

        // If there is superfluous indent, remove from lines and reset string
        if (minIndent > 0) {

            // Get indent string
            String indentStr = " ";
            for (int i = 1; i < minIndent; i++) indentStr += ' ';

            // Remove indent string from lines
            for (int i = 0; i < lines.length; i++)
                lines[i] = lines[i].replaceFirst(indentStr, "");

            // Rebuild string
            str = String.join("\n", lines);
        }

        // Return
        return str;
    }
}
