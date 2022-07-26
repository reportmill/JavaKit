/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import snap.util.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * This class represents a Java ParameterizedType.
 */
public class JavaParameterizedType extends JavaType {

    // The JavaDecls for parameter types for Constructor, Method
    protected JavaType[]  _paramTypes;

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
        _superType = (JavaType) _parent;

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
        _id = ResolverUtils.getIdForParameterizedTypeParts(aPar, theTypes);

        _name = _id;
        _paramTypes = theTypes;
        _evalType = this;
        _simpleName = _parent.getSimpleName() + '<' + StringUtils.join(getParamTypeSimpleNames(), ",") + '>';

        _resolver._decls.put(_id, this);
    }

    /**
     * Returns the parameter types.
     */
    public JavaType[] getParamTypes()  { return _paramTypes; }

    /**
     * Returns the parameter type simple names.
     */
    public String[] getParamTypeSimpleNames()
    {
        String[] names = new String[_paramTypes.length];
        for (int i = 0; i < names.length; i++) names[i] = _paramTypes[i].getSimpleName();
        return names;
    }

    /**
     * Returns whether is Type is explicit (doesn't contain any type variables).
     */
    public boolean isResolvedType()
    {
        // ParamType might subclass TypeVars
        JavaDecl parent = getParent();
        if (parent instanceof JavaTypeVariable)
            return false;

        // Or types might include TypeVars
        JavaType[] paramTypes = getParamTypes();
        for (JavaType paramType : paramTypes)
            if (paramType instanceof JavaTypeVariable)
                return false;

        // Return
        return true;
    }

    /**
     * Returns a resolved type for given unresolved type (TypeVar or ParamType<TypeVar>), if this decl can resolve it.
     */
    @Override
    public JavaType getResolvedType(JavaDecl aDecl)
    {
        // Search for TypeVar name in ParamTypes
        String typeVarName = aDecl.getName();
        JavaClass javaClass = getClassType();
        int ind = javaClass.getTypeVarIndex(typeVarName);
        if (ind >= 0 && ind < _paramTypes.length)
            return _paramTypes[ind];

        // Do normal version
        return super.getResolvedType(aDecl);
    }
}
