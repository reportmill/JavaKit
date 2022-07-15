/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for BreakStatement.
 */
public class JStmtBreak extends JStmt {
    // The break label
    public JExpr _label;

    /**
     * Returns the label.
     */
    public JExpr getLabel()
    {
        return _label;
    }

    /**
     * Sets the label.
     */
    public void setLabel(JExpr anExpr)
    {
        replaceChild(_label, _label = anExpr);
    }

}