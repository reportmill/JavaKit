/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import java.util.*;

/**
 * A JNode subclass for type variables (aka type paramters).
 * These are the unresolved types found in class, method, constructor declarations:
 * public class MyClass <T>
 * public <T> T myMethod(T anObj) { ... }
 */
public class JTypeVar extends JNode {

    // The name identifier
    JExprId _id;

    // The list of types
    List<JType> _types = new ArrayList();

    /**
     * Returns the identifier.
     */
    public JExprId getId()
    {
        return _id;
    }

    /**
     * Sets the identifier.
     */
    public void setId(JExprId anId)
    {
        replaceChild(_id, _id = anId);
        if (_id != null) setName(_id.getName());
    }

    /**
     * Returns the types.
     */
    public List<JType> getTypes()
    {
        return _types;
    }

    /**
     * Adds a type.
     */
    public void addType(JType aType)
    {
        _types.add(aType);
        addChild(aType, -1);
    }

    /**
     * Returns the bounds type.
     */
    public JavaDecl getBoundsType()
    {
        return _types.size() > 0 ? _types.get(0).getDecl() : getJavaDecl(Object.class);
    }

    /**
     * Override to get JavaDecl from parent decl (Class, Method).
     */
    protected JavaDecl getDeclImpl()
    {
        JavaDecl pdecl = getParent().getDecl();
        if (pdecl == null) return null;
        String name = getName();
        JavaDecl tvar = pdecl.getTypeVar(name);
        return tvar;
    }

    /**
     * Override to handle ID and nested case, e.g.: T extends Class <? super T>
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        // Handle ID
        if (aNode == _id)
            return getDecl();

        // Handle nested case, e.g.: T extends Class <? super T>
        if (aNode.getName().equals(getName()))
            return getJavaDecl(Object.class);

        // Do normal version
        return super.getDeclImpl(aNode);
    }

}