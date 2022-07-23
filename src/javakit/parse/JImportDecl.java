/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaType;

import java.util.*;

/**
 * A Java part for import declaration.
 */
public class JImportDecl extends JNode {

    // The import name expression
    JExpr _nameExpr;

    // Whether import is static
    boolean _static;

    // Whether import is inclusive (ends with '.*')
    boolean _inclusive;

    // Whether import is used
    boolean _used;

    // The list of child class names found by this import, if inclusive
    Set<String> _found = Collections.EMPTY_SET;

    /**
     * Returns the name expression.
     */
    public JExpr getNameExpr()
    {
        return _nameExpr;
    }

    /**
     * Sets the name expression.
     */
    public void setNameExpr(JExpr anExpr)
    {
        replaceChild(_nameExpr, _nameExpr = anExpr);
        if (_nameExpr != null) setName(_nameExpr.getString());
    }

    /**
     * Returns whether import is static.
     */
    public boolean isStatic()
    {
        return _static;
    }

    /**
     * Sets whether import is static.
     */
    public void setStatic(boolean aValue)
    {
        _static = aValue;
    }

    /**
     * Returns whether import is inclusive.
     */
    public boolean isInclusive()
    {
        return _inclusive;
    }

    /**
     * Sets whether import is inclusive.
     */
    public void setInclusive(boolean aValue)
    {
        _inclusive = aValue;
    }

    /**
     * Returns whether import is class name.
     */
    public boolean isClassName()
    {
        JavaDecl jd = getDecl();
        return jd != null && jd.isClass();
    }

    /**
     * Returns class or package declaration.
     */
    protected JavaDecl getDeclImpl()
    {
        String name = getName();
        if (name == null) return null;
        if (_inclusive && isKnownPackageName(name)) return getJavaDecl(name);

        // Iterate up parts of import till we find Class in case import is like: import path.Class.InnerClass;
        while (name != null) {
            if (isKnownClassName(name)) {
                if (name.length() < getName().length()) {
                    String icname = getName().substring(name.length()).replace('.', '$'), name2 = name + icname;
                    if (isKnownClassName(name2))
                        name = name2;
                }
                return getJavaDecl(name);
            }
            int i = name.lastIndexOf('.');
            if (i > 0) name = name.substring(0, i);
            else name = null;
        }

        // If class not found, return as package decl anyway
        return getJavaDecl(getName());
    }

    /**
     * Returns the class name for a given name.
     */
    public String getImportClassName(String aName)
    {
        String cname = isClassName() ? getEvalClassName() : getName();
        if (_inclusive) {
            if (!isStatic() || !cname.endsWith(aName))
                cname += (isClassName() ? '$' : '.') + aName;
        }
        return cname;
    }

    /**
     * Returns the member for given name and parameter types (if method) for static import.
     */
    public JavaDecl getImportMember(String aName, JavaType[] theParams)
    {
        JavaClass cls = (JavaClass) getEvalType();
        if (cls == null) return null;
        if (theParams == null)
            return cls.getField(aName);
        return cls.getCompatibleMethodAll(aName, theParams);
    }

    /**
     * Returns the list of child class names found by this import (if inclusive).
     */
    public Set<String> getFoundClassNames()
    {
        return _found;
    }

    /**
     * Adds a child class name to list of those
     */
    protected void addFoundClassName(String aName)
    {
        if (_found == Collections.EMPTY_SET) _found = new HashSet();
        _found.add(aName);
    }

}