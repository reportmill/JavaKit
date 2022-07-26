/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.*;

/**
 * This class represents
 */
public class JavaMember extends JavaDecl {

    /**
     * Constructor.
     */
    public JavaMember(Resolver anOwner, JavaDecl aPar, Member aMember)
    {
        super(anOwner, aPar, aMember);

        // Set mods, name, simple name
        _mods = aMember.getModifiers();
        _name = _simpleName = aMember.getName();
    }
}
