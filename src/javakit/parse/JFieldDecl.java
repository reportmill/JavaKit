/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import javakit.reflect.JavaDecl;

import java.util.*;

/**
 * A JMember for Field declarations.
 */
public class JFieldDecl extends JMemberDecl {
    // The type/return-type
    JType _type;

    // List of variable declarations
    List<JVarDecl> _vars = new ArrayList();

    /**
     * Returns the field type.
     */
    public JType getType()
    {
        return _type;
    }

    /**
     * Sets the field type.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the variable declarations.
     */
    public List<JVarDecl> getVarDecls()
    {
        return _vars;
    }

    /**
     * Adds a variable declarations.
     */
    public void addVarDecl(JVarDecl aVD)
    {
        _vars.add(aVD);
        addChild(aVD, -1);
    }

    /**
     * Override to return first var decl.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        return _vars.size() > 0 ? _vars.get(0).getDecl() : null;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        return "FieldDecl";
    }

}