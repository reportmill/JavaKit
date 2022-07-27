/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.TypeVariable;

/**
 * This class represents a Java TypeVariable.
 */
public class JavaTypeVariable extends JavaType {

    // The Class or Executable that owns this TypeVariable
    private JavaDecl  _owner;

    /**
     * Constructor.
     */
    public JavaTypeVariable(Resolver aResolver, JavaDecl anOwner, TypeVariable<?> typeVar)
    {
        // Do normal version
        super(aResolver);

        // Set type, name
        _type = DeclType.TypeVar;
        _name = _simpleName = typeVar.getName();

        // Set owner
        _owner = anOwner;

        Class<?> typeVarClass = ResolverUtils.getClassForType(typeVar);
        _evalType = getJavaClassForClass(typeVarClass);
    }

    /**
     * Returns the Class or Executable that owns this TypeVariable.
     */
    public JavaDecl getOwner()  { return _owner; }

    /**
     * Override to return false.
     */
    @Override
    public boolean isResolvedType()  { return false; }
}
