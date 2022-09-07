package javakit.shell;
import javakit.parse.JStmtBlock;
import javakit.text.JavaTextArea;
import snap.text.SubText;
import snap.text.TextLine;

/**
 * This class represents a block of code in the JavaTextDoc.
 */
public class JavaTextDocBlock {

    // The JavaTextDoc that contains this object
    private JavaTextDoc  _javaDoc;

    // The SubText of full Java text for code block
    private SubText  _subText;

    // The start/end line of block
    private TextLine  _startLine, _endLine;

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
        _startLine = _javaDoc.getLineForCharIndex(blockStart);
        _endLine = _javaDoc.getLineForCharIndex(blockEnd);

        // Get code start/end char index (inside of brackets)
        int codeStartIndex = _startLine.getEnd();
        int codeEndIndex = _endLine.getStart();

        // Create subText
        _subText = new SubText(_javaDoc, codeStartIndex, codeEndIndex);
    }

    /**
     * Returns the start line.
     */
    public TextLine getStartLine()  { return _startLine; }

    /**
     * Returns the end line.
     */
    public TextLine getEndLine()  { return _endLine; }

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
        _textArea = new JavaTextArea();
        _textArea.setRoundingRadius(4);
        _textArea.setShowPrintMargin(false);
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
     * Returns whether block is empty.
     */
    public boolean isEmpty()
    {
        String string = getString().trim();
        return string.length() == 0;
    }
}
