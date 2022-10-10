package javakit.text;
import snap.parse.ParseToken;
import snap.text.TextBoxLine;
import snap.text.TextBoxToken;
import snap.text.TextStyle;

/**
 * A TextToken subclass specifically for JavaText.
 */
public class JavaTextBoxToken extends TextBoxToken implements ParseToken {

    // The parse token
    protected ParseToken  _parseToken;

    /**
     * Creates a new Token for given box line, run and character start/end.
     */
    public JavaTextBoxToken(TextBoxLine aLine, TextStyle aStyle, int aStart, int aEnd, ParseToken parseToken)
    {
        super(aLine, aStyle, aStart, aEnd);
        _parseToken = parseToken;
    }

    /**
     * Parse Token method.
     */
    public String getName()
    {
        return _parseToken != null ? _parseToken.getName() : null;
    }

    /**
     * Parse Token method.
     */
    public String getPattern()
    {
        return _parseToken != null ? _parseToken.getPattern() : null;
    }

    /**
     * Parse Token method.
     */
    public int getStartCharIndex()
    {
        return getLine().getStartCharIndex() + getStart();
    }

    /**
     * Parse Token method.
     */
    public int getEndCharIndex()
    {
        return getLine().getStartCharIndex() + getEnd();
    }

    /**
     * Parse Token method.
     */
    public int getLineIndex()
    {
        return getLine().getIndex();
    }

    /**
     * Parse Token method.
     */
    public int getLineStartCharIndex()
    {
        return getLine().getStartCharIndex();
    }

    /**
     * Parse Token method.
     */
    public int getColumnIndex()
    {
        return getStart();
    }

    /**
     * Parse Token method.
     */
    public ParseToken getSpecialToken()
    {
        return null;
    }
}
