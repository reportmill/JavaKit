/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
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
     * Override to get statements from initializers.
     */
    @Override
    public JStmt[] getJFileStatements()
    {
        JFile jfile = getJFile();
        JClassDecl classDecl = jfile.getClassDecl();
        if (classDecl == null)
            return new JStmt[0];
        JInitializerDecl[] initDecls = classDecl.getInitDecls();
        JStmt[] stmtsAll = new JStmt[0];

        for (JInitializerDecl initDecl : initDecls) {
            JStmt[] stmts = JavaTextDocUtils.getStatementsForJavaNode(initDecl);
            stmtsAll = ArrayUtils.addAll(stmtsAll, stmts);

        }

        return stmtsAll;
    }

    /**
     * Returns a new JeplTextDoc from given source.
     */
    public static JeplTextDoc newFromSourceURL(WebURL aURL)
    {
        // Create TextDoc
        JeplTextDoc jeplTextDoc = new JeplTextDoc();
        jeplTextDoc.setSource(aURL);

        // Return
        return jeplTextDoc;
    }
}
