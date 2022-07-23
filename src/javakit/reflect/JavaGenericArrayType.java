/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;
import java.lang.reflect.GenericArrayType;

/**
 * This class represents a Java GenericArrayType.
 */
public class JavaGenericArrayType extends JavaType {

    /**
     * Constructor.
     */
    public JavaGenericArrayType(Resolver anOwner, JavaDecl aPar, GenericArrayType aGenArrayType)
    {
        // Do normal version
        super(anOwner, aPar, aGenArrayType);

        _type = DeclType.TypeVar;
        _name = _simpleName = aGenArrayType.getTypeName();
        _evalType = getClassDecl(Object[].class);
        _resolver._decls.put(_id, this);
    }
}
