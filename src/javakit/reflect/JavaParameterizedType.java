/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import snap.util.StringUtils;

/**
 * This class represents a Java ParameterizedType.
 */
public class JavaParameterizedType extends JavaType {

    // The JavaDecls for parameter types for Constructor, Method
    protected JavaType[]  _paramTypes;

    /**
     * Constructor.
     */
    public JavaParameterizedType(Resolver anOwner, JavaDecl aPar, JavaType[] theTypes)
    {
        // Do normal version
        super(anOwner, aPar, theTypes);

        // Set type/id
        _type = DeclType.ParamType;
        _id = _name = ResolverUtils.getIdForParameterizedTypeParts(aPar, theTypes);

        // Set type info
        _paramTypes = theTypes;
        _evalType = this;
        _simpleName = _parent.getSimpleName() + '<' + StringUtils.join(getParamTypeSimpleNames(), ",") + '>';
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
        int ind = javaClass.getTypeVarIndexForName(typeVarName);
        if (ind >= 0 && ind < _paramTypes.length)
            return _paramTypes[ind];

        // Do normal version
        return super.getResolvedType(aDecl);
    }
}
