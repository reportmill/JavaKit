/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;
import javakit.resolver.ResolverUtils;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * This class represents a Java TypeVariable.
 */
public class JavaTypeVariable extends JavaType {

    /**
     * Constructor.
     */
    public JavaTypeVariable(Resolver anOwner, JavaDecl aPar, TypeVariable typeVar)
    {
        // Do normal version
        super(anOwner, aPar, typeVar);

        _type = DeclType.TypeVar;
        _name = _simpleName = typeVar.getName();

        Type[] typeVarTypes = typeVar.getBounds();
        Type typeVarType = typeVarTypes[0];
        Class typeVarClass = ResolverUtils.getClassForType(typeVarType);
        _evalType = getJavaClass(typeVarClass);

        _resolver._decls.put(_id, this);
    }

    /**
     * Override to return false.
     */
    @Override
    public boolean isResolvedType()  { return false; }
}
