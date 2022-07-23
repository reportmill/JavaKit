/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.*;
import javakit.resolver.Resolver;
import javakit.resolver.ResolverUtils;
import snap.util.*;

/**
 * A class to represent a declaration of a Java Class, Method, Field or Constructor.
 */
public class JavaDecl implements Comparable<JavaDecl> {

    // The Resolver that produced this decl
    protected Resolver  _resolver;

    // The JavaDecl (class) that this decl was declared in
    protected JavaDecl  _parent;

    // A unique identifier
    protected String  _id;

    // The type
    protected DeclType  _type;

    // The modifiers
    protected int  _mods;

    // The name of the declaration member
    protected String  _name;

    // The simple name of the declaration member
    protected String  _simpleName;

    // The type this decl evaluates to when referenced
    protected JavaType  _evalType;

    // The JavaDecls for parameter types for Constructor, Method
    protected JavaType[]  _paramTypes;

    // The JavaDecls for TypeVars for Class, Method
    protected JavaTypeVariable[]  _typeVars = EMPTY_TYPE_VARS;

    // The super implementation of this type (Class, Method, Constructor)
    protected JavaDecl  _superDecl = NULL_DECL;

    // Constants for type
    public enum DeclType { Class, Field, Constructor, Method, Package, VarDecl, ParamType, TypeVar }

    // Shared empty TypeVar array
    private static JavaDecl NULL_DECL = new JavaDecl();
    private static JavaTypeVariable[] EMPTY_TYPE_VARS = new JavaTypeVariable[0];

    /**
     * Constructor.
     */
    public JavaDecl(Resolver anOwner, JavaDecl aPar, Object anObj)
    {
        assert (anOwner != null);

        // Set ivars
        _resolver = anOwner;
        _parent = aPar;

        // Set id
        _id = ResolverUtils.getId(anObj);
    }

    /**
     * For NULL_DECL.
     */
    private JavaDecl()
    {
        _id = _name = _simpleName = "NULL_DECL";
        _type = DeclType.Package;
    }

    /**
     * Returns the id.
     */
    public String getId()  { return _id; }

    /**
     * Returns the type.
     */
    public DeclType getType()  { return _type; }

    /**
     * Returns whether is a class reference.
     */
    public boolean isClass()  { return false; }

    /**
     * Returns whether is primitive.
     */
    public boolean isPrimitive()  { return false; }

    /**
     * Returns the primitive counter part, if available.
     */
    public JavaClass getPrimitive()  { return null; }

    /**
     * Returns the primitive counter part, if available.
     */
    public JavaClass getPrimitiveAlt()  { return null; }

    /**
     * Returns whether is a field reference.
     */
    public boolean isField()  { return _type == DeclType.Field; }

    /**
     * Returns whether is a constructor reference.
     */
    public boolean isConstructor()  { return _type == DeclType.Constructor; }

    /**
     * Returns whether is a method reference.
     */
    public boolean isMethod()  { return _type == DeclType.Method; }

    /**
     * Returns whether is a package reference.
     */
    public boolean isPackage()  { return _type == DeclType.Package; }

    /**
     * Returns whether is a variable declaration reference.
     */
    public boolean isVarDecl()  { return _type == DeclType.VarDecl; }

    /**
     * Returns whether is a parameterized class.
     */
    public boolean isParamType()  { return _type == DeclType.ParamType; }

    /**
     * Returns whether is a TypeVar.
     */
    public boolean isTypeVar()  { return _type == DeclType.TypeVar; }

    /**
     * Returns whether is a Type (Class, ParamType, TypeVar).
     */
    public boolean isType()  { return false; }

    /**
     * Returns the modifiers.
     */
    public int getModifiers()  { return _mods; }

    /**
     * Returns whether decl is static.
     */
    public boolean isStatic()
    {
        return Modifier.isStatic(_mods);
    }

    /**
     * Returns the name.
     */
    public String getName()  { return _name; }

    /**
     * Returns the simple name.
     */
    public String getSimpleName()  { return _simpleName; }

    /**
     * Returns the type of the most basic class associated with this type:
     * Class: itself
     * Field, Method, Constructor, ParamType: Parent class
     * TypeVar: EvalType.ClassType
     * VarDecl, Package: null?
     */
    public JavaClass getClassType()
    {
        if (isClass()) return (JavaClass) this;
        if (isTypeVar()) return _evalType.getClassType();
        return _parent != null ? _parent.getClassType() : null;
    }

    /**
     * Returns the class name.
     */
    public String getClassName()
    {
        JavaDecl ct = getClassType();
        return ct != null ? ct.getName() : null;
    }

    /**
     * Returns the class simple name.
     */
    public String getClassSimpleName()
    {
        JavaDecl ct = getClassType();
        return ct != null ? ct.getSimpleName() : null;
    }

    /**
     * Returns the enclosing class this decl.
     */
    public JavaDecl getParent()
    {
        return _parent;
    }

    /**
     * Returns the parent name.
     */
    public String getParentName()
    {
        return _parent != null ? _parent.getName() : "";
    }

    /**
     * Returns the top level class name.
     */
    public String getRootClassName()
    {
        if (_parent != null && _parent.isClass())
            return _parent.getRootClassName();
        return getClassName();
    }

    /**
     * Returns whether class is member.
     */
    public boolean isMemberClass()
    {
        return isClass() && _parent != null && _parent.isClass();
    }

    /**
     * Returns the JavaDecl for class this decl evaluates to when referenced.
     */
    public JavaType getEvalType()  { return _evalType; }

    /**
     * Returns the type name for class this decl evaluates to when referenced.
     */
    public String getEvalTypeName()
    {
        return _evalType != null ? _evalType.getName() : null;
    }

    /**
     * Returns the type name for class this decl evaluates to when referenced.
     */
    public String getEvalClassName()
    {
        return _evalType != null ? _evalType.getClassName() : null;
    }

    /**
     * Returns the class this decl evaluates to when referenced.
     */
    public Class<?> getEvalClass()
    {
        String cname = getEvalClassName();
        if (cname == null) return null;
        return _resolver.getClassForName(cname);
    }

    /**
     * Returns the number of Method/ParamType parameters.
     */
    public int getParamCount()
    {
        return _paramTypes.length;
    }

    /**
     * Returns the individual Method parameter type at index.
     */
    public JavaType getParamType(int anIndex)
    {
        return _paramTypes[anIndex];
    }

    /**
     * Returns the parameter types.
     */
    public JavaType[] getParamTypes()  { return _paramTypes; }

    /**
     * Returns the parameter type names.
     */
    public String[] getParamTypeNames()
    {
        String[] names = new String[_paramTypes.length];
        for (int i = 0; i < names.length; i++) names[i] = _paramTypes[i].getName();
        return names;
    }

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
     * Returns the TypeVars.
     */
    public JavaDecl[] getTypeVars()  { return _typeVars; }

    /**
     * Returns the TypeVar with given name.
     */
    public JavaTypeVariable getTypeVar(String aName)
    {
        // Handle Method, Constructor: Get type for name from TypeVars
        if (isMethod() || isConstructor()) {

            // Check Method, Constructor TypeVars
            for (JavaTypeVariable tvar : _typeVars)
                if (tvar.getName().equals(aName))
                    return tvar;

            // Forward to class
            return _parent.getTypeVar(aName);
        }

        // Handle any other type: Complain
        else System.err.println("JavaDecl.getTypeVar: request for typevar from wrong type " + this);

        // Return null since named type var not found
        return null;
    }

    /**
     * Returns the package decl.
     */
    public JavaDecl getPackageDecl()
    {
        if (isPackage()) return this;
        if (_parent != null) return _parent.getPackageDecl();
        return null;
    }

    /**
     * Returns the package name.
     */
    public String getPackageName()
    {
        JavaDecl pd = getPackageDecl();
        return pd != null ? pd.getName() : null;
    }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaDecl getSuper()
    {
        // If already set, just return
        if (_superDecl != NULL_DECL) return _superDecl;

        // Get superclass and helper
        JavaClass cdecl = getClassType();
        JavaClass scdecl = cdecl != null ? cdecl.getSuper() : null;

        // Handle Method
        if (isMethod())
            return _superDecl = scdecl != null ? scdecl.getMethodDeclDeep(getName(), getParamTypes()) : null;

        // Handle Constructor
        if (isConstructor())
            return _superDecl = scdecl != null ? scdecl.getConstructorDeclDeep(getParamTypes()) : null;

        // Handle ParamType
        if (isParamType())
            return _superDecl = _parent;

        // Complain and return
        System.err.println("JavaDecl.getSuper: Invalid type " + this);
        return _superDecl = null;
    }

    /**
     * Returns common ancestor of this decl and given decls.
     */
    public JavaDecl getCommonAncestor(JavaDecl aDecl)
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
        for (JavaDecl d0 = this; d0 != null; d0 = d0.getSuper())
            for (JavaDecl d1 = aDecl; d1 != null; d1 = d1.getSuper())
                if (d0 == d1) return d0;

        // Return Object (case where at least one was interface or ParamType of interface)
        return getJavaDecl(Object.class);
    }

    /**
     * Returns common ancestor of this decl and given decls.
     */
    protected JavaDecl getCommonAncestorPrimitive(JavaDecl aDecl)
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
     * Returns whether given type is assignable to this JavaDecl.
     */
    public boolean isAssignable(JavaDecl aDecl)
    {
        return false;
    }

    /**
     * Returns whether is Type is explicit (doesn't contain any type variables).
     */
    public boolean isResolvedType()
    {
        if (isTypeVar()) return false;
        if (isParamType()) {
            if (getParent().isTypeVar()) return false;
            for (JavaDecl tv : getTypeVars())
                if (tv.isTypeVar())
                    return false;
        }
        return true;
    }

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
        if (!aDecl.isTypeVar())
            return (JavaType) aDecl;

        // Handle ParamType:
        if (isParamType()) {
            String name = aDecl.getName();
            JavaClass cls = getClassType();
            int ind = cls.getTypeVarIndex(name);
            if (ind >= 0 && ind < _paramTypes.length)
                return _paramTypes[ind];
        }

        // If not resolve, just return bounds type
        return aDecl.getEvalType();
    }

    /**
     * Returns a name suitable to describe declaration.
     */
    public String getPrettyName()
    {
        String name = getClassName();
        if (isMethod() || isField()) name += '.' + _name;
        if (isMethod() || isConstructor()) name += '(' + StringUtils.join(getParamTypeSimpleNames(), ",") + ')';
        if (isPackage()) return _name;
        if (isVarDecl()) return _name;
        return name;
    }

    /**
     * Returns a name unique for matching declarations.
     */
    public String getMatchName()
    {
        String name = getClassName();
        if (isMethod() || isField()) name += '.' + _name;
        if (isMethod() || isConstructor()) name += '(' + StringUtils.join(getParamTypeNames(), ",") + ')';
        if (isPackage()) return _name;
        if (isVarDecl()) return _name;
        return name;
    }

    /**
     * Returns the full name.
     */
    public String getFullName()
    {
        if (_fname != null) return _fname;
        String name = getMatchName();
        if (isMethod() || isField()) name = getEvalTypeName() + " " + name;
        String mstr = Modifier.toString(_mods);
        if (mstr.length() > 0) name = mstr + " " + name;
        return _fname = name;
    }

    String _fname;

    /**
     * Returns a string representation of suggestion.
     */
    public String getSuggestionString()
    {
        StringBuffer sb = new StringBuffer(getSimpleName());
        switch (getType()) {
            case Constructor:
            case Method:
                sb.append('(').append(StringUtils.join(getParamTypeSimpleNames(), ",")).append(')');
            case VarDecl:
            case Field:
                if (getEvalType() != null) sb.append(" : ").append(getEvalType().getSimpleName());
                if (getClassName() != null) sb.append(" - ").append(getClassSimpleName());
                break;
            case Class:
                sb.append(" - ").append(getParentName());
                break;
            case Package:
                break;
            default:
                throw new RuntimeException("Unsupported Type " + getType());
        }

        // Return string
        return sb.toString();
    }

    /**
     * Returns the string to use when inserting this suggestion into code.
     */
    public String getReplaceString()
    {
        switch (getType()) {
            case Class:
                return getSimpleName();
            case Constructor:
            case Method:
                return getName() + '(' + StringUtils.join(getParamTypeSimpleNames(), ",") + ')';
            case Package: {
                String name = getPackageName();
                int index = name.lastIndexOf('.');
                return index > 0 ? name.substring(index + 1) : name;
            }
            default:
                return getName();
        }
    }

    /**
     * Returns a JavaDecl for given object.
     */
    public JavaDecl getJavaDecl(Object anObj)
    {
        return _resolver.getJavaDecl(anObj);
    }

    /**
     * Returns a JavaDecl for given object.
     */
    public JavaType getJavaType(Type anObj)
    {
        return _resolver.getTypeDecl(anObj);
    }

    /**
     * Returns a JavaDecl for given object.
     */
    public JavaClass getClassDecl(Object anObj)
    {
        return _resolver.getJavaDeclClass(anObj);
    }

    /**
     * Returns a ParamType decl for this base class and given types ( This<typ,type>).
     */
    public JavaType getParamTypeDecl(JavaType ... theTypeDecls)
    {
        return _resolver.getParamTypeDecl(this, theTypeDecls);
    }

    /**
     * Returns the Array decl for this base class.
     */
    public JavaType getArrayTypeDecl()
    {
        // Handle ParamType or unexpected type: Return ClassType.getArrayTypeDecl()
        if (!isClass()) {
            if (!isParamType() && !isTypeVar())
                System.err.println("JavaDecl.getArrayTypeDecl: Unexpected type: " + this);
            return getClassType().getArrayTypeDecl();
        }

        // Append array chars to class name and get decl
        String className = getName() + "[]";
        return (JavaType) getJavaDecl(className);
    }

    /**
     * Returns whether given declaration collides with this declaration.
     */
    public boolean matches(JavaDecl aDecl)
    {
        // Check identity
        if (aDecl == this) return true;

        // Handle ParamTypes: Test against ClassType instead
        if (isParamType()) return getClassType().matches(aDecl);
        else if (aDecl.isParamType()) return matches(aDecl.getClassType());

        // If Types don't match, just return
        if (aDecl._type != _type) return false;

        // For Method, Constructor: Check supers
        if (isMethod() || isConstructor())
            for (JavaDecl sup = aDecl.getSuper(); sup != null; sup = sup.getSuper())
                if (sup == this)
                    return true;

        // Return false, since no match
        return false;
    }

    /**
     * Standard compareTo implementation.
     */
    public int compareTo(JavaDecl aDecl)
    {
        int t1 = _type.ordinal(), t2 = aDecl._type.ordinal();
        if (t1 < t2) return -1;
        if (t2 < t1) return 1;
        return getMatchName().compareTo(aDecl.getMatchName());
    }

    /**
     * Standard hashcode implementation.
     */
    public int hashCode()
    {
        return getId().hashCode();
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return _type + ": " + getId();
    }

}