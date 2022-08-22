package javakit.shell;
import javakit.parse.*;
import javakit.reflect.JavaType;
import javakit.reflect.Resolver;
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
                if (isIncompleteVarDecl(stmt))
                    fixIncompleteVarDecl(stmt, stmtBlock);
            }
        }

        // Return
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
     * Returns whether expression statement is really a variable decl without type.
     */
    private boolean isIncompleteVarDecl(JStmt aStmt)
    {
        if (aStmt instanceof JStmtExpr) {
            JStmtExpr exprStmt = (JStmtExpr) aStmt;
            JExpr expr = exprStmt.getExpr();
            if (expr instanceof JExprMath && ((JExprMath) expr).getOp() == JExprMath.Op.Assign) {
                JExprMath assignExpr = (JExprMath) expr;
                JExpr assignTo = assignExpr.getOperand(0);
                if (assignTo.getDecl() == null)
                    return true;
            }
        }
        return false;
    }

    /**
     * Fixes incomplete VarDecl.
     */
    private void fixIncompleteVarDecl(JStmt aStmt, JStmtBlock stmtBlock)
    {
        // Get expr statement, assign expression and assign-to expression
        JStmtExpr exprStmt = (JStmtExpr) aStmt;
        JExprMath assignExpr = (JExprMath) exprStmt.getExpr();
        JExpr assignTo = assignExpr.getOperand(0);

        // Create VarDecl from Id and initializer
        JVarDecl varDecl = new JVarDecl();
        varDecl.setId((JExprId) assignTo);
        JExpr initializer = assignExpr.getOperand(1);
        varDecl.setInitializer(initializer);

        // Create VarDeclStatement and add VarDecl
        JStmtVarDecl varDeclStmt = new JStmtVarDecl();
        varDeclStmt.addVarDecl(varDecl);

        // Replace statements
        stmtBlock.removeStatement(aStmt);
        stmtBlock.addStatement(varDeclStmt);

        // Get initializer type
        JavaType initType = initializer.getEvalType();
        if (initType == null) {
            System.out.println("JSParser.fixIncompleteVarDecl: Failed to get init type for " + initializer.getString());
            initType = _resolver.getJavaClassForClass(Object.class);
        }

        // Create bogus type from initializer
        JType type = new JType();
        type.setName(initType.getName());
        type.setStartToken(assignTo.getStartToken());
        type.setEndToken(assignTo.getEndToken());
        type.setPrimitive(initType.isPrimitive());
        type.setParent(varDecl);
        varDecl.setType(type);
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
