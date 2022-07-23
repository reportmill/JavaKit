/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaType;

import java.util.*;

/**
 * A Java statement for SwitchStatement.
 */
public class JStmtSwitch extends JStmt {
    // The expression
    JExpr _expr;

    // The list of SwitchLabels
    List<SwitchLabel> _switchLabels = new ArrayList();

    /**
     * Returns the expression.
     */
    public JExpr getExpr()
    {
        return _expr;
    }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Returns the switch labels.
     */
    public List<SwitchLabel> getSwitchLabels()
    {
        return _switchLabels;
    }

    /**
     * Adds a switch label.
     */
    public void addSwitchLabel(SwitchLabel aSL)
    {
        _switchLabels.add(aSL);
        addChild(aSL, -1);
    }

    /**
     * A class to represent individual labels in a switch statement.
     */
    public static class SwitchLabel extends JNode {

        // Whether label is default
        private boolean _default;

        // The expression
        private JExpr  _expr;

        // The block statements
        private List<JStmt>  _stmts = new ArrayList<>();

        /**
         * Returns whether label is default.
         */
        public boolean isDefault()
        {
            return _default;
        }

        /**
         * Sets whether label is default.
         */
        public void setDefault(boolean aValue)
        {
            _default = aValue;
        }

        /**
         * Returns the expression.
         */
        public JExpr getExpr()
        {
            return _expr;
        }

        /**
         * Sets the expression.
         */
        public void setExpr(JExpr anExpr)
        {
            replaceChild(_expr, _expr = anExpr);
        }

        /**
         * Returns the statements.
         */
        public List<JStmt> getStatements()
        {
            return _stmts;
        }

        /**
         * Adds a statement.
         */
        public void addStatement(JStmt aStmt)
        {
            _stmts.add(aStmt);
            addChild(aStmt, -1);
        }

        /**
         * Override to check inner variable declaration statements.
         */
        protected JavaDecl getDeclImpl(JNode aNode)
        {
            // If node is case label and is id, try to evaluate against Switch expression enum type
            if (aNode == _expr && _expr instanceof JExprId) {
                String name = _expr.getName();
                JStmtSwitch swStmt = getParent(JStmtSwitch.class);
                JExpr swExpr = swStmt.getExpr();
                JavaType sdecl = swExpr != null ? swExpr.getEvalType() : null;
                if (sdecl != null && sdecl.isEnum()) {
                    JavaClass edecl = sdecl.getClassType();
                    JavaDecl enumConst = edecl.getField(name);
                    if (enumConst != null)
                        return enumConst;
                }
            }

            // If statements (as block) can resolve node, return decl
            JavaDecl decl = JStmtBlock.getDeclImpl(aNode, getStatements());
            if (decl != null)
                return decl;

            // Do normal version
            return super.getDeclImpl(aNode);
        }
    }

}