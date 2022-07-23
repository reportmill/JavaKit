/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;

/**
 * This class represents a Java Package.
 */
public class JavaPackage extends JavaDecl {

    /**
     * Constructor.
     */
    public JavaPackage(Resolver anOwner, JavaDecl aPar, String aPackageName)
    {
        super(anOwner, aPar, aPackageName);

        _type = DeclType.Package;
        _name = aPackageName;
        _simpleName = Resolver.getSimpleName(aPackageName);
    }
}
