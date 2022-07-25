/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.resolver.Resolver;
import snap.util.StringUtils;
import java.lang.reflect.*;

/**
 * This class represents a Java Method or Constructor.
 */
public class JavaExecutable extends JavaMember {

    // The JavaDecls for TypeVars for Method/Constructor
    protected JavaTypeVariable[]  _typeVars;

    // The JavaDecls for parameter types for Constructor, Method
    protected JavaType[]  _paramTypes;

    // Whether method has VarArgs
    protected boolean  _varArgs;

    /**
     * Constructor.
     */
    public JavaExecutable(Resolver anOwner, JavaDecl aPar, Member aMember)
    {
        super(anOwner, aPar, aMember);

        // Set mods, name, simple name
        _mods = aMember.getModifiers();
        _name = _simpleName = aMember.getName();
    }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaExecutable getSuper()  { return null; }

    /**
     * Returns the TypeVars.
     */
    public JavaTypeVariable[] getTypeVars()  { return _typeVars; }

    /**
     * Returns the TypeVar with given name.
     */
    public JavaTypeVariable getTypeVar(String aName)
    {
        // Check Method, Constructor TypeVars
        for (JavaTypeVariable tvar : _typeVars)
            if (tvar.getName().equals(aName))
                return tvar;

        // Forward to class
        JavaClass parentClass = getClassType();
        return parentClass.getTypeVar(aName);
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
     * Returns whether Method/Constructor is VarArgs type.
     */
    public boolean isVarArgs()
    {
        return _varArgs;
    }

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
     * Returns whether given declaration collides with this declaration.
     */
    @Override
    public boolean matches(JavaDecl aDecl)
    {
        // Check identity
        if (aDecl == this) return true;

        // If Types don't match, just return
        if (aDecl._type != _type)
            return false;

        // For Method, Constructor: Check supers
        JavaExecutable other = (JavaExecutable) aDecl;
        for (JavaExecutable sup = other.getSuper(); sup != null; sup = other.getSuper())
            if (sup == this)
                return true;

        // Return false, since no match
        return false;
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        StringBuffer sb = new StringBuffer(getSimpleName());
        String[] paramTypeNames = getParamTypeSimpleNames();
        sb.append('(').append(StringUtils.join(paramTypeNames, ",")).append(')');
        return sb.toString();
    }

    /**
     * Returns the string to use when inserting this suggestion into code.
     */
    @Override
    public String getReplaceString()
    {
        String name = getName();
        String[] paramTypeNames = getParamTypeSimpleNames();
        return name + '(' + StringUtils.join(getParamTypeSimpleNames(), ",") + ')';
    }

    /**
     * Returns a name suitable to describe declaration.
     */
    @Override
    public String getPrettyName()
    {
        String className = getClassName();
        String memberName = className;
        if (this instanceof JavaMethod)
            memberName = className + '.' + getName();
        String[] paramTypeNames = getParamTypeSimpleNames();
        return memberName + '(' + StringUtils.join(paramTypeNames, ",") + ')';
    }

    /**
     * Returns a name unique for matching declarations.
     */
    @Override
    public String getMatchName()
    {
        String className = getClassName();
        String memberName = className;
        if (this instanceof JavaMethod)
            memberName = className + '.' + getName();
        String[] paramTypeNames = getParamTypeNames();
        return memberName + '(' + StringUtils.join(paramTypeNames, ",") + ')';
    }
}
