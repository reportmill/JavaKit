/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import java.util.*;
import javakit.parse.*;
import snap.util.ListUtils;
import snap.util.SnapUtils;

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

        // Handle block statement
        if(aStmt instanceof JStmtBlock)
            return evalBlockStmt(anOR, (JStmtBlock)aStmt);

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
            return evalExprStmt((JStmtExpr) aStmt);

        // For statement
        else if (aStmt instanceof JStmtFor)
            return evalForStmt(anOR, (JStmtFor) aStmt);

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
     * Evaluate JStmtBlock.
     */
    public Object evalBlockStmt(Object anOR, JStmtBlock aBlockStmt) throws Exception
    {
        List<JStmt> statements = aBlockStmt.getStatements();
        for (JStmt stmt : statements)
            evalStmt(anOR, stmt);
        return null;
    }

    /**
     * Evaluate JStmtExpr.
     */
    public Object evalExprStmt(JStmtExpr aStmt) throws Exception
    {
        JExpr expr = aStmt.getExpr();
        Object val = evalExpr(expr);
        return val;
    }

    /**
     * Evaluate JStmtFor.
     */
    public Object evalForStmt(Object anOR, JStmtFor aForStmt) throws Exception
    {
        // Handle ForEach
        if (aForStmt.isForEach())
            return evalForEachStmt(anOR, aForStmt);

        // Get block statement
        JStmtBlock blockStmt = aForStmt.getBlock();

        // Get main var name statement
        JStmtVarDecl initDeclStmt = aForStmt.getInitDecl();
        evalStmt(anOR, initDeclStmt);

        // Get conditional
        JExpr condExpr = aForStmt.getConditional();

        // Get update statements
        List<JStmtExpr> updateStmts = aForStmt.getUpdateStmts();

        // Iterate while conditional is true
        while (true) {

            // Evaluate conditional and break if false
            Object condValue = evalExpr(condExpr);
            if (!SnapUtils.booleanValue(condValue))
                break;

            // Evaluate block statements
            evalStmt(anOR, blockStmt);

            // Execute update statements
            for (JStmtExpr updateStmt : updateStmts)
                evalStmt(anOR, updateStmt);
        }

        // Return
        return null;
    }

    /**
     * Evaluate JStmtFor.
     */
    public Object evalForEachStmt(Object anOR, JStmtFor aForStmt) throws Exception
    {
        // Get block statement
        JStmtBlock blockStmt = aForStmt.getBlock();

        // Get main variable name
        JStmtVarDecl initDeclStmt = aForStmt.getInitDecl();
        List<JVarDecl> varDecls = initDeclStmt.getVarDecls();
        JVarDecl varDecl0 = varDecls.get(0);
        String varName = varDecl0.getName();

        // Get list value
        JExpr listExpr = aForStmt.getConditional();
        Object listValue = evalExpr(listExpr);

        // If Object[], convert to list
        if (listValue instanceof Object[])
            listValue = Arrays.asList((Object[]) listValue);

        // Handle Iterable
        if (listValue instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) listValue;
            for (Object obj : iterable) {
                _exprEval.setLocalVarValue(varName, obj);
                evalStmt(anOR, blockStmt);
            }
        }

        // Return
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
                Object val = evalExpr(initExpr);
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
    public Object evalExpr(JExpr anExpr) throws Exception
    {
        // Evaluate expr
        Object val = _exprEval.evalExpr(anExpr);

        // Return
        return val;
    }
}