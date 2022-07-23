/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * This class represents a Java Method or Constructor.
 */
public class JavaContructor extends JavaExecutable {

    // Whether method has VarArgs
    protected boolean  _varArgs;

    /**
     * Constructor.
     */
    public JavaContructor(Resolver anOwner, JavaDecl aPar, Constructor constructor)
    {
        super(anOwner, aPar, constructor);

        // Set type
        _type = DeclType.Constructor;

        // Reset name for constructor
        Class<?> declaringClass = constructor.getDeclaringClass();
        _name = _simpleName = declaringClass.getSimpleName();

        // Get TypeVars
        TypeVariable[] typeVars = constructor.getTypeParameters();
        _typeVars = new JavaTypeVariable[typeVars.length];
        for (int i = 0, iMax = typeVars.length; i < iMax; i++)
            _typeVars[i] = new JavaTypeVariable(_resolver, this, typeVars[i]);
        _varArgs = constructor.isVarArgs();

        // Get Return Type
        _evalType = _resolver.getTypeDecl(declaringClass);

        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Type[] paramTypes = constructor.getGenericParameterTypes();
        if (paramTypes.length < constructor.getParameterCount())
            paramTypes = constructor.getParameterTypes();
        _paramTypes = new JavaType[paramTypes.length];
        for (int i = 0, iMax = paramTypes.length; i < iMax; i++)
            _paramTypes[i] = _resolver.getTypeDecl(paramTypes[i]);
    }
}
