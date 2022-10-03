/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import javakit.parse.*;
import javakit.reflect.JavaType;
import javakit.reflect.Resolver;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Utility methods and support for JavaShell.
 */
public class JavaShellUtils {

    /**
     * Returns a String for given array object.
     */
    public static String getStringForArray(Object anObj)
    {
        if (anObj instanceof Object[])
            return Arrays.toString((Object[]) anObj);
        if (anObj instanceof double[])
            return Arrays.toString((double[]) anObj);
        if (anObj instanceof float[])
            return Arrays.toString((float[]) anObj);
        if (anObj instanceof int[])
            return Arrays.toString((int[]) anObj);

        int len = Array.getLength(anObj);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++)
            array[i] = Array.get(anObj, i);
        return Arrays.toString(array);
    }

    /**
     * Returns whether expression statement is really a variable decl without type.
     */
    public static boolean isIncompleteVarDecl(JStmt aStmt)
    {
        // If expression statement, check for assignment
        if (aStmt instanceof JStmtExpr) {

            // Get expression
            JStmtExpr exprStmt = (JStmtExpr) aStmt;
            JExpr expr = exprStmt.getExpr();

            // If assignment, check for undefined 'AssignTo' type
            if (expr instanceof JExprMath && ((JExprMath) expr).getOp() == JExprMath.Op.Assign) {
                JExprMath assignExpr = (JExprMath) expr;
                JExpr assignTo = assignExpr.getOperand(0);
                if (assignTo.getDecl() == null && assignExpr.getOperandCount() > 1)
                    return true;
            }
        }

        // Return
        return false;
    }

    /**
     * Fixes incomplete VarDecl.
     */
    public static void fixIncompleteVarDecl(JStmt aStmt, JStmtBlock stmtBlock)
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

        // Swap VarDecl statement in for expr statement
        int index = stmtBlock.removeStatement(aStmt);
        stmtBlock.addStatement(varDeclStmt, index);

        // Get initializer type
        JavaType initType = initializer.getEvalType();
        if (initType == null) {
            System.out.println("JSParser.fixIncompleteVarDecl: Failed to get init type for " + initializer.getString());
            Resolver resolver = aStmt.getResolver();
            initType = resolver.getJavaClassForClass(Object.class);
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
