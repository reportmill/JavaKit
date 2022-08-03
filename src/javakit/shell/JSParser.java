package javakit.shell;
import javakit.parse.*;
import javakit.reflect.Resolver;
import snap.parse.ParseHandler;
import snap.parse.ParseRule;
import snap.parse.Parser;

/**
 * This class takes a Java text string and returns an array of JStmt (one for each line).
 */
public class JSParser {

    // A parser to parse expressions
    private static JavaParser  _javaParser;

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
        _javaParser = JavaParser.getShared();
        _stmtParser = new StmtParser();
        _exprParser = _javaParser.getExprParser();

        // Create Resolver
        _resolver = Resolver.newResolverForClassLoader(getClass().getClassLoader());
    }

    /**
     * Parses given Java text string and return an array of JStmt for each line.
     */
    public JStmt[] parseJavaText(String javaText)
    {
        // Split text into lines
        String[] javaLines = javaText.split("\n");
        JStmt[] statements = new JStmt[javaLines.length];

        // Get JavaFile and body method
        JFile javaFile = getJavaFile();
        JClassDecl classDecl = javaFile.getClassDecl();
        JMethodDecl methodDecl = classDecl.getMethodDecl("body", null);
        JStmtBlock stmtBlock = methodDecl.getBlock();

        // Iterate over lines
        for (int i = 0; i < javaLines.length; i++) {

            // Get line (just skip if empty or comment)
            String javaLine = javaLines[i].trim();
            if (javaLine.length() == 0 || javaLine.startsWith("//")) continue;

            // Parse line and add to body method block
            JStmt stmt = statements[i] = parseJavaLine(javaLines[i]);
            if (stmt != null)
                stmtBlock.addStatement(stmt);
        }

        // Return
        return statements;
    }

    /**
     * Parses given Java text string and return an array of JStmt for each line.
     */
    public JStmt parseJavaLine(String javaLine)
    {
        // Parse string to statement
        try {
            _stmtParser.setInput(javaLine);
            JStmt stmt = _stmtParser.parseCustom(JStmt.class);
            return stmt;
        }

        // Otherwise, try to parse to expression and wrap in JStmtExpr
        catch (Exception e) { }

        // Otherwise, try to parse to expression and wrap in JStmtExpr
        try {
            _exprParser.setInput(javaLine);
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
     * Returns a JavaFile.
     */
    private JFile getJavaFile()
    {
        // Construct class/method wrapper for statements
        String importDecl = "import snap.view.*;\n\n";
        String classDecl = "public class JavaShellEvaluator {\n\n";
        String methodDecl = "void body() { }\n\n";
        String javaText = importDecl + classDecl + methodDecl + "}";

        // Parse JavaText to JFile
        _javaParser.setInput(javaText);
        JFile jfile = _javaParser.parseCustom(JFile.class);
        jfile.setResolver(_resolver);

        // Return
        return jfile;
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
