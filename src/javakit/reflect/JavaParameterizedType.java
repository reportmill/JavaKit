/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;
import javakit.resolver.ResolverUtils;
import snap.util.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * This class represents a Java ParameterizedType.
 */
public class JavaParameterizedType extends JavaType {

    /**
     * Constructor.
     */
    public JavaParameterizedType(Resolver anOwner, JavaDecl aPar, ParameterizedType parameterizedType)
    {
        // Do normal version
        super(anOwner, aPar, parameterizedType);

        _type = DeclType.ParamType;
        _name = ResolverUtils.getTypeName(parameterizedType);

        Type rawType = parameterizedType.getRawType();
        _parent = _resolver.getTypeDecl(rawType);
        _superDecl = (JavaType) _parent;

        Type[] typArgs = parameterizedType.getActualTypeArguments();
        _paramTypes = new JavaType[typArgs.length];

        for (int i = 0, iMax = typArgs.length; i < iMax; i++)
            _paramTypes[i] = _resolver.getTypeDecl(typArgs[i]);

        _evalType = this;
        _simpleName = _parent.getSimpleName() + '<' + StringUtils.join(getParamTypeSimpleNames(), ",") + '>';

        _resolver._decls.put(_id, this);
    }

    /**
     * Constructor.
     */
    public JavaParameterizedType(Resolver anOwner, JavaDecl aPar, JavaType[] theTypes)
    {
        // Do normal version
        super(anOwner, aPar, theTypes);

        _type = DeclType.ParamType;
        _id = ResolverUtils.getParamTypeId(aPar, theTypes);

        _name = _id;
        _paramTypes = theTypes;
        _evalType = this;
        _simpleName = _parent.getSimpleName() + '<' + StringUtils.join(getParamTypeSimpleNames(), ",") + '>';

        _resolver._decls.put(_id, this);
    }
}
