/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;
import java.lang.reflect.*;

/**
 * This class represents a Java Method or Constructor.
 */
public class JavaExecutable extends JavaMember {

    // Whether method has VarArgs
    protected boolean  _varArgs;

    /**
     * Constructor.
     */
    public JavaExecutable(Resolver anOwner, JavaDecl aPar, Member aMember)
    {
        super(anOwner, aPar, aMember);

        // Set mods, name, simple name
        _mods = aMember.getModifiers();
        _name = _simpleName = aMember.getName();
    }

    /**
     * Returns whether Method/Constructor is VarArgs type.
     */
    public boolean isVarArgs()
    {
        return _varArgs;
    }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaExecutable getSuper()  { return null; }

    /**
     * Returns whether given declaration collides with this declaration.
     */
    public boolean matches(JavaDecl aDecl)
    {
        // Check identity
        if (aDecl == this) return true;

        // If Types don't match, just return
        if (aDecl._type != _type)
            return false;

        // For Method, Constructor: Check supers
        JavaExecutable other = (JavaExecutable) aDecl;
        for (JavaExecutable sup = other.getSuper(); sup != null; sup = other.getSuper())
            if (sup == this)
                return true;

        // Return false, since no match
        return false;
    }
}
