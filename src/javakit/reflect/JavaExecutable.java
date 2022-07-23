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
     * Initialize member (Field, Method, Constructor).
     */
    private void initMember(Member aMember)
    {
        // Set type
        Constructor constructor = (Constructor) aMember;
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

    /**
     * Returns whether Method/Constructor is VarArgs type.
     */
    public boolean isVarArgs()
    {
        return _varArgs;
    }
}
