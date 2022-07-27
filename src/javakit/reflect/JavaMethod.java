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
    public JavaMethod(Resolver anOwner, JavaDecl aPar, Method aMethod)
    {
        super(anOwner, aPar, aMethod);

        // Set type
        _type = DeclType.Method;

        // Get TypeVars
        TypeVariable<?>[] typeVars = aMethod.getTypeParameters();
        _typeVars = new JavaTypeVariable[typeVars.length];
        for (int i = 0, iMax = typeVars.length; i < iMax; i++)
            _typeVars[i] = new JavaTypeVariable(_resolver, this, typeVars[i]);

        // Get Return Type
        Type returnType = aMethod.getReturnType();
        _evalType = _resolver.getTypeDecl(returnType);

        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Type[] paramTypes = aMethod.getGenericParameterTypes();
        if (paramTypes.length < aMethod.getParameterCount())
            paramTypes = aMethod.getParameterTypes();
        _paramTypes = new JavaType[paramTypes.length];
        for (int i = 0, iMax = paramTypes.length; i < iMax; i++)
            _paramTypes[i] = _resolver.getTypeDecl(paramTypes[i]);

        // Get whether VarArgs
        _varArgs = aMethod.isVarArgs();

        // Get whether default
        _default = aMethod.isDefault();
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
        JavaClass javaClass = getClassType();
        JavaClass superClass = javaClass != null ? javaClass.getSuper() : null;
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
