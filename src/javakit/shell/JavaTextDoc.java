/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import javakit.parse.*;
import snap.gfx.Font;
import snap.props.PropChange;
import snap.text.TextDoc;
import snap.text.TextDocUtils;
import snap.text.TextLine;
import snap.text.TextStyle;
import snap.util.ArrayUtils;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextDoc extends TextDoc {

    // The parsed Java file
    private JFile  _jfile;

    // The parser to parse Java
    private JavaParser  _javaParser;

    // The blocks
    private JavaTextDocBlock[]  _blocks = new JavaTextDocBlock[0];

    /**
     * Constructor.
     */
    public JavaTextDoc()
    {
        super();

        TextStyle textStyle = getDefaultStyle();
        Font font = getDefaultFont();
        TextStyle textStyle2 = textStyle.copyFor(font);
        setDefaultStyle(textStyle2);
    }

    /**
     * Returns the JFile (parsed Java file).
     */
    public JFile getJFile()
    {
        // If already set, just return
        if (_jfile != null) return _jfile;

        // Get parsed java file
        JavaParser javaParser = getJavaParser();
        String javaStr = getString();
        JFile jfile = javaParser.getJavaFile(javaStr);

        // This sucks
        getStatementsForJavaNode(jfile);

        // Set, return
        return _jfile = jfile;
    }

    /**
     * Returns the parser to parse java file.
     */
    public JavaParser getJavaParser()
    {
        // If already set, just return
        if (_javaParser != null) return _javaParser;

        // Create, set, return
        JavaParser javaParser = JavaParser.getShared();
        return _javaParser = javaParser;
    }

    /**
     * Sets the parser to parse java file.
     */
    public void setJavaParser(JavaParser aJavaParser)  { _javaParser = aJavaParser; }

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
            JavaTextDocBlock block = new JavaTextDocBlock(this, blockStmt);
            addBlock(block);
        }
    }

    /**
     * Returns the blocks.
     */
    public JavaTextDocBlock[] getBlocks()  { return _blocks; }

    /**
     * Returns the number of blocks.
     */
    public int getBlockCount()  { return _blocks.length; }

    /**
     * Returns the individual block at given index.
     */
    public JavaTextDocBlock getBlock(int anIndex)  { return _blocks[anIndex]; }

    /**
     * Adds a block at end of blocks.
     */
    public void addBlock(JavaTextDocBlock aBlock)
    {
        addBlock(aBlock, getBlockCount());
    }

    /**
     * Adds a block at given index.
     */
    public void addBlock(JavaTextDocBlock aBlock, int anIndex)
    {
        _blocks = ArrayUtils.add(_blocks, aBlock, anIndex);
    }

    /**
     * Removes the block at given index.
     */
    public JavaTextDocBlock removeBlock(int anIndex)
    {
        // Remove from array
        JavaTextDocBlock block = getBlock(anIndex);
        _blocks = ArrayUtils.remove(_blocks, anIndex);

        // Delete text
        int startCharIndex = block.getStartCharIndex() - 2;
        int endCharIndex = block.getEndCharIndex() + 2;
        removeChars(startCharIndex, endCharIndex);

        // Dispose block so Block.SubText can disconnect from parent TextDoc
        block.dispose();

        // Return
        return block;
    }

    /**
     * Removes the given block.
     */
    public void removeBlock(JavaTextDocBlock aBlock)
    {
        int index = ArrayUtils.indexOfId(_blocks, aBlock);
        if (index >= 0)
            removeBlock(index);
    }

    /**
     * Returns the last block.
     */
    public JavaTextDocBlock getBlockLast()
    {
        int blockCount = getBlockCount();
        return blockCount > 0 ? getBlock(blockCount - 1) : null;
    }

    /**
     * Returns the empty block at end.
     */
    public JavaTextDocBlock getEmptyBlock()
    {
        // Iterate over blocks and return empty one
        JavaTextDocBlock lastBlock = getBlockLast();
        if (lastBlock != null && lastBlock.getString().length() == 0)
            return lastBlock;

        // Otherwise add empty and return
        JavaTextDocBlock emptyBlock = addEmptyBlock();
        return emptyBlock;
    }

    /**
     * Adds an empty block at end.
     */
    public JavaTextDocBlock addEmptyBlock()
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
        JavaTextDocBlock newBlock = new JavaTextDocBlock(this, lastBlock);
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
     * Returns an array of statements for given JFile.
     */
    public JStmt[] getStatementsForJavaNode(JNode aJNode)
    {
        // Get last line for node
        int childCount = aJNode.getChildCount();
        if (childCount == 0) // Can happen if Parse totally fails (e.g., tokenizer fails)
            return new JStmt[0];
        JNode lastChild = aJNode.getChild(childCount - 1);
        int lastLineIndex = lastChild.getEndToken().getLineIndex();

        // Create statement array and load
        JStmt[] stmtArray = new JStmt[lastLineIndex + 2];
        getStatementsForJavaNode(aJNode, stmtArray);

        // Iterate over statement array and if partial VarDecl if needed
        for (JStmt stmt : stmtArray) {
            if (JavaShellUtils.isIncompleteVarDecl(stmt)) {
                JStmtBlock blockStmt = stmt.getParent(JStmtBlock.class);
                JavaShellUtils.fixIncompleteVarDecl(stmt, blockStmt);
            }
        }

        // Return
        return stmtArray;
    }

    /**
     * Returns an array of statements for given JFile.
     */
    private void getStatementsForJavaNode(JNode aJNode, JStmt[] stmtArray)
    {
        // Handle statement node (but not block), get line index and set in array
        if (aJNode instanceof JStmt && !(aJNode instanceof JStmtBlock)) {
            JStmt stmt = (JStmt) aJNode;
            int lineIndex = stmt.getLineIndex();
            stmtArray[lineIndex] = (JStmt) aJNode;
            return;
        }

        // Handle any node: Iterate over children and recurse
        List<JNode> children = aJNode.getChildren();
        for (JNode child : children)
            getStatementsForJavaNode(child, stmtArray);
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
    }

    /**
     * Copies this JavaTextDoc for REPL. Converts simple initializer blocks/entries to static decls.
     */
    public JavaTextDoc copyForREPL()
    {
        // Do normal copy
        JavaTextDoc copy = (JavaTextDoc) clone();

        // Get initializer blocks
        JStmtBlock[] initDecls = copy.getInitDeclBlocks();

        // Convert initializer blocks
        for (int i = initDecls.length - 1; i >= 0; i--) {
            JStmtBlock initBlock = initDecls[i];
            copy.convertInitBlock(initBlock, i == 0, i == initDecls.length - 1);
        }

        // Return
        return copy;
    }

    /**
     * Converts an initializer block.
     */
    private void convertInitBlock(JStmtBlock initBlock, boolean isFirst, boolean isLast)
    {
        // Replace open bracket with space
        if (!isFirst) {
            int startCharIndex = initBlock.getStart();
            replaceChars(" ", startCharIndex, startCharIndex + 1);
        }

        // Replace close bracket with space
        if (!isLast) {
            int endCharIndex = initBlock.getEnd();
            replaceChars(" ", endCharIndex - 1, endCharIndex);
        }
    }

    /**
     * Returns the default font.
     */
    private static Font getDefaultFont()
    {
        // Get
        Font defaultFont = Font.Arial10;
        String[] names = { "Monaco", "Consolas", "Courier" };
        for (String name : names) {
            defaultFont = new Font(name, 12);
            if (defaultFont.getFamily().startsWith(name))
                break;
        }

        // Return
        return defaultFont;
    }
}
