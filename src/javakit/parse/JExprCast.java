/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;

/**
 * A JExpr subclass for Cast expressions.
 */
public class JExprCast extends JExpr {

    // The cast type
    JType _type;

    // The real expression for cast
    JExpr _expr;

    /**
     * Returns the cast JType.
     */
    public JType getType()  { return _type; }

    /**
     * Sets the cast JType.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the expression being cast.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the cast expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "Cast"; }

    /**
     * Override to return declaration of type.
     */
    protected JavaDecl getDeclImpl()
    {
        return _type.getDecl();
    }
}
