/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;

/**
 * This class represents a generic Java Type: Class, ParameterizedType, TypeVariable, GenericArrayType.
 */
public class JavaType extends JavaDecl {

    // The super implementation of this type (Class, Method, Constructor)
    protected JavaType  _superType;

    /**
     * Constructor.
     */
    protected JavaType(Resolver anOwner, JavaDecl aPar, Object aType)
    {
        // Do normal version
        super(anOwner, aPar, aType);

    }

    /**
     * Returns whether is a Type (Class, ParamType, TypeVar).
     */
    @Override
    public boolean isType()  { return true; }

    /**
     * Returns whether is a enum reference.
     */
    public boolean isEnum()  { return false; }

    /**
     * Returns whether is a interface reference.
     */
    public boolean isInterface()  { return false; }

    /**
     * Returns whether is an array.
     */
    public boolean isArray()  { return false; }

    /**
     * Returns the Array item type (if Array).
     */
    public JavaType getArrayItemType()  { return null; }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaType getSuper()  { return _superType; }

    /**
     * Returns common ancestor of this decl and given decls.
     */
    public JavaType getCommonAncestor(JavaType aDecl)
    {
        if (aDecl == this) return this;

        // Handle primitive
        if (isPrimitive() && aDecl.isPrimitive())
            return getCommonAncestorPrimitive(aDecl);
        else if (isPrimitive())
            return getPrimitiveAlt().getCommonAncestor(aDecl);
        else if (aDecl.isPrimitive())
            return getCommonAncestor(aDecl.getPrimitiveAlt());

        // Iterate up each super chain to check
        for (JavaType d0 = this; d0 != null; d0 = d0.getSuper())
            for (JavaType d1 = aDecl; d1 != null; d1 = d1.getSuper())
                if (d0 == d1)
                    return d0;

        // Return Object (case where at least one was interface or ParamType of interface)
        return getJavaType(Object.class);
    }

    /**
     * Returns common ancestor of this decl and given decls.
     */
    protected JavaType getCommonAncestorPrimitive(JavaType aDecl)
    {
        String n0 = getName();
        String n1 = aDecl.getName();
        if (n0.equals("double")) return this;
        if (n1.equals("double")) return aDecl;
        if (n0.equals("float")) return this;
        if (n1.equals("float")) return aDecl;
        if (n0.equals("long")) return this;
        if (n1.equals("long")) return aDecl;
        if (n0.equals("int")) return this;
        if (n1.equals("int")) return aDecl;
        if (n0.equals("short")) return this;
        if (n1.equals("short")) return aDecl;
        if (n0.equals("char")) return this;
        if (n1.equals("char")) return aDecl;
        return this;
    }

    /**
     * Returns whether is Type is explicit (doesn't contain any type variables).
     */
    public boolean isResolvedType()  { return true; }

    /**
     * Returns a resolved type for given unresolved type (TypeVar or ParamType<TypeVar>), if this decl can resolve it.
     */
    public JavaType getResolvedType(JavaDecl aDecl)
    {
        // Handle ParamType and anything not a TypeVar
        if (aDecl.isParamType()) {
            System.err.println("JavaDecl.getResolvedType: ParamType not yet supported");
            return (JavaParameterizedType) aDecl;
        }

        // Should always be a TypeVar I think
        if (!aDecl.isTypeVar())
            return (JavaType) aDecl;

        // If not resolve, just return bounds type
        return aDecl.getEvalType();
    }

    /**
     * Returns whether given declaration collides with this declaration.
     */
    public boolean matches(JavaDecl aDecl)
    {
        // Check identity
        if (aDecl == this) return true;

        // Handle ParamTypes: Test against ClassType instead
        if (isParamType())
            return getClassType().matches(aDecl);
        else if (aDecl.isParamType())
            return matches(aDecl.getClassType());

        // Return false, since no match
        return false;
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        String simpleName = getSimpleName();
        String parentName = getParentName();
        return simpleName + " - " + parentName;
    }

    /**
     * Returns the string to use when inserting this suggestion into code.
     */
    @Override
    public String getReplaceString()
    {
        return getSimpleName();
    }
}
