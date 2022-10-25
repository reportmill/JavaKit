/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.props.PropObject;
import snap.props.PropSet;
import snap.text.SubText;
import snap.text.TextLine;
import snap.web.WebFile;
import snap.web.WebURL;
import java.util.Objects;

/**
 * This class manages collections of snippets.
 */
public class JavaReplDoc extends PropObject {

    // The name
    private String  _name;

    // The url for contents of doc
    private WebURL  _sourceURL;

    // The JavaTextDoc
    private JavaTextDoc  _javaDoc;

    // The JavaTextDoc
    private SubText  _replDoc;

    // Constants for properties
    public static final String SourceURL_Prop = "SourceURL";

    /**
     * Constructor.
     */
    public JavaReplDoc()
    {
        super();
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
     * Returns the source url.
     */
    public WebURL getSourceURL()  { return null; }

    /**
     * Sets the source url.
     */
    public void setSourceURL(WebURL aURL)
    {
        if (Objects.equals(aURL, getSourceURL())) return;
        firePropChange(SourceURL_Prop, _sourceURL, _sourceURL = aURL);
    }

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
     * Override to register props.
     */
    @Override
    protected void initProps(PropSet aPropSet)
    {
        aPropSet.addPropNamed(SourceURL_Prop, WebURL.class, null);
    }

    /**
     * Returns the prop value for given key.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        // Handle properties
        switch (aPropName) {

            // SourceURL
            case SourceURL_Prop: return getSourceURL();

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

            // SourceURL
            case SourceURL_Prop: setSourceURL((WebURL) aValue);

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
