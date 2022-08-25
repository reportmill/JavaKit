package javakit.shell;
import javakit.parse.*;
import javakit.text.JavaTextArea;
import snap.props.PropChange;
import snap.text.TextBox;
import snap.text.TextDoc;
import snap.text.TextDocUtils;
import snap.text.TextLine;
import snap.util.ArrayUtils;

import java.util.stream.Stream;

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

        // Get initializer decls statement blocks
        JStmtBlock[] initDeclsBlocks = getInitDeclBlocks();

        // Iterate over init decls statement blocks and create block
        for (JStmtBlock blockStmt : initDeclsBlocks) {
            Block block = new Block(blockStmt);
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
     * Returns the last block.
     */
    public Block getBlockLast()
    {
        int blockCount = getBlockCount();
        return blockCount > 0 ? getBlock(blockCount - 1) : null;
    }

    /**
     * Returns the empty block at end.
     */
    public Block getEmptyBlock()
    {
        // Iterate over blocks and return empty one
        Block lastBlock = getBlockLast();
        if (lastBlock != null && lastBlock.getString().length() == 0)
            return lastBlock;

        // Otherwise add empty and return
        Block emptyBlock = addEmptyBock();
        return emptyBlock;
    }

    /**
     * Adds an empty block at end.
     */
    public Block addEmptyBock()
    {
        // Get char index of closing '}' in Class decl (really get line start for closing char)
        JFile jFile = getJFile();
        JClassDecl classDecl = jFile.getClassDecl();
        int endCharIndex = classDecl.getEnd();
        TextLine endLine = getLineForCharIndex(endCharIndex);
        endCharIndex = endLine.getStart();

        // Add block chars
        addChars("{\n}\n\n", null, endCharIndex);

        // Get new last init decl statement block
        JStmtBlock[] initBlocks = getInitDeclBlocks();
        JStmtBlock lastBlock = initBlocks[initBlocks.length - 1];

        // Create/add new block
        Block newBlock = new Block(lastBlock);
        addBlock(newBlock);
        return newBlock;
    }

    /**
     * Returns the JInitializerDecls.
     */
    private JStmtBlock[] getInitDeclBlocks()
    {
        // Get initializer decls
        JFile jFile = getJFile();
        JClassDecl classDecl = jFile.getClassDecl();
        JInitializerDecl[] initDecls = classDecl.getInitDecls();

        // Convert initializer decls array to block statement array
        Stream<JInitializerDecl> initDeclsStream = Stream.of(initDecls);
        Stream<JStmtBlock> stmtBlockStream = initDeclsStream.map(id -> id.getBlock());
        JStmtBlock[] stmtBlocks = stmtBlockStream.toArray(size -> new JStmtBlock[size]);

        // Return
        return stmtBlocks;
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
            textDidCharsChange((TextDocUtils.CharsChange) aPC);
    }

    /**
     * Called when text changes.
     */
    private void textDidCharsChange(TextDocUtils.CharsChange aCharsChange)
    {
        // Clear JFile
        _jfile = null;

        // Call didAddChars/didRemoveChars
        int charIndex = aCharsChange.getIndex();
        //CharSequence addChars = aCharsChange.getNewValue();
        //CharSequence removeChars = aCharsChange.getOldValue();
        //if (addChars != null) textDidAddChars(addChars, charIndex);
        //else textDidRemoveChars(removeChars, charIndex);

        // Reset blocks
        Block[] blocks = getBlocks();
        for (Block block : blocks) {
            if (charIndex < block.getStartCharIndex() && block._textArea != null)
                block.resetTextBoxRange();
            else break;
        }
    }

    /**
     * This class represents a block of code in the TextDoc.
     */
    public class Block {

        // The start/end line of block
        private TextLine  _startLine, _endLine;

        // The JavaTextArea
        private JavaTextArea  _textArea;

        /**
         * Constructor.
         */
        public Block(JStmtBlock blockStmt)
        {
            int startCharIndex = blockStmt.getStart();
            int endCharIndex = blockStmt.getEnd();
            _startLine = getLineForCharIndex(startCharIndex);
            _endLine = getLineForCharIndex(endCharIndex);
        }

        /**
         * Returns the start char index.
         */
        public int getStartCharIndex()  { return _startLine.getEnd(); }

        /**
         * Returns the end char index.
         */
        public int getEndCharIndex()  { return _endLine.getStart(); }

        /**
         * Returns a JavaTextArea.
         */
        public JavaTextArea getTextArea()
        {
            // If already set, just return
            if (_textArea != null) return _textArea;

            // Create/config
            JavaTextArea textArea = new JavaTextArea();
            textArea.setTextDoc(JavaTextDoc.this);
            resetTextBoxRange();

            // Set/return
            return _textArea = textArea;
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
            textBox.setStart(getStartCharIndex());
            textBox.setEndCharIndex(getEndCharIndex());
        }

        /**
         * Returns the string for block.
         */
        public String getString()
        {
            int startCharIndex = getStartCharIndex();
            int endCharIndex = getEndCharIndex();
            String str = JavaTextDoc.this.subSequence(startCharIndex, endCharIndex).toString().trim();
            return str;
        }
    }
}
