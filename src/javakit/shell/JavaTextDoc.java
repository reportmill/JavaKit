package javakit.shell;
import javakit.parse.JClassDecl;
import javakit.parse.JInitializerDecl;
import javakit.parse.JFile;
import javakit.parse.JavaParser;
import snap.props.PropChange;
import snap.text.TextDoc;
import snap.text.TextDocUtils;
import snap.util.ArrayUtils;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextDoc extends TextDoc {

    // The parsed Java file
    private JFile  _jfile;

    // The blocks
    private Block[]  _blocks = new Block[0];

    /**
     * Constructor.
     */
    public JavaTextDoc()
    {
        super();
    }

    /**
     * Returns the JFile (parsed Java file).
     */
    public JFile getJFile()
    {
        // If already set, just return
        if (_jfile != null) return _jfile;

        // Get parsed java file
        JavaParser javaParser = JavaParser.getShared();
        JFile jfile = javaParser.getJavaFile(this);

        // Set, return
        return _jfile = jfile;
    }

    /**
     * Override to parse blocks.
     */
    @Override
    public void setString(String aString)
    {
        // Do normal version
        super.setString(aString);
        _jfile = null;

        // Get JFile
        JFile jfile = getJFile();

        // Get static decls
        JClassDecl classDecl = jfile.getClassDecl();
        JInitializerDecl[] initDecls = classDecl.getInitDecls();

        // Create blocks
        for (JInitializerDecl initDecl : initDecls) {

            // Create block
            Block block = new Block();
            block._start = initDecl.getStart();
            block._end = initDecl.getStart();
            addBlock(block);
        }
    }

    /**
     * Returns the blocks.
     */
    public Block[] getBlocks()  { return _blocks; }

    /**
     * Returns the number of blocks.
     */
    public int getBlockCount()  { return _blocks.length; }

    /**
     * Returns the individual block at given index.
     */
    public Block getBlock(int anIndex)  { return _blocks[anIndex]; }

    /**
     * Adds a block at end of blocks.
     */
    public void addBlock(Block aBlock)
    {
        addBlock(aBlock, getBlockCount());
    }

    /**
     * Adds a block at given index.
     */
    public void addBlock(Block aBlock, int anIndex)
    {
        _blocks = ArrayUtils.add(_blocks, aBlock, anIndex);
    }

    /**
     * Removes the block at given index.
     */
    public Block removeBlock(int anIndex)
    {
        Block block = getBlock(anIndex);
        _blocks = ArrayUtils.remove(_blocks, anIndex);
        return block;
    }

    /**
     * Override to detect char changes.
     */
    @Override
    protected void firePropChange(PropChange aPC)
    {
        // Do normal version
        super.firePropChange(aPC);

        // Handle CharsChange
        String propName = aPC.getPropName();
        if (propName == Chars_Prop)
            textDidChange((TextDocUtils.CharsChange) aPC);
    }

    /**
     * Called when text changes.
     */
    private void textDidChange(TextDocUtils.CharsChange aCharsChange)
    {
        // Clear JFile
        _jfile = null;

        // Call didAddChars/didRemoveChars
        int charIndex = aCharsChange.getIndex();
        CharSequence addChars = aCharsChange.getNewValue();
        CharSequence removeChars = aCharsChange.getOldValue();
        if (addChars != null)
            textDidAddChars(addChars, charIndex);
        else textDidRemoveChars(removeChars, charIndex);
    }

    /**
     * Called when characters are added to text.
     */
    protected void textDidAddChars(CharSequence theChars, int charIndex)
    {

    }

    /**
     * Called when characters are removed from text.
     */
    protected void textDidRemoveChars(CharSequence theChars, int charIndex)
    {

    }

    /**
     * This class represents a block of code in the TextDoc.
     */
    public class Block {

        // The start/end char index
        private int  _start, _end;

        // The start/end line index
        private int  _startLineIndex, _endLineIndex;

        /**
         * Constructor.
         */
        public Block()  { }


    }
}
