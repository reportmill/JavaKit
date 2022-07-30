/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import java.util.*;

import javakit.parse.*;
import javakit.reflect.Resolver;
import snap.parse.*;
import snap.util.ListUtils;

/**
 * A class to evaluate Java statements.
 */
public class JSEvalStmt {

    // The Expression evaluator
    private JSEvalExpr  _exprEval = JSEvalExpr.get(null);

    // A parser to parse expressions
    private static Parser  _stmtParser = new StmtParser();

    // A Resolver
    protected Resolver  _resolver;

    /**
     * Constructor.
     */
    public JSEvalStmt()
    {

    }

    /**
     * Evaluate expression.
     */
    public Object eval(Object aOR, String anExpr)
    {
        // Parse string to statement
        _stmtParser.setInput(anExpr);
        JStmt stmt = _stmtParser.parseCustom(JStmt.class);
        stmt.setResolver(_resolver);

        // Set ObjectRef and eval statement
        _exprEval._thisObj = aOR;
        Object value;
        try {
            value = evalStmt(aOR, stmt);
        }

        // Handle exceptions
        catch (Exception e) {
            return e;
        }

        // Return
        return value;
    }

    /**
     * Evaluate JStmt.
     */
    public Object evalStmt(Object anOR, JStmt aStmt) throws Exception
    {
        //if(aStmt instanceof JStmtAssert) return evalJStmtAssert((JStmtAssert)aStmt);
        //else if(aStmt instanceof JStmtBlock) return evalJStmtBlock((JStmtBlock)aStmt, false);
        //else if(aStmt instanceof JStmtBreak) return evalJStmtBreak((JStmtBreak)aStmt);
        //else if(aStmt instanceof JStmtClassDecl) return evalJStmtClassDecl((JStmtClassDecl)aStmt);
        // else if(aStmt instanceof JStmtConstrCall) return evalJStmtConstrCall((JStmtConstrCall)aStmt);

        //else if(aStmt instanceof JStmtContinue) return evalJStmtContinue((JStmtContinue)aStmt);
        //else if(aStmt instanceof JStmtDo) return evalJStmtDo((JStmtDo)aStmt);

        // Empty statement
        if(aStmt instanceof JStmtEmpty)
            return null;

        // Expression statement
        else if (aStmt instanceof JStmtExpr)
            return evalJStmtExpr((JStmtExpr) aStmt);

        // For statement
        else if (aStmt instanceof JStmtFor)
            return evalJStmtFor((JStmtFor) aStmt);

        //else if(aStmt instanceof JStmtIf) return evalJStmtIf((JStmtIf)aStmt);
        //else if(aStmt instanceof JStmtLabeled) return evalJStmtLabeled((JStmtLabeled)aStmt);
        //else if(aStmt instanceof JStmtReturn) return evalJStmtReturn((JStmtReturn)aStmt);
        //else if(aStmt instanceof JStmtSwitch) return evalJStmtSwitch((JStmtSwitch)aStmt);
        //else if(aStmt instanceof JStmtSynchronized) return evalJStmtSynchronized((JStmtSynchronized)aStmt);
        //else if(aStmt instanceof JStmtThrow) return evalJStmtThrow((JStmtThrow)aStmt);
        //else if(aStmt instanceof JStmtTry) return evalJStmtTry((JStmtTry)aStmt);

        // Variable declaration statement
        else if (aStmt instanceof JStmtVarDecl)
            return evalJStmtVarDecl((JStmtVarDecl) aStmt);

        //else if(aStmt instanceof JStmtWhile) return evalJStmtWhile((JStmtWhile)aStmt);

        else throw new RuntimeException("EvalStmt.evalStmt: Unsupported statement " + aStmt.getClass());
    }

    /**
     * Evaluate JStmtExpr.
     */
    public Object evalJStmtExpr(JStmtExpr aStmt)
    {
        JExpr expr = aStmt.getExpr();
        Object val = evalJExpr(expr);
        return val;
    }

    /**
     * Evaluate JStmtFor.
     */
    public Object evalJStmtFor(JStmtFor aStmt)
    {
        return null;
    }

    /**
     * Evaluate JStmtVarDecl.
     */
    public Object evalJStmtVarDecl(JStmtVarDecl aStmt)
    {
        List vals = new ArrayList();
        for (JVarDecl vd : aStmt.getVarDecls()) {
            JExpr iexpr = vd.getInitializer();
            if (iexpr != null) {
                Object val = evalJExpr(iexpr);
                _exprEval.setLocalVarValue(vd.getName(), val);
                vals.add(val);
            }
        }

        if (vals.size() == 1)
            return vals.get(0);
        return ListUtils.joinStrings(vals, ", ");
    }

    /**
     * Evaluate JStmtExpr.
     */
    public Object evalJExpr(JExpr anExpr)
    {
        // Evaluate expr
        Object val;
        try {
            val = _exprEval.evalExpr(anExpr);
        }

        // Handle exceptions
        catch (Exception e) {
            return e;
        }

        // Return
        return val;
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