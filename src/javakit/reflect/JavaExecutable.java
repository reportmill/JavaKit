/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
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
    public JavaExecutable(Resolver anOwner, JavaClass aDeclaringClass, Member aMember)
    {
        super(anOwner, aDeclaringClass, aMember);

        // Get VarArgs
        _varArgs = isVarArgs(aMember);

        // Get TypeVariables
        TypeVariable<?>[] typeVars = getTypeParameters(aMember);
        _typeVars = new JavaTypeVariable[typeVars.length];
        for (int i = 0, iMax = typeVars.length; i < iMax; i++)
            _typeVars[i] = new JavaTypeVariable(_resolver, this, typeVars[i]);
    }

    /**
     * Resolves types.
     */
    protected void initTypes(Member aMember)
    {
        // Get ParameterTypes
        Type[] paramTypes = getGenericParameterTypes(aMember);
        _paramTypes = _resolver.getJavaTypeArrayForTypes(paramTypes);
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
    public JavaTypeVariable getTypeVarForName(String aName)
    {
        // Check Method, Constructor TypeVars
        for (JavaTypeVariable typeVar : _typeVars)
            if (typeVar.getName().equals(aName))
                return typeVar;

        // Forward to class
        JavaClass parentClass = getClassType();
        return parentClass.getTypeVarForName(aName);
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
        // Get SimpleName,
        String simpleName = getSimpleName();
        String[] paramTypeNames = getParamTypeSimpleNames();
        String paramTypeNamesStr = StringUtils.join(paramTypeNames, ",");

        // Construct string SimpleName(ParamType.SimpleName, ...)
        return simpleName + '(' + paramTypeNamesStr + ')';
    }

    /**
     * Returns the string to use when inserting this suggestion into code.
     */
    @Override
    public String getReplaceString()
    {
        String name = getName();
        String[] paramTypeNames = getParamTypeSimpleNames();
        String paramTypeNamesStr = StringUtils.join(paramTypeNames, ",");
        return name + '(' + paramTypeNamesStr + ')';
    }

    /**
     * Returns a name suitable to describe declaration.
     */
    @Override
    public String getPrettyName()
    {
        String className = getDeclaringClassName();
        String memberName = className;
        if (this instanceof JavaMethod)
            memberName = className + '.' + getName();
        String[] paramTypeNames = getParamTypeSimpleNames();
        String paramTypeNamesStr = StringUtils.join(paramTypeNames, ",");
        return memberName + '(' + paramTypeNamesStr + ')';
    }

    /**
     * Returns a name unique for matching declarations.
     */
    @Override
    public String getMatchName()
    {
        String className = getDeclaringClassName();
        String memberName = className;
        if (this instanceof JavaMethod)
            memberName = className + '.' + getName();
        String[] paramTypeNames = getParamTypeNames();
        String paramTypeNamesStr = StringUtils.join(paramTypeNames, ",");
        return memberName + '(' + paramTypeNamesStr + ')';
    }

    /**
     * Returns a rating of a method for given possible arg classes.
     */
    public static int getMatchRatingForTypes(JavaExecutable aMethod, JavaType[] theTypes)
    {
        // Handle VarArg methods special
        if (aMethod.isVarArgs())
            return getMatchRatingForTypesWidthVarArgs(aMethod, theTypes);

        // Get method param types and length (just return if given arg count doesn't match)
        JavaType[] paramTypes = aMethod.getParamTypes();
        int plen = paramTypes.length, rating = 0;
        if (theTypes.length != plen)
            return 0;
        if (plen == 0)
            return 1000;

        // Iterate over classes and add score based on matching classes
        // This is a punt - need to groc the docs on this: https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html
        for (int i = 0, iMax = plen; i < iMax; i++) {
            JavaType cls1 = paramTypes[i].getClassType();
            JavaType cls2 = theTypes[i];
            if (cls2 != null)
                cls2 = cls2.getClassType();
            if (!cls1.isAssignable(cls2))
                return 0;
            rating += cls1 == cls2 ? 1000 : cls2 != null ? 100 : 10;
        }

        // Return rating
        return rating;
    }

    /**
     * Returns a rating of a method for given possible arg classes.
     */
    private static int getMatchRatingForTypesWidthVarArgs(JavaExecutable aMethod, JavaType[] theTypes)
    {
        // Get method param types and length (just return if given arg count is insufficient)
        JavaType[] paramTypes = aMethod.getParamTypes();
        int plen = paramTypes.length, vind = plen - 1, rating = 0;
        if (theTypes.length < vind)
            return 0;
        if (plen == 1 && theTypes.length == 0)
            return 10;

        // Iterate over classes and add score based on matching classes
        // This is a punt - need to groc the docs on this: https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html
        for (int i = 0, iMax = vind; i < iMax; i++) {
            JavaDecl cls1 = paramTypes[i].getClassType(), cls2 = theTypes[i];
            if (cls2 != null) cls2 = cls2.getClassType();
            if (!cls1.isAssignable(cls2))
                return 0;
            rating += cls1 == cls2 ? 1000 : cls2 != null ? 100 : 10;
        }

        // Get VarArg type
        JavaType varArgArrayType = paramTypes[vind];
        JavaType varArgType = varArgArrayType.getArrayItemType();

        // If only one arg and it is of array type, add 1000
        JavaType argType = theTypes.length == plen ? theTypes[vind] : null;
        if (argType != null && argType.isArray() && varArgArrayType.isAssignable(argType))
            rating += 1000;

            // If any var args match, add 1000
        else for (int i = vind; i < theTypes.length; i++) {
            JavaDecl type = theTypes[i];
            if (varArgType.isAssignable(type))
                rating += 1000;
        }

        // Return rating
        return rating;
    }

    /**
     * Returns whether is VarArgs.
     */
    private static boolean isVarArgs(Member aMember)
    {
        if (aMember instanceof Method)
            return ((Method) aMember).isVarArgs();
        return ((Constructor<?>) aMember).isVarArgs();
    }

    /**
     * Returns TypeVariables.
     */
    private static TypeVariable<?>[] getTypeParameters(Member aMember)
    {
        if (aMember instanceof Method)
            return ((Method) aMember).getTypeParameters();
        return ((Constructor<?>) aMember).getTypeParameters();
    }

    /**
     * Returns ParameterTypes.
     */
    private static Type[] getGenericParameterTypes(Member aMember)
    {
        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        if (aMember instanceof Method) {
            Method method = (Method) aMember;
            Type[] paramTypes = method.getGenericParameterTypes();
            if (paramTypes.length < method.getParameterCount())
                paramTypes = method.getParameterTypes();
            return paramTypes;
        }

        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Constructor<?> constructor = (Constructor<?>) aMember;
        Type[] paramTypes = constructor.getGenericParameterTypes();
        if (paramTypes.length < constructor.getParameterCount())
            paramTypes = constructor.getParameterTypes();
        return paramTypes;
    }
}
