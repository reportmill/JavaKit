/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.runner.JavaShell;
import snap.props.PropObject;
import snap.props.PropSet;
import snap.text.SubText;
import snap.text.TextLine;
import snap.util.SnapUtils;
import snap.web.WebFile;
import snap.web.WebURL;
import java.util.Arrays;

/**
 * This class manages collections of snippets.
 */
public class JavaReplDoc extends PropObject {

    // The JavaTextDoc
    private JavaTextDoc  _javaDoc;

    // The JavaTextDoc
    private SubText  _replDoc;

    // The name
    private String  _name;

    // Whether notebook needs update
    private boolean  _needsUpdate;

    // The JavaShell
    protected JavaShell _javaShell;

    // The Repl values by line
    private Object[]  _lineValues;

    // The Repl values by line
    private Object[]  _replValues;

    // Constants for properties
    public static final String NeedsUpdate_Prop = "NeedsUpdate";

    /**
     * Constructor.
     */
    public JavaReplDoc()
    {
        super();

        // Create JavaShell
        _javaShell = new JavaShell();
    }

    /**
     * Returns the source url.
     */
    public WebURL getSourceURL()  { return null; }

    /**
     * Sets the source url.
     */
    public void setSourceURL(WebURL aURL)  { }

    /**
     * Returns the JavaDoc.
     */
    public JavaTextDoc getJavaDoc()  { return _javaDoc; }

    /**
     * Sets the JavaDoc.
     */
    public void setJavaDoc(JavaTextDoc aJavaDoc)
    {
        // If already set, just return
        if (aJavaDoc == getJavaDoc()) return;

        // Set JavaDoc
        _javaDoc = aJavaDoc;
        _replDoc = null;
    }

    /**
     * Returns the ReplDoc.
     */
    public SubText getReplDoc()
    {
        // If already set, just return
        if (_replDoc != null) return _replDoc;

        // Create SubText
        JFile jfile = _javaDoc.getJFile();
        JClassDecl classDecl = jfile.getClassDecl();
        JMethodDecl bodyMethod = classDecl.getMethodDecl("body", null);
        JStmtBlock blockStmt = bodyMethod.getBlock();

        // Set start/end char indexes for code block ( " { ... } ")
        int blockStart = blockStmt.getStartCharIndex();
        int blockEnd = blockStmt.getEndCharIndex();

        // Get start/end lines indexes
        TextLine startLine = _javaDoc.getLineForCharIndex(blockStart);
        TextLine endLine = _javaDoc.getLineForCharIndex(blockEnd);

        // Get code start/end char index (inside of brackets)
        int codeStartIndex = startLine.getEndCharIndex();
        int codeEndIndex = endLine.getStartCharIndex();

        // Create ReplDoc
        return _replDoc = new SubText(_javaDoc, codeStartIndex, codeEndIndex);
    }

    /**
     * Returns the name.
     */
    public String getName()  { return _name; }

    /**
     * Sets the name.
     */
    public void setName(String aName)
    {
        _name = aName;
    }

    /**
     * Returns whether notebook needs update.
     */
    public boolean isNeedsUpdate()  { return _needsUpdate; }

    /**
     * Sets whether notebook needs update.
     */
    protected void setNeedsUpdate(boolean aValue)
    {
        if (aValue == isNeedsUpdate()) return;
        firePropChange(NeedsUpdate_Prop, _needsUpdate, _needsUpdate = aValue);
    }

    /**
     * Updates the notebook.
     */
    public void updateDocValues()
    {
        // Run JavaCode
        JavaTextDoc javaDoc = getJavaDoc();
        _javaShell.runJavaCode(javaDoc);

        // Get line values
        _lineValues = _javaShell.getLineValues();

        int replStartCharIndex = _replDoc.getStartCharIndex();
        int replStartLineIndex = _javaDoc.getLineForCharIndex(replStartCharIndex).getIndex();
        _replValues = Arrays.copyOfRange(_lineValues, replStartLineIndex, _lineValues.length);

        // Reset NeedsUpdate
        setNeedsUpdate(false);
    }

    /**
     * Returns all Repl values.
     */
    public Object[] getReplValues()  { return _replValues; }

    /**
     * Returns the Repl value for line.
     */
    public Object getReplValueForLineIndex(int lineIndex)
    {
        int replStartCharIndex = _replDoc.getStartCharIndex();
        int replStartLineIndex = _javaDoc.getLineForCharIndex(replStartCharIndex).getIndex();
        int replLineIndex = replStartLineIndex + lineIndex;
        Object replValue = replLineIndex >= 0 && replLineIndex < _lineValues.length ? _lineValues[replLineIndex] : null;
        return replValue;
    }

    /**
     * Override to register props.
     */
    @Override
    protected void initProps(PropSet aPropSet)
    {
        aPropSet.addPropNamed(NeedsUpdate_Prop, boolean.class, false);
    }

    /**
     * Returns the prop value for given key.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        // Handle properties
        switch (aPropName) {

            // NeedsUpdate
            case NeedsUpdate_Prop: return isNeedsUpdate();

            // Handle super class properties (or unknown)
            default: return super.getPropValue(aPropName);
        }
    }

    /**
     * Sets the prop value for given key.
     */
    @Override
    public void setPropValue(String aPropName, Object aValue)
    {
        // Handle properties
        switch (aPropName) {

            // NeedsUpdate
            case NeedsUpdate_Prop: setNeedsUpdate(SnapUtils.boolValue(aValue));

            // Handle super class properties (or unknown)
            default: super.setPropValue(aPropName, aValue);
        }
    }

    /**
     * The real save method.
     */
    public void saveToURL()
    {
        WebURL url = getSourceURL();
        WebFile file = url.getFile();
        if (file == null)
            file = url.createFile(false);

        byte[] bytes = null;
        file.setBytes(bytes);
        file.save();
    }

    /**
     * Creates a new Notebook from Source URL.
     */
    public static JavaReplDoc createFromSourceURL(WebURL aURL)
    {
        return null;
    }
}
