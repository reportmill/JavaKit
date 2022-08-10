/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import javakit.parse.JStmt;
import java.io.PrintStream;

/**
 * A class to evaluate JavaShell code.
 */
public class JavaShell {

    // The JavaShell parser
    private JSParser  _javaParser;

    // A Statement evaluator
    private JSStmtEval _stmtEval;

    // An object to act as "this"
    private Object  _thisObject = new Object();

    // The console
    private Console  _console;

    // The values
    protected Object[]  _lineVals;

    // The public out and err PrintStreams
    private PrintStream  _stdOut = System.out;
    private PrintStream  _stdErr = System.err;

    // Proxy standard out/err to capture console
    private PrintStream  _shellOut = new JavaShellUtils.ProxyPrintStream(this, _stdOut);
    private PrintStream  _shellErr = new JavaShellUtils.ProxyPrintStream(this,_stdErr);

    /**
     * Creates a new PGEvaluator.
     */
    public JavaShell()
    {
        // Create JSParser
        _javaParser = new JSParser();

        // Create Statement eval
        _stmtEval = new JSStmtEval();

        // Create default console
        _console = new JavaShellUtils.DefaultConsole();
    }

    /**
     * Returns the console.
     */
    public Console getConsole()  { return _console; }

    /**
     * Sets the console.
     */
    public void setConsole(Console aConsole)
    {
        _console = aConsole;
    }

    /**
     * Evaluate string.
     */
    public void runJavaCode(String aStr)
    {
        // Parse Java text to statements
        JStmt[] javaStmts = _javaParser.parseJavaText(aStr);

        // Set System out/err to catch console output
        System.setOut(_shellOut);
        System.setErr(_shellErr);

        // Clear console
        _console.clear();

        // Get line vals for statements
        _lineVals = new Object[javaStmts.length];

        // Iterate over lines and eval each
        for (int i = 0, iMax = javaStmts.length; i < iMax; i++) {

            // Get Statement (if null, just set empty string value and continue)
            JStmt stmt = javaStmts[i];
            if (stmt == null) {
                _lineVals[i] = ""; continue; }

            // Evaluate statement
            _lineVals[i] = evalStatement(stmt);
        }

        // Restore System out/err
        System.setOut(_stdOut);
        System.setErr(_stdErr);
    }

    /**
     * Evaluate JStmt.
     */
    protected Object evalStatement(JStmt aStmt)
    {
        // Get textview and mark current length, in case we need to check for console output
        int start = _console.getTextLength();

        // Eval statement
        Object val;
        try {
            val = _stmtEval.evalStmt(_thisObject, aStmt);
        }

        // Handle statement eval exception: Try expression
        catch (Exception e) {
            e.printStackTrace();
            val = e;
        }

        // If val is null, see if there was any console output
        if (val == null) {
            int end = _console.getTextLength();
            if (start != end)
                val = '"' + _console.getTextSubstring(start, end).trim() + '"';
        }

        // Return
        return val;
    }

    /**
     * Returns the resulting line values from last execution.
     */
    public Object[] getLineValues()  { return _lineVals; }

    /**
     * Sets the base repl class name.
     */
    public void setREPLClassName(String aName)
    {
        _javaParser.setREPLClassName(aName);
    }

    /**
     * Adds an import.
     */
    public void addImport(String anImportStr)
    {
        _javaParser.addImport(anImportStr);
    }

    /**
     * An interface to capture console output.
     */
    public interface Console {

        /**
         * Clears the RunConsole text.
         */
        void clear();

        /**
         * Appends to out.
         */
        void appendOut(String aStr);

        /**
         * Appends to err.
         */
        void appendErr(String aStr);

        /**
         * Returns the text length at any given point.
         */
        int getTextLength();

        /**
         * Returns a substring of appended text.
         */
        String getTextSubstring(int aStart, int anEnd);
    }
}