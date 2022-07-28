/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.*;
import java.util.*;
import snap.util.StringUtils;

/**
 * This JavaType subclass represents a java.lang.Class.
 */
public class JavaClass extends JavaType {

    // The package (if root level class)
    private JavaPackage  _package;

    // The Declaring class (if member of enclosing class)
    private JavaClass  _declaringClass;

    // The super class type (could be ParameterizedType)
    private JavaType  _superType;

    // The super class
    private JavaClass  _superClass;

    // The modifiers
    protected int  _mods;

    // Whether class decl is enum, interface, primitive
    private boolean  _enum, _interface, _primitive;

    // The array of interfaces
    protected JavaClass[]  _interfaces;

    // The field decls
    protected List<JavaField>  _fieldDecls;

    // The method decls
    protected List<JavaMethod>  _methDecls = new ArrayList<>();

    // The constructor decls
    protected List<JavaConstructor>  _constrDecls = new ArrayList<>();

    // The inner class decls
    protected List<JavaClass>  _innerClasses = new ArrayList<>();

    // The type var decls
    protected List<JavaTypeVariable>  _typeVarDecls = new ArrayList<>();

    // The Array item type (if Array)
    private JavaType  _arrayItemType;

    // The updater
    private JavaClassUpdater  _updater;

    /**
     * Constructor.
     */
    public JavaClass(Resolver aResolver, JavaDecl aPar, Class<?> aClass)
    {
        // Do normal version
        super(aResolver, DeclType.Class);

        // Set Id, Name, SimpleName
        _id = _name = ResolverUtils.getIdForClass(aClass);
        _simpleName = aClass.getSimpleName();

        // Set DeclaringClass or Package
        if (aPar instanceof JavaClass)
            _declaringClass = (JavaClass) aPar;
        else if (aPar instanceof JavaPackage)
            _package = (JavaPackage) aPar;

        // Add to decls
        aResolver._decls.put(_id, this);
        if (aClass.isArray()) {
            String altName = aClass.getName();
            if (!altName.equals(_id))
                aResolver._decls.put(altName, this);
        }

        // Set Mods, Enum, Interface, Primitive
        _mods = aClass.getModifiers();
        _enum = aClass.isEnum();
        _interface = aClass.isInterface();
        _primitive = aClass.isPrimitive();

        // Set EvalType to this
        _evalType = this;

        // Create/set updater
        _updater = new JavaClassUpdater(this);

        // Get type super type and set in decl
        Type superType = aClass.getGenericSuperclass();
        if (superType != null) {
            _superType = _resolver.getJavaTypeForType(superType);
            _superClass = _superType.getEvalClass();
        }

        // Handle Array
        if (aClass.isArray()) {

            // Set ArrayItemType
            Class<?> compClass = aClass.getComponentType();
            _arrayItemType = getJavaClassForClass(compClass);

            // Set Decls from Object[] for efficiency
            if (aClass != Object[].class) {
                JavaClass aryDecl = getJavaClassForClass(Object[].class);
                _fieldDecls = aryDecl.getFields();
                _interfaces = aryDecl._interfaces;
                _methDecls = aryDecl._methDecls;
                _constrDecls = aryDecl._constrDecls;
                _innerClasses = aryDecl._innerClasses;
                _typeVarDecls = aryDecl._typeVarDecls;
            }
        }
    }

    /**
     * Returns the package that declares this class.
     */
    public JavaPackage getPackage()  { return _package; }

    /**
     * Returns the class that contains this class (if inner class).
     */
    public JavaClass getDeclaringClass()  { return _declaringClass; }

    /**
     * Returns the class name.
     */
    @Override
    public String getClassName()  { return _name; }

    /**
     * Returns the top level class name.
     */
    public String getRootClassName()
    {
        if (_declaringClass != null)
            return _declaringClass.getRootClassName();
        return getClassName();
    }

    /**
     * Override to return as Class type.
     */
    public JavaType getSuperType()
    {
        return _superType;
    }

    /**
     * Override to return as Class type.
     */
    public JavaClass getSuperClass()
    {
        return _superClass;
    }

    /**
     * Returns the class this decl evaluates to when referenced.
     */
    public Class<?> getRealClass()
    {
        String className = getClassName();
        Class<?> realClass = className != null ? _resolver.getClassForName(className) : null;
        if (realClass == null)
            System.err.println("JavaClass.getRealClass: Couldn't find real class for name: " + className);
        return realClass;
    }

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
     * Returns whether class is member.
     */
    public boolean isMemberClass()  { return _declaringClass != null; }

    /**
     * Returns whether is a enum reference.
     */
    public boolean isEnum()  { return _enum; }

    /**
     * Returns whether is a interface reference.
     */
    public boolean isInterface()  { return _interface; }

    /**
     * Returns whether is an array.
     */
    public boolean isArray()  { return _arrayItemType != null; }

    /**
     * Returns the Array item type (if Array).
     */
    public JavaType getArrayItemType()
    {
        return _arrayItemType;
    }

    /**
     * Returns whether is primitive.
     */
    public boolean isPrimitive()
    {
        return _primitive;
    }

    /**
     * Returns the interfaces this class implments.
     */
    public JavaClass[] getInterfaces()
    {
        getFields();
        return _interfaces;
    }

    /**
     * Returns the fields.
     */
    public List<JavaField> getFields()
    {
        if (_fieldDecls == null) updateDecls();
        return _fieldDecls;
    }

    /**
     * Returns the methods.
     */
    public List<JavaMethod> getMethods()
    {
        getFields();
        return _methDecls;
    }

    /**
     * Returns the Constructors.
     */
    public List<JavaConstructor> getConstructors()
    {
        getFields();
        return _constrDecls;
    }

    /**
     * Returns the inner classes.
     */
    public List<JavaClass> getInnerClasses()
    {
        getFields();
        return _innerClasses;
    }

    /**
     * Returns the inner classes.
     */
    public List<JavaTypeVariable> getTypeVars()
    {
        getFields();
        return _typeVarDecls;
    }

    /**
     * Returns a field decl for field name.
     */
    public JavaField getFieldForName(String aName)
    {
        List<JavaField> fields = getFields();
        for (JavaField field : fields)
            if (field.getName().equals(aName))
                return field;

        // Return
        return null;
    }

    /**
     * Returns a field decl for field name.
     */
    public JavaField getFieldDeepForName(String aName)
    {
        JavaField field = getFieldForName(aName);
        if (field == null && _superClass != null)
            field = _superClass.getFieldDeepForName(aName);

        // Return
        return field;
    }

    /**
     * Returns a constructor decl for parameter types.
     */
    public JavaConstructor getConstructorForTypes(JavaType[] theTypes)
    {
        List<JavaConstructor> constructors = getConstructors();
        for (JavaConstructor constructor : constructors) {
            JavaType[] constrParamTypes = constructor.getParamTypes();
            if (isTypesEqual(constrParamTypes, theTypes))
                return constructor;
        }

        // Return
        return null;
    }

    /**
     * Returns a constructor decl for parameter types.
     */
    public JavaConstructor getConstructorDeepForTypes(JavaType[] theTypes)
    {
        JavaConstructor decl = getConstructorForTypes(theTypes);
        if (decl == null && _superClass != null)
            decl = _superClass.getConstructorDeepForTypes(theTypes);
        return decl;
    }

    /**
     * Returns a method decl for method name and parameter types.
     */
    public JavaMethod getMethodForNameAndTypes(String aName, JavaType[] theTypes)
    {
        List<JavaMethod> methods = getMethods();
        for (JavaMethod method : methods) {
            if (method.getName().equals(aName)) {
                JavaType[] methodParamTypes = method.getParamTypes();
                if (isTypesEqual(methodParamTypes, theTypes))
                    return method;
            }
        }

        // Return
        return null;
    }

    /**
     * Returns a method decl for method name and parameter types.
     */
    public JavaMethod getMethodDeepForNameAndTypes(String aName, JavaType[] theTypes)
    {
        JavaMethod method = getMethodForNameAndTypes(aName, theTypes);
        if (method == null && _superClass != null)
            method = _superClass.getMethodDeepForNameAndTypes(aName, theTypes);
        return method;
    }

    /**
     * Returns a Class decl for inner class simple name.
     */
    public JavaClass getInnerClassForName(String aName)
    {
        List<JavaClass> icdecls = getInnerClasses();
        for (JavaClass jd : icdecls)
            if (jd.getSimpleName().equals(aName))
                return jd;
        return null;
    }

    /**
     * Returns a Class decl for inner class name.
     */
    public JavaClass getInnerClassDeepForName(String aName)
    {
        JavaClass decl = getInnerClassForName(aName);
        if (decl == null && _superClass != null)
            decl = _superClass.getInnerClassDeepForName(aName);
        return decl;
    }

    /**
     * Returns a TypeVar decl for inner class name.
     */
    public JavaTypeVariable getTypeVarForName(String aName)
    {
        // Iterate to find TypeVar for name
        List<JavaTypeVariable> typeVars = getTypeVars();
        for (JavaTypeVariable typeVar : typeVars)
            if (typeVar.getName().equals(aName))
                return typeVar;

        // Return not found
        return null;
    }

    /**
     * Returns a TypeVar decl for inner class name.
     */
    public int getTypeVarIndexForName(String aName)
    {
        // Iterate to find TypeVar index for name
        List<JavaTypeVariable> typeVars = getTypeVars();
        for (int i = 0, iMax = typeVars.size(); i < iMax; i++) {
            JavaTypeVariable typeVar = typeVars.get(i);
            if (typeVar.getName().equals(aName))
                return i;
        }

        // Return not found
        return -1;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public List<JavaField> getPrefixFields(String aPrefix)
    {
        // Create return list of prefix fields
        List<JavaField> fieldsWithPrefix = new ArrayList<>();

        // Iterate over classes
        for (JavaClass cls = this; cls != null; cls = cls.getSuperClass()) {

            // Get Class fields
            List<JavaField> fields = cls.getFields();
            for (JavaField field : fields)
                if (StringUtils.startsWithIC(field.getName(), aPrefix))
                    fieldsWithPrefix.add(field);

            // Should iterate over class interfaces, too
        }

        // Return list of prefix fields
        return fieldsWithPrefix;
    }

    /**
     * Returns methods that match given prefix.
     */
    public List<JavaMethod> getPrefixMethods(String aPrefix)
    {
        // Create return list of prefix methods
        List<JavaMethod> methodsWithPrefix = new ArrayList<>();

        // Iterate over classes
        for (JavaClass cls = this; cls != null; cls = cls.getSuperClass()) {

            // Get Class methods
            List<JavaMethod> methods = cls.getMethods();
            for (JavaMethod method : methods)
                if (StringUtils.startsWithIC(method.getName(), aPrefix))
                    methodsWithPrefix.add(method);

            // If interface, iterate over class interfaces, too (should probably do this anyway to catch default methods).
            if (cls.isInterface()) {
                for (JavaClass c2 : cls.getInterfaces()) {
                    List<JavaMethod> pmeths2 = c2.getPrefixMethods(aPrefix);
                    methodsWithPrefix.addAll(pmeths2);
                }
            }
        }

        // Return list of prefix methods
        return methodsWithPrefix;
    }

    /**
     * Returns a compatible constructor for given name and param types.
     */
    public JavaConstructor getCompatibleConstructor(JavaType[] theTypes)
    {
        List<JavaConstructor> constructors = getConstructors();
        JavaConstructor constructor = null;
        int rating = 0;

        // Iterate over constructors to find highest rating
        for (JavaConstructor constr : constructors) {
            int rtg = JavaExecutable.getMatchRatingForTypes(constr, theTypes);
            if (rtg > rating) {
                constructor = constr;
                rating = rtg;
            }
        }

        // Return
        return constructor;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public JavaMethod getCompatibleMethod(String aName, JavaType[] theTypes)
    {
        List<JavaMethod> methods = getMethods();
        JavaMethod method = null;
        int rating = 0;

        // Iterate over methods to find highest rating
        for (JavaMethod meth : methods) {
            if (meth.getName().equals(aName)) {
                int rtg = JavaExecutable.getMatchRatingForTypes(meth, theTypes);
                if (rtg > rating) {
                    method = meth;
                    rating = rtg;
                }
            }
        }

        // Return
        return method;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public JavaMethod getCompatibleMethodDeep(String aName, JavaType[] theTypes)
    {
        // Search this class and superclasses for compatible method
        for (JavaClass cls = this; cls != null; cls = cls.getSuperClass()) {
            JavaMethod decl = cls.getCompatibleMethod(aName, theTypes);
            if (decl != null)
                return decl;
        }
        return null;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public JavaMethod getCompatibleMethodAll(String aName, JavaType[] theTypes)
    {
        // Search this class and superclasses for compatible method
        JavaMethod decl = getCompatibleMethodDeep(aName, theTypes);
        if (decl != null)
            return decl;

        // Search this class and superclasses for compatible interface
        for (JavaClass cls = this; cls != null; cls = cls.getSuperClass()) {
            for (JavaClass infc : cls.getInterfaces()) {
                decl = infc.getCompatibleMethodAll(aName, theTypes);
                if (decl != null)
                    return decl;
            }
        }

        // If this class is Interface, check Object
        if (isInterface()) {
            JavaClass objDecl = getJavaClassForClass(Object.class);
            return objDecl.getCompatibleMethodDeep(aName, theTypes);
        }

        // Return null since compatible method not found
        return null;
    }

    /**
     * Returns a compatible methods for given name and param types.
     */
    public List<JavaMethod> getCompatibleMethods(String aName, JavaType[] theTypes)
    {
        List<JavaMethod> matches = Collections.EMPTY_LIST;
        List<JavaMethod> methods = getMethods();

        // Iterate over methods to find highest rating
        for (JavaMethod method : methods) {
            if (method.getName().equals(aName)) {
                int rtg = JavaExecutable.getMatchRatingForTypes(method, theTypes);
                if (rtg > 0) {
                    if (matches == Collections.EMPTY_LIST)
                        matches = new ArrayList<>();
                    matches.add(method);
                }
            }
        }

        // Return
        return matches;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public List<JavaMethod> getCompatibleMethodsDeep(String aName, JavaType[] theTypes)
    {
        // Search this class and superclasses for compatible method
        List<JavaMethod> matches = Collections.EMPTY_LIST;

        // Iterate over this class and parents
        for (JavaClass cls = this; cls != null; cls = cls.getSuperClass()) {
            List<JavaMethod> decls = cls.getCompatibleMethods(aName, theTypes);
            if (decls.size() > 0) {
                if (matches == Collections.EMPTY_LIST)
                    matches = decls;
                else matches.addAll(decls);
            }
        }

        // Return
        return matches;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public List<JavaMethod> getCompatibleMethodsAll(String aName, JavaType[] theTypes)
    {
        // Search this class and superclasses for compatible method
        List<JavaMethod> matches = Collections.EMPTY_LIST;
        List<JavaMethod> methods = getCompatibleMethodsDeep(aName, theTypes);
        if (methods.size() > 0)
            matches = methods;

        // Search this class and superclasses for compatible interface
        for (JavaClass cls = this; cls != null; cls = cls.getSuperClass()) {
            for (JavaClass infc : cls.getInterfaces()) {
                methods = infc.getCompatibleMethodsAll(aName, theTypes);
                if (methods.size() > 0) {
                    if (matches == Collections.EMPTY_LIST) matches = methods;
                    else matches.addAll(methods);
                }
            }
        }

        // If this class is Interface, check Object
        if (isInterface()) {
            JavaClass objDecl = getJavaClassForClass(Object.class);
            methods = objDecl.getCompatibleMethodsDeep(aName, theTypes);
            if (methods.size() > 0) {
                if (matches == Collections.EMPTY_LIST) matches = methods;
                else matches.addAll(methods);
            }
        }

        // Remove supers and duplicates
        for (int i = 0; i < matches.size(); i++) {
            JavaMethod method = matches.get(i);
            for (JavaMethod superMethod = method.getSuper(); superMethod != null; superMethod = superMethod.getSuper())
                matches.remove(superMethod);
            for (int j = i + 1; j < matches.size(); j++)
                if (matches.get(j) == method)
                    matches.remove(j);
        }

        // Return null since compatible method not found
        return matches;
    }

    /**
     * Returns the lambda method.
     */
    public JavaMethod getLambdaMethod(int argCount)
    {
        List<JavaMethod> methods = getMethods();
        for (JavaMethod method : methods)
            if (method.getParamCount() == argCount)
                return method;
        return null;
    }

    /**
     * Returns the primitive counter part, if available.
     */
    public JavaClass getPrimitive()
    {
        if (isPrimitive())
            return this;

        // Handle primitive types
        switch (_name) {
            case "java.lang.Boolean": return getJavaClassForClass(boolean.class);
            case "java.lang.Byte": return getJavaClassForClass(byte.class);
            case "java.lang.Character": return getJavaClassForClass(char.class);
            case "java.lang.Short": return getJavaClassForClass(short.class);
            case "java.lang.Integer": return getJavaClassForClass(int.class);
            case "java.lang.Long": return getJavaClassForClass(long.class);
            case "java.lang.Float": return getJavaClassForClass(float.class);
            case "java.lang.Double": return getJavaClassForClass(double.class);
            case "java.lang.Void": return getJavaClassForClass(void.class);
            default: return null;
        }
    }

    /**
     * Returns the primitive counter part, if available.
     */
    public JavaClass getPrimitiveAlt()
    {
        if (!isPrimitive())
            return this;

        // Handle primitive types
        switch (_name) {
            case "boolean": return getJavaClassForClass(Boolean.class);
            case "byte": return getJavaClassForClass(Byte.class);
            case "char": return getJavaClassForClass(Character.class);
            case "short": return getJavaClassForClass(Short.class);
            case "int": return getJavaClassForClass(Integer.class);
            case "long": return getJavaClassForClass(Long.class);
            case "float": return getJavaClassForClass(Float.class);
            case "double": return getJavaClassForClass(Double.class);
            case "void": return getJavaClassForClass(Void.class);
            default: return null;
        }
    }

    /**
     * Returns whether given type is assignable to this JavaDecl.
     */
    @Override
    public boolean isAssignable(JavaType aType)
    {
        // If this decl is primitive, forward to primitive version
        if (isPrimitive() && aType instanceof JavaClass)
            return isAssignablePrimitive(aType);

        // If given val is null or this decl is Object return true
        if (aType == null)
            return true;
        if (getName().equals("java.lang.Object"))
            return true;
        JavaClass otherClass = aType.getEvalClass();
        if (otherClass.isPrimitive())
            otherClass = otherClass.getPrimitiveAlt();

        // If either are array type, check ArrayItemTypes if both are (otherwise return false)
        if (isArray() || otherClass.isArray()) {
            if (isArray() && otherClass.isArray())
                return getArrayItemType().isAssignable(otherClass.getArrayItemType());
            return false;
        }

        // Iterate up given class superclasses and check class and interfaces
        for (JavaClass cls = otherClass; cls != null; cls = cls.getSuperClass()) {

            // If classes match, return true
            if (cls == this)
                return true;

            // If any interface of this decl match, return true
            if (isInterface()) {
                for (JavaClass infc : cls.getInterfaces())
                    if (isAssignable(infc))
                        return true;
            }
        }

        // Return false since no match found
        return false;
    }

    /**
     * Returns whether given type is assignable to this JavaDecl.
     */
    private boolean isAssignablePrimitive(JavaType otherType)
    {
        if (otherType == null)
            return false;
        JavaClass otherClass = otherType.getEvalClass();
        JavaClass otherPrimitive = otherClass.getPrimitive();
        if (otherPrimitive == null)
            return false;
        JavaDecl common = getCommonAncestorPrimitive(otherPrimitive);
        return common == this;
    }

    /**
     * Returns a resolved type for given unresolved type (TypeVar or ParamType<TypeVar>), if this decl can resolve it.
     */
    @Override
    public JavaType getResolvedType(JavaType aType)
    {
        // Handle ParamType and anything not a TypeVar
        if (aType instanceof JavaParameterizedType) {
            System.err.println("JavaDecl.getResolvedType: ParamType not yet supported");
            return aType;
        }

        // If not TypeVariable, we shouldn't be here
        if (!(aType instanceof JavaTypeVariable))
            return aType;

        // If has type var, return bounds type
        String name = aType.getName();
        JavaDecl typeVar = getTypeVarForName(name);
        if (typeVar != null)
            return typeVar.getEvalType();

        // If super has type var, return mapped type //JavaDecl sdecl = getSuper();
        /*if(sdecl!=null && sdecl.getTypeVar(name)!=null) {
            int ind = sdecl.getHpr().getTypeVarDeclIndex(name);
            if(ind>=0 && ind<_paramTypes.length) return _paramTypes[ind]; }*/

        // If SuerType is ParameterizedType, let it try to resolve
        if (_superType instanceof JavaParameterizedType)
            return _superType.getResolvedType(aType);

        // Otherwise just return EvalType
        JavaType evalType = aType.getEvalType();
        return evalType;
    }

    /**
     * Returns the full name.
     */
    @Override
    protected String getFullNameImpl()
    {
        // Get Match name
        String name = getMatchName();

        // Add mod string
        String modifierStr = Modifier.toString(_mods);
        if (modifierStr.length() > 0)
            name = modifierStr + " " + name;

        // Return
        return name;
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        String simpleName = getSimpleName();
        if (_declaringClass != null)
            return simpleName + " - " + _declaringClass.getName();
        if (_package != null)
            return simpleName + " - " + _package.getName();
        return simpleName;
    }

    /**
     * Returns the updater.
     */
    public JavaClassUpdater getUpdater()  { return _updater; }

    /**
     * Updates JavaDecls. Returns whether the decls changed since last update.
     */
    public boolean updateDecls()
    {
        return _updater.updateDecls();
    }

    /**
     * Returns the list of all decls.
     */
    public List<JavaDecl> getAllDecls()
    {
        return _updater.getAllDecls();
    }
}