/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.text.TextDocUtils;
import snap.util.ArrayUtils;
import snap.web.WebURL;

/**
 * This JavaTextDoc subclass supports Java Repl.
 */
public class JeplTextDoc extends JavaTextDoc {

    // A JavaTextDocBuilder to build Java class boiler plate
    private JavaTextDocBuilder  _javaTextDocBuilder;

    /**
     * Constructor.
     */
    public JeplTextDoc()
    {
        super();
        _javaTextDocBuilder = new JavaTextDocBuilder();
    }

    /**
     * Returns the JavaTextDocBuilder.
     */
    public JavaTextDocBuilder getJavaTextDocBuilder()  { return _javaTextDocBuilder; }

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
        JeplTextDocUtils.findAndFixIncompleteVarDecls(jfile);
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
