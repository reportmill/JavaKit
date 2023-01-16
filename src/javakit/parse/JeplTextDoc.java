/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.text.TextDocUtils;
import snap.util.ArrayUtils;
import snap.web.WebURL;

import java.util.Arrays;

/**
 * This JavaTextDoc subclass supports Java Repl.
 */
public class JeplTextDoc extends JavaTextDoc {

    // The array of imports
    private String[]  _imports = DEFAULT_IMPORTS;

    // The super class name
    private String  _superClassName = "Object";

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
     * Returns the parser to parse java file.
     */
    protected JavaParser getJavaParserImpl()  { return new JeplParser(this); }

    /**
     * Override to fix incomplete var decls.
     */
    @Override
    protected JFile createJFile()
    {
        JFile jfile = super.createJFile();
        JeplParser.findAndFixIncompleteVarDecls(jfile);
        return jfile;
    }

    /**
     * Override to get statements from initializers.
     */
    @Override
    public JStmt[] getJFileStatements()
    {
        // Get JFile, ClassDecl (just return if not found)
        JFile jfile = getJFile();
        JClassDecl classDecl = jfile.getClassDecl();
        if (classDecl == null)
            return new JStmt[0];

        // Get initializers
        JInitializerDecl[] initDecls = classDecl.getInitDecls();
        JStmt[] stmtsAll = new JStmt[0];

        // Iterate over initializers and add statements
        for (JInitializerDecl initDecl : initDecls) {
            JStmt[] stmts = JavaTextDocUtils.getStatementsForJavaNode(initDecl);
            stmtsAll = ArrayUtils.addAll(stmtsAll, stmts);
        }

        // Return
        return stmtsAll;
    }

    /**
     * Override to just do full re-parse.
     */
    @Override
    protected void updateJFileForChange(TextDocUtils.CharsChange charsChange)
    {
        _jfile = null;
    }

    /**
     * Returns a new JeplTextDoc from given source.
     */
    public static JeplTextDoc newFromSourceURL(WebURL aURL)
    {
        // Create TextDoc and read from URL
        JeplTextDoc jeplTextDoc = new JeplTextDoc();
        jeplTextDoc.readFromSourceURL(aURL);

        // Return
        return jeplTextDoc;
    }
}
