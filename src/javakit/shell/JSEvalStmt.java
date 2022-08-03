/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import java.util.*;
import javakit.parse.*;
import snap.util.ListUtils;

/**
 * A class to evaluate Java statements.
 */
public class JSEvalStmt {

    // The Expression evaluator
    private JSEvalExpr  _exprEval = new JSEvalExpr();

    /**
     * Constructor.
     */
    public JSEvalStmt()
    {

    }

    /**
     * Evaluate JStmt.
     */
    public Object evalStmt(Object anOR, JStmt aStmt) throws Exception
    {
        // Dunno
        _exprEval._thisObj = anOR;

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
    public Object evalJStmtExpr(JStmtExpr aStmt) throws Exception
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
    public Object evalJStmtVarDecl(JStmtVarDecl aStmt) throws Exception
    {
        // Get list
        List<JVarDecl> varDecls = aStmt.getVarDecls();
        List<Object> vals = new ArrayList<>();

        // Iterate over VarDecls
        for (JVarDecl varDecl : varDecls) {

            // If initializer expression, evaluate and set local var
            JExpr initExpr = varDecl.getInitializer();
            if (initExpr != null) {
                Object val = evalJExpr(initExpr);
                _exprEval.setLocalVarValue(varDecl.getName(), val);
                vals.add(val);
            }
        }

        // If one value, just return it
        if (vals.size() == 1)
            return vals.get(0);

        // Otherwise, return joined string
        return ListUtils.joinStrings(vals, ", ");
    }

    /**
     * Evaluate JStmtExpr.
     */
    public Object evalJExpr(JExpr anExpr) throws Exception
    {
        // Evaluate expr
        Object val = _exprEval.evalExpr(anExpr);

        // Return
        return val;
    }
}