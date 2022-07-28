/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.*;

/**
 * This class represents a Java Method.
 */
public class JavaMethod extends JavaExecutable {

    // Whether method is Default method
    private boolean  _default;

    // The super implementation of this method
    protected JavaMethod  _super;

    /**
     * Constructor.
     */
    public JavaMethod(Resolver aResolver, JavaClass aDeclaringClass, Method aMethod)
    {
        super(aResolver, DeclType.Method, aDeclaringClass, aMethod);

        // Get whether default
        _default = aMethod.isDefault();
    }

    /**
     * Resolves types.
     */
    protected void initTypes(Method aMethod)
    {
        // Get/set EvalType to method return Type
        Type returnType = aMethod.getGenericReturnType();
        _evalType = _resolver.getTypeDecl(returnType);

        // Do normal version
        super.initTypes(aMethod);
    }

    /**
     * Returns whether Method is default type.
     */
    public boolean isDefault()  { return _default; }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaMethod getSuper()
    {
        // If already set, just return
        if (_super != null)
            return _super != this ? _super : null;

        // Get superclass and helper
        JavaClass declaringClass = getDeclaringClass();
        JavaClass superClass = declaringClass != null ? declaringClass.getSuperClass() : null;
        if (superClass == null)
            return null;

        // Get super method
        String name = getName();
        JavaType[] paramTypes = getParamTypes();
        JavaMethod superMethod = superClass.getMethodDeepForNameAndTypes(name, paramTypes);
        if (superMethod == null)
            superMethod = this;

        // Set/return
        return _super = superMethod;
    }
}
