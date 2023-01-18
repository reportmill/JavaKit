/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.project.JeplAgent;
import javakit.project.ProjectUtils;
import snap.util.ArrayUtils;
import snap.web.WebFile;
import snap.web.WebURL;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * This JavaTextDoc subclass supports Java Repl.
 */
public class JeplTextDoc extends JavaTextDoc {

    // The array of imports
    private String[]  _imports = DEFAULT_IMPORTS;

    // The super class name
    private String  _superClassName = "Object";

    // A configure
    private static Consumer<JeplTextDoc>  _jeplDocConfig;

    // Constants for imports
    private static final String IMPORT1 = "java.util.*";
    private static final String IMPORT2 = "java.util.stream.*";
    private static final String IMPORT3 = "snap.view.*";
    private static final String[] DEFAULT_IMPORTS = { IMPORT1, IMPORT2, IMPORT3 };

    /**
     * Constructor.
     */
    public JeplTextDoc()
    {
        super();

        // If config set, do configure
        if (_jeplDocConfig != null)
            _jeplDocConfig.accept(this);
    }

    /**
     * Returns the imports.
     */
    public String[] getImports()  { return _imports; }

    /**
     * Adds an import.
     */
    public void addImport(String anImportStr)
    {
        _imports = ArrayUtils.add(_imports, anImportStr);
        Arrays.sort(_imports);
    }

    /**
     * Returns the base class name.
     */
    public String getSuperClassName()  { return _superClassName; }

    /**
     * Sets the base class name.
     */
    public void setSuperClassName(String aName)
    {
        _superClassName = aName;
    }

    /**
     * Sets a configure function.
     */
    public static void setJeplDocConfig(Consumer<JeplTextDoc> aConfig)
    {
        _jeplDocConfig = aConfig;
    }

    /**
     * Returns the JeplTextDoc for given source.
     */
    public static JeplTextDoc getJeplTextDocForSourceURL(Object aSource)
    {
        // If Source is null, create temp file
        Object source = aSource;
        if (source == null) {
            WebFile tempFile = ProjectUtils.getTempSourceFile(null, "jepl");
            source = tempFile.getURL();
        }

        // Get Source URL and file
        WebURL url = WebURL.getURL(source);
        WebFile sourceFile = ProjectUtils.getProjectSourceFileForURL(url);

        // Get java agent and TextDoc
        JeplAgent javaAgent = JeplAgent.getAgentForFile(sourceFile);
        JeplTextDoc jeplTextDoc = javaAgent.getJavaTextDoc();

        // Return
        return jeplTextDoc;
    }
}
