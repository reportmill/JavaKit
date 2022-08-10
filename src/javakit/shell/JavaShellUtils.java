/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Utility methods and support for JavaShell.
 */
public class JavaShellUtils {

    /**
     * A PrintStream to stand in for System.out and System.err.
     */
    protected static class ProxyPrintStream extends PrintStream {

        // The JavaShell
        private JavaShell  _javaShell;

        // Whether is standard err
        private boolean  _stdErr;

        /**
         * Constructor.
         */
        public ProxyPrintStream(JavaShell javaShell, PrintStream printStream)
        {
            super(printStream);
            _javaShell = javaShell;
            _stdErr = printStream == System.err;
        }

        /**
         * Creates new PGPrintStream.
         */
        public ProxyPrintStream(OutputStream aPS)
        {
            super(aPS);
        }

        /**
         * Override to send to ScanView.
         */
        public void write(int b)
        {
            // Do normal version
            super.write(b);

            // Write char to console
            JavaShell.Console console = _javaShell.getConsole();
            String str = String.valueOf(Character.valueOf((char) b));
            if (_stdErr)
                console.appendErr(str);
            else console.appendOut(str);
        }

        /**
         * Override to send to ScanView.
         */
        public void write(byte[] buf, int off, int len)
        {
            // Do normal version
            super.write(buf, off, len);

            // Write buff to console
            JavaShell.Console console = _javaShell.getConsole();
            String str = new String(buf, off, len);
            if (_stdErr)
               console.appendErr(str);
            else console.appendOut(str);
        }
    }


    /**
     * This class implements JavaShell.Console.
     */
    protected static class DefaultConsole implements JavaShell.Console {

        // Holds
        private StringBuilder _sb = new StringBuilder();

        @Override
        public void clear()  { _sb.setLength(0); }

        @Override
        public void appendOut(String aStr)  { _sb.append(aStr); }

        @Override
        public void appendErr(String aStr)  { _sb.append(aStr); }

        @Override
        public int getTextLength()  { return _sb.length(); }

        @Override
        public String getTextSubstring(int aStart, int anEnd)
        {
            return _sb.substring(aStart, anEnd);
        }
    }
}
