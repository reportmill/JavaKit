/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.reflect.JavaType;
import snap.util.ClassUtils;

/**
 * A JNode for types.
 */
public class JType extends JNode {

    // Whether type is primitive type
    protected boolean  _primitive;

    // Whether is reference (array or class/interface type)
    protected int  _arrayCount;

    // The generic Types
    private List<JType>  _typeArgs;

    // The base type
    private JavaType  _baseDecl;

    /**
     * Returns whether type is primitive type.
     */
    public boolean isPrimitive()
    {
        return _primitive;
    }

    /**
     * Sets whether type is primitive type.
     */
    public void setPrimitive(boolean aValue)
    {
        _primitive = aValue;
    }

    /**
     * Returns whether type is array.
     */
    public boolean isArrayType()
    {
        return _arrayCount > 0;
    }

    /**
     * Returns the array count if array type.
     */
    public int getArrayCount()
    {
        return _arrayCount;
    }

    /**
     * Sets the array count.
     */
    public void setArrayCount(int aValue)
    {
        _arrayCount = aValue;
    }

    /**
     * Returns the generic types.
     */
    public List<JType> getTypeArgs()
    {
        return _typeArgs;
    }

    /**
     * Adds a type arg.
     */
    public void addTypeArg(JType aType)
    {
        if (_typeArgs == null) _typeArgs = new ArrayList();
        _typeArgs.add(aType);
        addChild(aType, -1);
    }

    /**
     * Returns the number of type args.
     */
    public int getTypeArgCount()
    {
        return _typeArgs.size();
    }

    /**
     * Returns the type arg type at given index.
     */
    public JType getTypeArg(int anIndex)
    {
        return _typeArgs.get(anIndex);
    }

    /**
     * Returns the type arg decl at given index.
     */
    public JavaType getTypeArgDecl(int anIndex)
    {
        JType targ = getTypeArg(anIndex);
        JavaType jd = targ.getDecl();
        return jd;
    }

    /**
     * Returns the simple name.
     */
    public String getSimpleName()
    {
        int index = _name.lastIndexOf('.');
        return index > 0 ? _name.substring(index + 1, _name.length()) : _name;
    }

    /**
     * Returns whether type is number type.
     */
    public boolean isNumberType()
    {
        JavaType decl = getBaseDecl();
        String tp = decl != null ? decl.getClassName() : null;
        if (tp == null) return false;
        tp = tp.intern();
        return tp == "byte" || tp == "short" || tp == "int" || tp == "long" || tp == "float" || tp == "double" ||
                tp == "java.lang.Byte" || tp == "java.lang.Short" || tp == "java.lang.Integer" || tp == "java.lang.Long" ||
                tp == "java.lang.Float" || tp == "java.lang.Double" || tp == "java.lang.Number";
    }

    /**
     * Override to return as JavaType.
     */
    @Override
    public JavaType getDecl()
    {
        JavaType javaType = (JavaType) super.getDecl();
        return javaType;
    }

    /**
     * Override to resolve type class name and create declaration from that.
     */
    protected JavaType getBaseDecl()
    {
        // If already set, just return
        if (_baseDecl != null) return _baseDecl;

        // Handle primitive type
        Class primitiveClass = ClassUtils.getPrimitiveClass(_name);
        if (primitiveClass != null)
            return _baseDecl = getJavaClass(primitiveClass);


        // If not primitive, try to resolve class
        JavaType javaClass = (JavaType) getDeclImpl(this);

        // Set/return
        return _baseDecl = javaClass;
    }

    /**
     * Override to resolve type class name and create declaration from that.
     */
    protected JavaType getDeclImpl()
    {
        // Get base decl
        JavaType decl = getBaseDecl();
        if (decl == null) {
            System.err.println("JType.getDeclImpl: Can't find base decl: " + getName());
            return getJavaClass(Object.class);
        }

        // If type args, build array and get decl for ParamType
        if (_typeArgs != null) {
            int len = _typeArgs.size();
            JavaType[] decls = new JavaType[len];
            for (int i = 0; i < len; i++)
                decls[i] = getTypeArgDecl(i);
            decl = decl.getParamTypeDecl(decls);
        }

        // If ArrayCount, get decl for array
        for (int i = 0; i < _arrayCount; i++)
            decl = decl.getArrayTypeDecl();

        // Return declaration
        if (decl == null)
            System.err.println("JType.getDeclImpl: Shouldn't happen: decl not found for " + getName());
        return decl;
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        System.out.println("JType.equals: Was called"); // I don't think this method is ever used

        // Check identity, get other JType, check SimpleNames and Decls
        if (anObj == this) return true;
        JType other = anObj instanceof JType ? (JType) anObj : null;
        if (other == null) return false;
        return getSimpleName().equals(other.getSimpleName()) && getDecl() == other.getDecl();
    }

    /**
     * Standard hashCode implementation.
     */
    public int hashCode()
    {
        return getDecl() != null ? getDecl().hashCode() : super.hashCode();
    }

}