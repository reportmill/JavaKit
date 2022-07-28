/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import snap.util.StringUtils;
import java.lang.reflect.*;

/**
 * A class to represent a declaration of a Java Class, Method, Field or Constructor.
 */
public class JavaDecl implements Comparable<JavaDecl> {

    // The Resolver that produced this decl
    protected Resolver  _resolver;

    // A unique identifier
    protected String  _id;

    // The type
    protected DeclType  _type;

    // The name of this declaration
    protected String  _name;

    // The full name of this declaration
    private String  _fullName;

    // The simple name of this declaration
    protected String  _simpleName;

    // The type this decl evaluates to when referenced
    protected JavaType  _evalType;

    // Constants for type
    public enum DeclType { Class, Field, Constructor, Method, Package, VarDecl, ParamType, TypeVar, GenArrayType }

    /**
     * Constructor.
     */
    protected JavaDecl(Resolver aResolver, DeclType aType)
    {
        // Set Resolver, Type
        _resolver = aResolver; assert (aResolver != null);
        _type = aType; assert (aType != null);
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
     * Field, Method, Constructor: DeclaringClass
     */
    public JavaClass getClassType()
    {
        // Handle JavaClass
        if (this instanceof JavaClass)
            return (JavaClass) this;

        // Handle JavaMember
        if (this instanceof JavaMember)
            return ((JavaMember) this).getDeclaringClass();

        // Anything else: Try EvalType.ClassType?
        JavaType evalType = getEvalType();
        return evalType != null ? evalType.getClassType() : null;
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
        String className = getEvalClassName();
        if (className == null) return null;
        return _resolver.getClassForName(className);
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
        // If already set, just return
        if (_fullName != null) return _fullName;

        // Get, set, return
        String fullName = getFullNameImpl();
        return _fullName = fullName;
    }

    /**
     * Returns the full name.
     */
    protected String getFullNameImpl()
    {
        return getMatchName();
    }

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
    public JavaClass getJavaClassForClass(Class<?> aClass)
    {
        return _resolver.getJavaClassForClass(aClass);
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
        // Compare type order
        int typeOrder1 = getType().ordinal();
        int typeOrder2 = aDecl.getType().ordinal();
        if (typeOrder1 != typeOrder2)
            return typeOrder1 - typeOrder2;

        // Compare match names
        String matchName1 = getMatchName();
        String matchName2 = aDecl.getMatchName();
        return matchName1.compareTo(matchName2);
    }

    /**
     * Standard hashcode implementation.
     */
    public int hashCode()
    {
        String id = getId();
        if (id == null)
            return System.identityHashCode(this);
        return id.hashCode();
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        String className = getClass().getSimpleName();
        String propStrings = toStringProps();
        return className + " { " + propStrings + " }";
    }

    /**
     * Standard toStringProps implementation.
     */
    public String toStringProps()
    {
        StringBuffer sb = new StringBuffer();
        StringUtils.appendProp(sb,"Id", getId());
        return sb.toString();
    }
}