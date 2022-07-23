/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;
import javakit.resolver.ResolverUtils;
import snap.util.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * This class represents a Java TypeVariable.
 */
public class JavaTypeVariable extends JavaType {

    /**
     * Constructor.
     */
    public JavaTypeVariable(Resolver anOwner, JavaDecl aPar, TypeVariable aType)
    {
        // Do normal version
        super(anOwner, aPar, aType);

        TypeVariable typeVar = (TypeVariable) aType;
        _type = DeclType.TypeVar;
        _name = _simpleName = typeVar.getName();
        Type[] etypes = typeVar.getBounds();
        Class ecls = ResolverUtils.getClassForType(etypes[0]);
        _evalType = getClassDecl(ecls);
        _resolver._decls.put(_id, this);
    }

    /**
     * Init object.
     */
    protected void initObject(Object anObj)  { }
}
