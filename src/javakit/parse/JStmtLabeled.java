/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import snap.util.SnapUtils;

/**
 * A Java statement for Labeled Statement.
 */
public class JStmtLabeled extends JStmt {

    // The label identifier
    JExprId  _labelId;

    // The actual statement
    JStmt  _stmt;

    // The bogus label variable declaration
    JVarDecl _varDecl;

    /**
     * Returns the label.
     */
    public JExprId getLabel()  { return _labelId; }

    /**
     * Sets the label.
     */
    public void setLabel(JExprId anExpr)
    {
        replaceChild(_labelId, _labelId = anExpr);
    }

    /**
     * Returns the label name.
     */
    public String getLabelName()
    {
        return _labelId != null ? _labelId.getName() : null;
    }

    /**
     * Returns a bogus label variable declaration.
     */
    public JVarDecl getLabelVarDecl()
    {
        // If already set, just return
        if (_varDecl != null) return _varDecl;

        // Create VarDecl
        _varDecl = new JVarDecl();
        _varDecl._id = _labelId;
        _varDecl.setName(_labelId.getName());

        // Create type and add to VarDecl
        JavaClass stringClass = getJavaClassForClass(String.class);
        JType type = new JType.Builder().name("String").type(stringClass).build();
        _varDecl._type = type;
        type.setParent(_varDecl);

        // Add VarDecl to this node
        _varDecl.setParent(this);

        // Return
        return _varDecl;
    }

    /**
     * Returns the statement.
     */
    public JStmt getStmt()  { return _stmt; }

    /**
     * Sets the statement.
     */
    public void setStmt(JStmt aStmt)
    {
        replaceChild(_stmt, _stmt = aStmt);
    }

    /**
     * Override to handle label variable declaration.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // Check label name
        String name = anExprId.getName();
        if (SnapUtils.equals(getLabelName(), name))
            return getLabelVarDecl().getDecl();

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }
}