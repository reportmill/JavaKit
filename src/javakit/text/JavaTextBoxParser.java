package javakit.text;
import javakit.parse.JavaParser;
import snap.parse.ParseToken;
import snap.text.TextBoxLine;
import snap.text.TextBoxToken;
import snap.text.TextStyle;

/**
 * A JavaParser specifically for JavaTextBox.
 */
public class JavaTextBoxParser extends JavaParser {

    /**
     * Constructor.
     */
    public JavaTextBoxParser()
    {
        super();
    }

    /**
     * Creates a JavaTextBoxToken.
     */
    public TextBoxToken createJavaTextBoxToken(ParseToken parseToken, TextBoxLine line, TextStyle style, int tokenStart, int tokenEnd)
    {
        JavaTextBoxToken textBoxToken = new JavaTextBoxToken(line, style, tokenStart, tokenEnd);
        textBoxToken._token = parseToken;
        return textBoxToken;
    }

    /**
     * A TextToken subclass specifically for JavaText.
     */
    public static class JavaTextBoxToken extends TextBoxToken implements ParseToken {

        // The parse token
        protected ParseToken _token;

        /**
         * Creates a new Token for given box line, run and character start/end.
         */
        public JavaTextBoxToken(TextBoxLine aLine, TextStyle aStyle, int aStart, int aEnd)
        {
            super(aLine, aStyle, aStart, aEnd);
        }

        /**
         * Parse Token method.
         */
        public String getName()
        {
            return _token != null ? _token.getName() : null;
        }

        /**
         * Parse Token method.
         */
        public String getPattern()
        {
            return _token != null ? _token.getPattern() : null;
        }

        /**
         * Parse Token method.
         */
        public int getStartCharIndex()
        {
            return getLine().getStart() + getStart();
        }

        /**
         * Parse Token method.
         */
        public int getEndCharIndex()
        {
            return getLine().getStart() + getEnd();
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
            return getLine().getStart();
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
        public ParseToken getSpecialToken()  { return null; }
    }
}
