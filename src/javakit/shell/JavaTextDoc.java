/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import javakit.parse.*;
import javakit.text.JavaTextUtils;
import snap.gfx.Font;
import snap.props.PropChange;
import snap.text.*;
import java.util.List;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextDoc extends TextDoc {

    // The parsed Java file
    private JFile  _jfile;

    // The parser to parse Java
    private JavaParser  _javaParser;

    /**
     * Constructor.
     */
    public JavaTextDoc()
    {
        super();

        TextStyle textStyle = getDefaultStyle();
        Font font = JavaTextUtils.getCodeFont();
        TextStyle textStyle2 = textStyle.copyFor(font);
        setDefaultStyle(textStyle2);

        TextLineStyle lineStyle = getDefaultLineStyle();
        TextLineStyle lineStyleSpaced = lineStyle.copyFor(TextLineStyle.SPACING_KEY, 4);
        setDefaultLineStyle(lineStyleSpaced);
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
     * Override to clear JFile.
     */
    @Override
    public void setString(String aString)
    {
        // Do normal version
        super.setString(aString);
        _jfile = null;
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
}
