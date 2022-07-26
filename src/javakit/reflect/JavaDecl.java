/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.*;

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

    // Constants for type
    public enum DeclType { Class, Field, Constructor, Method, Package, VarDecl, ParamType, TypeVar }

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
     * Returns whether given type is assignable to this JavaDecl.
     */
    public boolean isAssignable(JavaDecl aDecl)
    {
        return false;
    }

    /**
     * Returns a name suitable to describe declaration.
     */
    public String getPrettyName()
    {
        return getName();
    }

    /**
     * Returns a name unique for matching declarations.
     */
    public String getMatchName()
    {
        return getName();
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
        String simpleName = getSimpleName();
        return simpleName;
    }

    /**
     * Returns the string to use when inserting this suggestion into code.
     */
    public String getReplaceString()
    {
        String simpleName = getSimpleName();
        return simpleName;
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
    public JavaClass getJavaClass(Class aClass)
    {
        return _resolver.getJavaClass(aClass);
    }

    /**
     * Returns a ParamType decl for this base class and given types ( This<typ,type>).
     */
    public JavaType getParamTypeDecl(JavaType ... theTypeDecls)
    {
        return _resolver.getParamTypeDecl(this, theTypeDecls);
    }

    /**
     * Returns whether given declaration collides with this declaration.
     */
    public boolean matches(JavaDecl aDecl)
    {
        return aDecl == this;
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