/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.Constructor;

/**
 * This class represents a Java Method or Constructor.
 */
public class JavaConstructor extends JavaExecutable {

    // The super implementation of this method
    protected JavaConstructor  _super;

    /**
     * Constructor.
     */
    public JavaConstructor(Resolver aResolver, JavaClass aDeclaringClass, Constructor<?> constructor)
    {
        super(aResolver, DeclType.Constructor, aDeclaringClass, constructor);

        // Set EvalType to DeclaringClass
        _evalType = aDeclaringClass;
    }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaConstructor getSuper()
    {
        // If already set, just return
        if (_super != null)
            return _super != this ? _super : null;

        // Get superclass and helper
        JavaClass declaringClass = getDeclaringClass();
        JavaClass superClass = declaringClass != null ? declaringClass.getSuper() : null;
        if (superClass == null)
            return null;

        // Get super method
        JavaType[] paramTypes = getParamTypes();
        JavaConstructor superMethod = superClass.getConstructorDeepForTypes(paramTypes);
        if (superMethod == null)
            superMethod = this;

        // Set/return
        return _super = superMethod;
    }
}
