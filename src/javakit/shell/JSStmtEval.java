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
public class JSStmtEval {

    // The Expression evaluator
    private JSExprEval _exprEval = new JSExprEval();

    /**
     * Constructor.
     */
    public JSStmtEval()
    {

    }

    /**
     * Evaluate JStmt.
     */
    public Object evalStmt(Object anOR, JStmt aStmt) throws Exception
    {
        // Dunno
        _exprEval._thisObj = anOR;

        // Handle Assert statement
        if (aStmt instanceof JStmtAssert)
            throw new RuntimeException("JSStmtEval: Assert Statement not implemented");

        // Handle block statement
        if (aStmt instanceof JStmtBlock)
            return evalBlockStmt(anOR, (JStmtBlock) aStmt);

        // Handle break statement
        if (aStmt instanceof JStmtBreak)
            throw new RuntimeException("JSStmtEval: break Statement not implemented");

        // Handle ClassDecl
        if (aStmt instanceof JStmtClassDecl)
            throw new RuntimeException("JSStmtEval: ClassDecl Statement not implemented");

        // Handle constructor call
        if (aStmt instanceof JStmtConstrCall)
            throw new RuntimeException("JSStmtEval: constructor Statement not implemented");

        // Handle continue statement
        if (aStmt instanceof JStmtContinue)
            throw new RuntimeException("JSStmtEval: continue Statement not implemented");

        // Handle Do statement
        if (aStmt instanceof JStmtDo)
            throw new RuntimeException("JSStmtEval: Do Statement not implemented");

        // Empty statement
        if(aStmt instanceof JStmtEmpty)
            return null;

        // Expression statement
        if (aStmt instanceof JStmtExpr)
            return evalExprStmt((JStmtExpr) aStmt);

        // For statement
        if (aStmt instanceof JStmtFor)
            return evalForStmt(anOR, (JStmtFor) aStmt);

        // Handle if statement
        if (aStmt instanceof JStmtIf)
            return evalIfStmt(anOR, (JStmtIf) aStmt);

        // Handle labeled statement
        if (aStmt instanceof JStmtLabeled)
            throw new RuntimeException("JSStmtEval: labeled Statement not implemented");

        // Handle return statement
        if (aStmt instanceof JStmtReturn)
            throw new RuntimeException("JSStmtEval: return Statement not implemented");

        // Handle switch statement
        if (aStmt instanceof JStmtSwitch)
            throw new RuntimeException("JSStmtEval: switch Statement not implemented");

        // Handle sync statement
        if (aStmt instanceof JStmtSynchronized)
            return evalStmt(anOR, aStmt.getBlock());

        // Handle throw statement
        if (aStmt instanceof JStmtThrow)
            throw new RuntimeException("JSStmtEval: throw Statement not implemented");

        // Handle try statement
        if (aStmt instanceof JStmtTry)
            throw new RuntimeException("JSStmtEval: try Statement not implemented");

        // Handle variable declaration statement
        if (aStmt instanceof JStmtVarDecl)
            return evalVarDeclStmt((JStmtVarDecl) aStmt);

        // Handle while statement
        if (aStmt instanceof JStmtWhile)
            throw new RuntimeException("JSStmtEval: while Statement not implemented");

        // Complain
        throw new RuntimeException("EvalStmt.evalStmt: Unsupported statement " + aStmt.getClass());
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
    public Object evalIfStmt(Object anOR, JStmtIf anIfStmt) throws Exception
    {
        // Get conditional
        JExpr condExpr = anIfStmt.getConditional();

        Object condValue = evalExpr(condExpr);
        if (SnapUtils.booleanValue(condValue)) {

            // Get true statement and eval
            JStmt trueStmt = anIfStmt.getStatement();
            return evalStmt(anOR, trueStmt);
        }

        // Return
        return null;
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
    public Object evalVarDeclStmt(JStmtVarDecl aStmt) throws Exception
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