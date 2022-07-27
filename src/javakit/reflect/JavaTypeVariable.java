/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.TypeVariable;

/**
 * This class represents a Java TypeVariable.
 */
public class JavaTypeVariable extends JavaType {

    /**
     * Constructor.
     */
    public JavaTypeVariable(Resolver anOwner, JavaDecl aPar, TypeVariable<?> typeVar)
    {
        // Do normal version
        super(anOwner, aPar, typeVar);

        _type = DeclType.TypeVar;
        _name = _simpleName = typeVar.getName();

        Class<?> typeVarClass = ResolverUtils.getClassForType(typeVar);
        _evalType = getJavaClassForClass(typeVarClass);
    }

    /**
     * Override to return false.
     */
    @Override
    public boolean isResolvedType()  { return false; }
}
