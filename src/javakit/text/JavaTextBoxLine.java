package javakit.text;
import snap.text.TextBox;
import snap.text.TextBoxLine;
import snap.text.TextLine;
import snap.text.TextStyle;

/**
 * A TextLine subclass specifically for JavaText.
 */
public class JavaTextBoxLine extends TextBoxLine {

    // Whether line is unterminated comment
    protected boolean  _utermCmnt;

    /**
     * Constructor.
     */
    public JavaTextBoxLine(TextBox aBox, TextStyle aStartStyle, TextLine aTextLine, int theRTLStart)
    {
        super(aBox, aStartStyle, aTextLine, theRTLStart);
    }

    /**
     * Returns whether line is an unterminated comment.
     */
    public boolean isUnterminatedComment()
    {
        return _utermCmnt;
    }
}
