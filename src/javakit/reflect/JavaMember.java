/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.*;

/**
 * This class represents
 */
public class JavaMember extends JavaDecl {

    // The declaring class
    private JavaClass  _declaringClass;

    /**
     * Constructor.
     */
    public JavaMember(Resolver aResolver, JavaClass aDeclaringClass, Member aMember)
    {
        super(aResolver);

        // Set id
        _id = ResolverUtils.getIdForMember(aMember);

        // Set mods, declaring class
        _mods = aMember.getModifiers();
        _declaringClass = aDeclaringClass;

        // Set name/simple name
        _name = _simpleName = aMember.getName();
    }

    /**
     * Returns the declaring class.
     */
    public JavaClass getDeclaringClass()  { return _declaringClass; }

    /**
     * Returns the declaring class name.
     */
    public String getDeclaringClassName()  { return _declaringClass.getName(); }
}
