/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
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

    // A Statement evaluator
    private JSEvalStmt  _stmtEval;

    // An expression evaluator
    private JSEvalExpr  _exprEval;

    // The lines
    private String[]  _lines;

    // The values
    protected Object[]  _lineVals;

    // The Resolver
    private Resolver  _resolver;

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

        // Create Resolver
        _resolver = Resolver.newResolverForClassLoader(getClass().getClassLoader());

        // Create Statement eval
        _stmtEval = new JSEvalStmt();
        _stmtEval._resolver = _resolver;
        _exprEval = JSEvalExpr.getExprEvaluatorForThisObject(_javaShell);
        _exprEval._resolver = _resolver;
    }

    /**
     * Evaluate string.
     */
    public void eval(String aStr)
    {
        // Set sys out/err to catch console output
        System.setOut(_shellOut);
        System.setErr(_shellErr);

        // Clear console
        _javaShell.getConsole().clear();

        _lines = aStr.split("\n");
        _lineVals = new Object[_lines.length];

        // Iterate over lines and eval each
        for (int i = 0, iMax = _lines.length; i < iMax; i++) {
            String line = _lines[i];
            _lineVals[i] = evalLine(line);
        }

        // Restore sys out/err
        System.setOut(_stdOut);
        System.setErr(_stdErr);
    }

    /**
     * Evaluate string.
     */
    protected Object evalLine(String aLine)
    {
        // Get trimmed line (just return if empty or comment)
        String line = aLine.trim();
        if (line.length() == 0 || line.startsWith("//")) return null;

        // Get textview and mark current length, in case we need to check for console output
        TextArea tview = _javaShell.getConsole().getConsoleView();
        int start = tview.length();

        // Eval line as statement
        Object val = null;
        try {
            val = _stmtEval.eval(_javaShell, line);
        }

        // Handle statement eval exception: Try expression
        catch (Exception e) {

            // Try Expression evaluator
            try {
                val = _exprEval.eval(line);
            }
            catch (Exception e2) { }
        }

        // If val is null, see if there was any console output
        if (val == null) {
            int end = tview.length();
            if (start != end)
                val = '"' + tview.getText().substring(start, end).trim() + '"';
        }

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