package javakit.shell;
import javakit.parse.JStmtBlock;
import javakit.text.JavaTextArea;
import snap.text.TextBox;
import snap.text.TextLine;

/**
 * This class represents a block of code in the JavaTextDoc.
 */
public class JavaTextDocBlock {

    // The JavaTextDoc that contains this object
    private JavaTextDoc  _javaDoc;

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

        // Set start/end line
        int startCharIndex = blockStmt.getStart();
        int endCharIndex = blockStmt.getEnd();
        _startLine = _javaDoc.getLineForCharIndex(startCharIndex);
        _endLine = _javaDoc.getLineForCharIndex(endCharIndex);
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
    public int getStartCharIndex()  { return _startLine.getEnd(); }

    /**
     * Returns the end char index.
     */
    public int getEndCharIndex()  { return _endLine.getStart(); }

    /**
     * Returns the number of lines.
     */
    public int getLineCount()
    {
        int startLineIndex = _startLine.getIndex();
        int endLineIndex = _endLine.getIndex();
        return endLineIndex - startLineIndex;
    }

    /**
     * Returns a JavaTextArea.
     */
    public JavaTextArea getTextArea()
    {
        // If already set, just return
        if (_textArea != null) return _textArea;

        // Create/config
        _textArea = new JavaTextArea();
        _textArea.setTextDoc(_javaDoc);
        resetTextBoxRange();

        // Return
        return _textArea;
    }

    /**
     * Resets the TextBox start/end char index.
     */
    public void resetTextBoxRange()
    {
        // If no TextArea, just return
        if (_textArea == null) return;

        // Set TextBox char range
        TextBox textBox = _textArea.getTextBox();
        int startCharIndex = getStartCharIndex();
        int endCharIndex = getEndCharIndex();
        textBox.setStartCharIndex(startCharIndex);
        textBox.setEndCharIndex(endCharIndex);
    }

    /**
     * Returns the string for block.
     */
    public String getString()
    {
        int startCharIndex = getStartCharIndex();
        int endCharIndex = getEndCharIndex();
        String str = _javaDoc.subSequence(startCharIndex, endCharIndex).toString().trim();
        return str;
    }
}
