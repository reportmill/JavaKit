/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;

/**
 * This class represents a Java Package.
 */
public class JavaPackage extends JavaDecl {

    // The package
    private JavaPackage  _package;

    /**
     * Constructor.
     */
    public JavaPackage(Resolver aResolver, JavaPackage aParent, String aPackageName)
    {
        super(aResolver);

        _type = DeclType.Package;
        _name = aPackageName;
        _simpleName = getSimpleName(aPackageName);

        _package = aParent;
    }

    /**
     * Returns the parent package.
     */
    public JavaPackage getPackage()  { return _package; }

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
