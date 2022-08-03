/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import javakit.parse.JStmt;
import javakit.reflect.Resolver;
import snap.view.TextArea;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A class to evaluate JavaShell code.
 */
public class JSEvaluator {

    // The JavaShell
    private JavaShell  _javaShell;

    // The JavaShell parser
    private JSParser  _javaParser;

    // A Statement evaluator
    private JSEvalStmt  _stmtEval;

    // The values
    protected Object[]  _lineVals;

    // The public out and err PrintStreams
    private PrintStream  _stdOut = System.out;
    private PrintStream  _stdErr = System.err;

    // Proxy standard out/err to capture console
    private PrintStream  _shellOut = new PGPrintStream(_stdOut);
    private PrintStream  _shellErr = new PGPrintStream(_stdErr);

    /**
     * Creates a new PGEvaluator.
     */
    public JSEvaluator(JavaShell aPG)
    {
        _javaShell = aPG;

        // Create JSParser
        _javaParser = new JSParser();

        // Create Statement eval
        _stmtEval = new JSEvalStmt();
    }

    /**
     * Evaluate string.
     */
    public void eval(String aStr)
    {
        // Parse Java text to statements
        JStmt[] javaStmts = _javaParser.parseJavaText(aStr);

        // Set sys out/err to catch console output
        System.setOut(_shellOut);
        System.setErr(_shellErr);

        // Clear console
        _javaShell.getConsole().clear();

        // Get line vals for statements
        _lineVals = new Object[javaStmts.length];

        // Iterate over lines and eval each
        for (int i = 0, iMax = javaStmts.length; i < iMax; i++) {
            JStmt stmt = javaStmts[i];
            if (stmt != null)
                _lineVals[i] = evalStatement(stmt);
        }

        // Restore sys out/err
        System.setOut(_stdOut);
        System.setErr(_stdErr);
    }

    /**
     * Evaluate JStmt.
     */
    protected Object evalStatement(JStmt aStmt)
    {
        // Get textview and mark current length, in case we need to check for console output
        TextArea tview = _javaShell.getConsole().getConsoleView();
        int start = tview.length();

        // Eval statement
        Object val;
        try {
            val = _stmtEval.evalStmt(_javaShell, aStmt);
        }

        // Handle statement eval exception: Try expression
        catch (Exception e) {
            val = e;
        }

        // If val is null, see if there was any console output
        if (val == null) {
            int end = tview.length();
            if (start != end)
                val = '"' + tview.getText().substring(start, end).trim() + '"';
        }

        // Return
        return val;
    }

    /**
     * A PrintStream to stand in for System.out and System.err.
     */
    private class PGPrintStream extends PrintStream {

        /**
         * Creates new PGPrintStream.
         */
        public PGPrintStream(OutputStream aPS)
        {
            super(aPS);
        }

        /**
         * Override to send to ScanView.
         */
        public void write(int b)
        {
            super.write(b);
            String str = String.valueOf(Character.valueOf((char) b));
            if (this == _shellOut)
                _javaShell.getConsole().appendOut(str);
            else _javaShell.getConsole().appendErr(str);
        }

        /**
         * Override to send to ScanView.
         */
        public void write(byte[] buf, int off, int len)
        {
            super.write(buf, off, len);
            String str = new String(buf, off, len);
            if (this == _shellOut)
                _javaShell.getConsole().appendOut(str);
            else _javaShell.getConsole().appendErr(str);
        }
    }
}