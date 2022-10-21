package javakit.shell;
import javakit.parse.*;
import javakit.resolver.Resolver;
import snap.parse.ParseHandler;
import snap.parse.ParseRule;
import snap.parse.Parser;

/**
 * This class takes a Java text string and returns an array of JStmt (one for each line).
 */
public class JSParser {

    // A parser to parse expressions
    private static Parser  _stmtParser;

    // A parser to parse expressions
    private static Parser  _exprParser;

    // The Resolver
    private Resolver  _resolver;

    /**
     * Constructor.
     */
    public JSParser()
    {
        super();

        // Get parsers
        JavaParser javaParser = JavaParser.getShared();
        _stmtParser = new StmtParser();
        _exprParser = javaParser.getExprParser();

        // Create Resolver
        _resolver = Resolver.newResolverForClassLoader(getClass().getClassLoader());
    }

    /**
     * Parses given Java text string and return an array of JStmt for each line.
     */
    public JStmt[] parseJavaText(JavaText javaText)
    {
        // Get header text for java file
        String javaHeader = javaText.getHeaderText();

        // Split text into lines
        String[] javaLines = javaText.getBodyLines();
        JStmt[] statements = new JStmt[javaLines.length];

        // Get empty JFile
        JFile javaFile = javaText.getEmptyJFile();
        javaFile.setResolver(_resolver);

        // Get JavaFile and body method
        JClassDecl classDecl = javaFile.getClassDecl();
        JMethodDecl methodDecl = classDecl.getMethodDecl("body", null);
        JStmtBlock stmtBlock = methodDecl.getBlock();

        // Iterate over lines
        for (int i = 0; i < javaLines.length; i++) {

            // Append line to
            int startCharIndex = javaHeader.length();
            javaHeader += javaLines[i] + '\n';

            // Parse line and add to body method block
            JStmt stmt = statements[i] = parseJavaLine(javaHeader, startCharIndex);
            if (stmt != null) {
                stmtBlock.addStatement(stmt);

                // Fix expression statement is really an incomplete variable decl, fix it
                if (JavaShellUtils.isIncompleteVarDecl(stmt))
                    JavaShellUtils.fixIncompleteVarDecl(stmt, stmtBlock);
            }
        }

        // Return
        return statements;
    }

    /**
     * Parses given Java text string and return an array of JStmt for each line.
     */
    public JStmt[] parseJavaText(JavaTextDoc javaDoc)
    {
        // Get file and set Resolver
        JFile jfile = javaDoc.getJFile();
        jfile.setResolver(_resolver);

        // Return statements
        JStmt[] statements = javaDoc.getStatementsForJavaNode(jfile);
        return statements;
    }

    /**
     * Parses given Java text string and return an array of JStmt for each line.
     */
    public JStmt parseJavaLine(String javaLine, int startCharIndex)
    {
        // Parse string to statement
        try {
            _stmtParser.setInput(javaLine);
            _stmtParser.setCharIndex(startCharIndex);
            JStmt stmt = _stmtParser.parseCustom(JStmt.class);
            return stmt;
        }

        // Otherwise, try to parse to expression and wrap in JStmtExpr
        catch (Exception e) { }

        // Otherwise, try to parse to expression and wrap in JStmtExpr
        try {
            _exprParser.setInput(javaLine);
            _stmtParser.setCharIndex(startCharIndex);
            JExpr expr = _exprParser.parseCustom(JExpr.class);
            JStmtExpr exprStmt = new JStmtExpr();
            exprStmt.setExpr(expr);
            return exprStmt;
        }

        // Otherwise, try to parse to expression and wrap in JStmtExpr
        catch (Exception e) {
            System.out.println("JSParser: Parse failed: " + e);
            return null;
        }
    }

    /**
     * A Java Statement parser.
     */
    protected static class StmtParser extends Parser {

        /**
         * Creates a new StmtParser.
         */
        public StmtParser()
        {
            super(JavaParser.getShared().getRule("BlockStatement"));
        }

        /**
         * Override to ignore exception.
         */
        protected void parseFailed(ParseRule aRule, ParseHandler aHandler)
        {
            if (aRule.getPattern() != ";")
                super.parseFailed(aRule, aHandler);
        }
    }
}
