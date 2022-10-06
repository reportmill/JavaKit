package javakit.text;
import javakit.parse.JavaParser;
import snap.parse.Token;
import snap.parse.Tokenizer;
import snap.text.TextBoxLine;
import snap.text.TextBoxToken;
import snap.text.TextStyle;

/**
 * A JavaParser specifically for JavaTextBox.
 */
public class JavaTextBoxParser extends JavaParser {

    // The text tokenizer
    private JavaTextBoxTokenizer  _textTokenizer;

    /**
     * Constructor.
     */
    public JavaTextBoxParser(JavaTextBox javaTextBox)
    {
        super();
        _textTokenizer = new JavaTextBoxTokenizer(javaTextBox);
    }

    /**
     * Returns tokenizer that gets tokens from text.
     */
    public JavaTextBoxTokenizer getTokenizer()  { return _textTokenizer; }

    /**
     * Returns the original tokenizer.
     */
    public JavaTokenizer getRealTokenizer()
    {
        return super.getTokenizer();
    }

    /**
     * Creates a JavaTextBoxToken.
     */
    public TextBoxToken createJavaTextBoxToken(Token parseToken, TextBoxLine line, TextStyle style, int tokenStart, int tokenEnd)
    {
        JavaTextBoxToken textBoxToken = new JavaTextBoxToken(line, style, tokenStart, tokenEnd);
        textBoxToken._tokenizer = getTokenizer();
        textBoxToken._token = parseToken;
        return textBoxToken;
    }

    /**
     * A tokenizer that gets tokens from text lines.
     */
    private static class JavaTextBoxTokenizer extends JavaTokenizer {

        // The JavaTextBox
        private JavaTextBox  _javaTextBox;

        // The current line
        protected TextBoxLine  _line;

        // The token index on line
        protected int  _tokenIndex;

        /**
         * Constructor.
         */
        public JavaTextBoxTokenizer(JavaTextBox javaTextBox)
        {
            _javaTextBox = javaTextBox;
        }

        /**
         * Override to reset tokenizer.
         */
        @Override
        public void setInput(CharSequence anInput)
        {
            super.setInput(anInput);
            _line = null;
        }

        /**
         * Sets the input start.
         */
        @Override
        public void setCharIndex(int aStart)
        {
            _line = _javaTextBox.getLineForCharIndex(aStart);
            _tokenIndex = 0;
            while (_tokenIndex < _line.getTokenCount()) {
                if (aStart < _line.getToken(_tokenIndex).getEnd() + _line.getStart())
                    break;
                _tokenIndex++;
            }
        }

        /**
         * Override to get token from next line.
         */
        @Override
        public Token getNextToken()
        {
            // If line is out of tokens, get next line
            if (_line == null || _tokenIndex >= _line.getTokenCount()) {
                TextBoxLine line = getNextLine(_line);
                while (line != null && line.getTokenCount() == 0)
                    line = getNextLine(line);
                if (line == null) return null;
                _line = line;
                _tokenIndex = 0;
            }

            // Return token for line
            JavaTextBoxToken token = (JavaTextBoxToken) _line.getToken(_tokenIndex++);
            if (token.isSpecialToken())
                return getNextToken();

            // Return
            return token;
        }

        /**
         * Returns the next line.
         */
        private TextBoxLine getNextLine(TextBoxLine aLine)
        {
            int index = aLine != null ? aLine.getIndex() + 1 : 0;
            return index < _javaTextBox.getLineCount() ? _javaTextBox.getLine(index) : null;
        }
    }

    /**
     * A TextToken subclass specifically for JavaText.
     */
    private static class JavaTextBoxToken extends TextBoxToken implements Token {

        // The tokenizer that provided this token
        protected Tokenizer  _tokenizer;

        // The parse token
        protected Token  _token;

        /**
         * Creates a new Token for given box line, run and character start/end.
         */
        public JavaTextBoxToken(TextBoxLine aLine, TextStyle aStyle, int aStart, int aEnd)
        {
            super(aLine, aStyle, aStart, aEnd);
        }

        /**
         * The Tokenizer that provided this token.
         */
        public Tokenizer getTokenizer()  { return _tokenizer; }

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
        public int getInputStart()
        {
            return getLine().getStart() + getStart();
        }

        /**
         * Parse Token method.
         */
        public int getInputEnd()
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
        public int getLineStart()
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
        public Token getSpecialToken()  { return null; }

        /**
         * Returns whether this token is SpecialToken (Java comment).
         */
        public boolean isSpecialToken()
        {
            String name = getName();
            return name != null && name.endsWith("Comment");
        }
    }
}
