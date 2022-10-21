/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import snap.util.ArrayUtils;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextDocBuilder {

    // The array of imports
    private String[]  _imports = DEFAULT_IMPORTS;

    // The super class name
    private String  _superClassName = "Object";

    // The body text
    private String  _bodyText = "";

    // Constants for imports
    private static final String IMPORT1 = "java.util.*";
    private static final String IMPORT2 = "java.util.stream.*";
    private static final String IMPORT3 = "snap.view.*";
    private static final String[] DEFAULT_IMPORTS = { IMPORT1, IMPORT2, IMPORT3 };

    /**
     * Constructor.
     */
    public JavaTextDocBuilder()
    {
        super();
    }

    /**
     * Returns the text of the file.
     */
    public String getText()
    {
        // Get header text
        String headerText = getHeaderText();

        // Get body text
        String bodyText = getBodyText();
        String bodyTextAll = "void body() {\n\n" + bodyText;

        // Get tail
        String tailText = "\n}\n}";

        // Return
        return headerText + bodyTextAll + tailText;
    }

    /**
     * Adds an import.
     */
    public void addImport(String anImportStr)
    {
        _imports = ArrayUtils.add(_imports, anImportStr);
    }

    /**
     * Sets the base class name.
     */
    public void setSuperClassName(String aName)
    {
        _superClassName = aName;
    }

    /**
     * Returns the Java file header text.
     */
    public String getHeaderText()
    {
        // Get import declarations string
        String importsDecl = "";
        for (String importDecl : _imports)
            importsDecl += "import " + importDecl + ";\n";
        importsDecl += '\n';

        // Get class decl string
        String classDecl = "public class JavaShellREPL extends " + _superClassName + " {\n\n";

        // Return
        return importsDecl + classDecl;
    }

    /**
     * Returns the body.
     */
    public String getBodyText()  { return _bodyText; }

    /**
     * Sets the body.
     */
    public void setBodyText(String bodyText)
    {
        _bodyText = bodyText;
    }

    /**
     * Creates a JavaTextDoc.
     */
    public JavaTextDoc createJavaTextDoc()
    {
        // Create JavaTextDoc
        JavaTextDoc javaTextDoc = new JavaTextDoc();

        // Get/set text
        String javaTextStr = getText();
        javaTextDoc.setString(javaTextStr);

        // Return
        return javaTextDoc;
    }
}
