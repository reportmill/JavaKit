/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;

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
        _simpleName = getSimpleName(aPackageName);
    }

    /**
     * Returns a simple class name.
     */
    private static String getSimpleName(String cname)
    {
        int i = cname.lastIndexOf('$');
        if (i < 0) i = cname.lastIndexOf('.');
        if (i > 0) cname = cname.substring(i + 1);
        return cname;
    }
}
