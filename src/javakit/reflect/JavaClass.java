/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import java.lang.reflect.*;
import java.util.*;
import snap.util.StringUtils;

/**
 * A subclass of JavaDecl especially for Class declarations.
 */
public class JavaClass extends JavaType {

    // The Declaring class
    private JavaClass  _declaringClass;

    // The package
    private JavaPackage  _package;

    // The modifiers
    private int  _mods;

    // The super class decl
    private JavaClass  _superClass;

    // Whether class decl is enum, interface, primitive
    private boolean  _enum, _interface, _primitive;

    // The array of interfaces
    private JavaClass[]  _interfaces;

    // The field decls
    private List<JavaField>  _fieldDecls;

    // The method decls
    private List<JavaMethod>  _methDecls = new ArrayList<>();

    // The constructor decls
    private List<JavaConstructor>  _constrDecls = new ArrayList<>();

    // The inner class decls
    private List<JavaClass>  _innerClasses = new ArrayList<>();

    // The type var decls
    private List<JavaTypeVariable>  _typeVarDecls = new ArrayList<>();

    // A cached list of all decls
    private List<JavaDecl> _allDecls;

    // The Array item type (if Array)
    private JavaType  _arrayItemType;

    /**
     * Creates a new JavaDeclClass for given owner, parent and Class.
     */
    public JavaClass(Resolver aResolver, JavaDecl aPar, Class<?> aClass)
    {
        // Do normal version
        super(aResolver, DeclType.Class);

        // Set id
        _id = _name = ResolverUtils.getIdForClass(aClass);

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

        // Set class attributes
        _mods = aClass.getModifiers();
        _simpleName = aClass.getSimpleName();
        _enum = aClass.isEnum();
        _interface = aClass.isInterface();
        _primitive = aClass.isPrimitive();

        // Set EvalType to this
        _evalType = this;

        // Get type super type and set in decl
        AnnotatedType superAType = aClass.getAnnotatedSuperclass();
        Type superType = superAType != null ? superAType.getType() : null;
        if (superType != null) {
            _superType = getJavaType(superType);
            _superClass = _superType.getClassType();
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
                _allDecls = aryDecl.getAllDecls();
            }
        }
    }

    /**
     * Returns the class that contains this class (if inner class).
     */
    public JavaClass getDeclaringClass()  { return _declaringClass; }

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
     * Returns the package that declares this class.
     */
    public JavaPackage getPackage()  { return _package; }

    /**
     * Returns whether class is member.
     */
    public boolean isMemberClass()  { return _declaringClass != null; }

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
        if (isPrimitive())
            return isAssignablePrimitive(aType);

        // If given val is null or this decl is Object return true
        if (aType == null)
            return true;
        if (getName().equals("java.lang.Object"))
            return true;
        JavaClass ctype1 = aType.getClassType();
        if (ctype1.isPrimitive())
            ctype1 = ctype1.getPrimitiveAlt();

        // If either are array type, check ArrayItemTypes if both are (otherwise return false)
        if (isArray() || ctype1.isArray()) {
            if (isArray() && ctype1.isArray())
                return getArrayItemType().isAssignable(ctype1.getArrayItemType());
            return false;
        }

        // Iterate up given class superclasses and check class and interfaces
        for (JavaClass ct1 = ctype1; ct1 != null; ct1 = ct1.getSuper()) {

            // If classes match, return true
            if (ct1 == this)
                return true;

            // If any interface of this decl match, return true
            if (isInterface()) {
                for (JavaClass infc : ct1.getInterfaces())
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
    private boolean isAssignablePrimitive(JavaDecl aDecl)
    {
        if (aDecl == null) return false;
        JavaClass ctype0 = getClassType();
        JavaClass ctype1 = aDecl.getClassType().getPrimitive();
        if (ctype1 == null)
            return false;
        JavaDecl common = getCommonAncestorPrimitive(ctype1);
        return common == this;
    }

    /**
     * Override to return as Class type.
     */
    public JavaClass getSuper()
    {
        return _superClass;
    }

    /**
     * Returns a resolved type for given unresolved type (TypeVar or ParamType<TypeVar>), if this decl can resolve it.
     */
    public JavaType getResolvedType(JavaDecl aDecl)
    {
        // Handle ParamType and anything not a TypeVar
        if (aDecl instanceof JavaParameterizedType) {
            System.err.println("JavaDecl.getResolvedType: ParamType not yet supported");
            return (JavaParameterizedType) aDecl;
        }

        // If not TypeVariable, we shouldn't be here
        if (!(aDecl instanceof JavaTypeVariable))
            return (JavaType) aDecl;

        // If has type var, return bounds type
        String name = aDecl.getName();
        JavaDecl typeVar = getTypeVarForName(name);
        if (typeVar != null)
            return typeVar.getEvalType();

        // If super has type var, return mapped type //JavaDecl sdecl = getSuper();
        /*if(sdecl!=null && sdecl.getTypeVar(name)!=null) {
            int ind = sdecl.getHpr().getTypeVarDeclIndex(name);
            if(ind>=0 && ind<_paramTypes.length) return _paramTypes[ind]; }*/

        // If SuerType is ParameterizedType, let it try to resolve
        if (_superType instanceof JavaParameterizedType)
            return _superType.getResolvedType(aDecl);

        // Otherwise just return EvalType
        JavaType evalType = aDecl.getEvalType();
        return evalType;
    }

    /**
     * Updates JavaDecls.
     *
     * @return whether the decls changed since last update.
     */
    public boolean updateDecls()
    {
        // If first time, set decls
        if (_fieldDecls == null)
            _fieldDecls = new ArrayList<>();

        // Get eval class
        Class evalClass = getEvalClass();
        String className = getClassName();
        if (evalClass == null) {
            System.err.println("JavaDeclClass: Failed to load class: " + className);
            return false;
        }

        // Get interfaces
        Class[] interfaces = evalClass.getInterfaces();
        _interfaces = new JavaClass[interfaces.length];
        for (int i = 0, iMax = interfaces.length; i < iMax; i++) {
            Class intrface = interfaces[i];
            _interfaces[i] = getJavaClassForClass(intrface);
        }

        // Create set for added/removed decls
        int addedDecls = 0;
        HashSet<JavaDecl> removedDecls = new HashSet<>(getAllDecls());

        // Make sure class decl is up to date
        if (getModifiers() != evalClass.getModifiers())
            _mods = evalClass.getModifiers();

        // Get TypeVariables
        TypeVariable[] typeVariables;
        try { typeVariables = evalClass.getTypeParameters(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each Type parameter
        for (TypeVariable typeVariable : typeVariables) {
            String name = typeVariable.getName();
            JavaDecl decl = getTypeVarForName(name);
            if (decl == null) {
                decl = new JavaTypeVariable(_resolver, this, typeVariable);
                addDecl(decl);
                addedDecls++;
            } else removedDecls.remove(decl);
        }

        // Get Inner Classes
        Class<?>[] innerClasses;
        try { innerClasses = evalClass.getDeclaredClasses(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each inner class
        for (Class innerClass : innerClasses) {   //if(icls.isSynthetic()) continue;
            JavaDecl decl = getInnerClassForName(innerClass.getSimpleName());
            if (decl == null) {
                decl = getJavaDecl(innerClass);
                addDecl(decl);
                addedDecls++;
            } else removedDecls.remove(decl);
        }

        // Get Fields
        Field[] fields;
        try { fields = evalClass.getDeclaredFields(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each declared field - also make sure field type is in refs
        for (Field field : fields) {
            JavaDecl decl = getField(field);
            if (decl == null) {
                decl = new JavaField(_resolver, this, field);
                addDecl(decl);
                addedDecls++;
            } else removedDecls.remove(decl);
        }

        // Get Methods
        Method[] methods;
        try { methods = evalClass.getDeclaredMethods(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each declared method - also make sure return/parameter types are in refs
        for (Method meth : methods) {
            if (meth.isSynthetic()) continue;
            JavaMethod decl = getMethodDecl(meth);
            if (decl == null) {
                decl = new JavaMethod(_resolver, this, meth);
                addDecl(decl);
                decl.initTypes(meth);
                addedDecls++;
            } else removedDecls.remove(decl);
        }

        // Get Constructors
        Constructor[] constructors;
        try { constructors = evalClass.getDeclaredConstructors(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each constructor - also make sure parameter types are in refs
        for (Constructor constr : constructors) {
            if (constr.isSynthetic()) continue;
            JavaConstructor decl = getConstructorDecl(constr);
            if (decl == null) {
                decl = new JavaConstructor(_resolver, this, constr);
                addDecl(decl);
                decl.initTypes(constr);
                addedDecls++;
            } else removedDecls.remove(decl);
        }

        // Array.length: Handle this special for Object[]
        if (isArray() && getFieldForName("length") == null) {
            Field lenField = getLenField();
            JavaDecl decl = new JavaField(_resolver, this, lenField);
            addDecl(decl);
            addedDecls++;
        }

        // Remove unused decls
        for (JavaDecl jd : removedDecls)
            removeDecl(jd);

        // Return whether decls were changed
        boolean changed = addedDecls > 0 || removedDecls.size() > 0;
        if (changed)
            _allDecls = null;

        // Return
        return changed;
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
        List<JavaTypeVariable> tvdecls = getTypeVars();
        for (JavaTypeVariable jd : tvdecls)
            if (jd.getName().equals(aName))
                return jd;
        return null;
    }

    /**
     * Returns a TypeVar decl for inner class name.
     */
    public int getTypeVarIndexForName(String aName)
    {
        List<JavaTypeVariable> tvdecls = getTypeVars();
        for (int i = 0, iMax = tvdecls.size(); i < iMax; i++) {
            JavaTypeVariable jd = tvdecls.get(i);
            if (jd.getName().equals(aName))
                return i;
        }
        return -1;
    }

    /**
     * Returns the list of all decls.
     */
    public List<JavaDecl> getAllDecls()
    {
        // If already set, just return
        if (_allDecls != null) return _allDecls;

        // Create new AllDecls cached list with decls for fields, methods, constructors, inner classes and this class
        List<JavaField> fdecls = getFields();
        List<JavaDecl> decls = new ArrayList<>(fdecls.size() + _methDecls.size() + _constrDecls.size() + _innerClasses.size() + 1);
        decls.add(this);
        decls.addAll(_fieldDecls);
        decls.addAll(_methDecls);
        decls.addAll(_constrDecls);
        decls.addAll(_innerClasses);

        // Set/return
        return _allDecls = decls;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public List<JavaField> getPrefixFields(String aPrefix)
    {
        // Create return list of prefix fields
        List<JavaField> fieldsWithPrefix = new ArrayList<>();

        // Iterate over classes
        for (JavaClass cls = this; cls != null; cls = cls.getSuper()) {

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
        List<JavaMethod> methodsWithPrefix = new ArrayList();

        // Iterate over classes
        for (JavaClass cls = this; cls != null; cls = cls.getSuper()) {

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
        for (JavaClass cls = this; cls != null; cls = cls.getSuper()) {
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
        for (JavaClass cls = this; cls != null; cls = cls.getSuper()) {
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
        for (JavaClass cls = this; cls != null; cls = cls.getSuper()) {
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
        if (methods.size() > 0) {
            if (matches == Collections.EMPTY_LIST) matches = methods;
            else matches.addAll(methods);
        }

        // Search this class and superclasses for compatible interface
        for (JavaClass cls = this; cls != null; cls = cls.getSuper()) {
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
     * Returns the field decl for field.
     */
    public JavaField getField(Field aField)
    {
        String name = aField.getName();
        JavaField field = getFieldForName(name);
        if (field == null)
            return null;

        int mods = aField.getModifiers();
        if (mods != field.getModifiers())
            return null;

        // Return
        return field;
    }

    /**
     * Returns the method decl for method.
     */
    public JavaMethod getMethodDecl(Method aMeth)
    {
        String id = ResolverUtils.getIdForMember(aMeth);
        JavaMethod method = getMethodForId(id);
        if (method == null)
            return null;

        int mods = aMeth.getModifiers();
        if (mods != method.getModifiers())
            return null;

        // Check return type?
        return method;
    }

    /**
     * Returns the method decl for id string.
     */
    private JavaMethod getMethodForId(String anId)
    {
        List<JavaMethod> methods = getMethods();
        for (JavaMethod method : methods)
            if (method.getId().equals(anId))
                return method;

        // Return
        return null;
    }

    /**
     * Returns the decl for constructor.
     */
    public JavaConstructor getConstructorDecl(Constructor aConstr)
    {
        String id = ResolverUtils.getIdForMember(aConstr);
        JavaConstructor constructor = getConstructorForId(id);
        if (constructor == null)
            return null;

        // Check mods
        int mods = aConstr.getModifiers();
        if (mods != constructor.getModifiers())
            return null;

        // Return
        return constructor;
    }

    /**
     * Returns the Constructor decl for id string.
     */
    public JavaConstructor getConstructorForId(String anId)
    {
        List<JavaConstructor> constructors = getConstructors();
        for (JavaConstructor constructor : constructors)
            if (constructor.getId().equals(anId))
                return constructor;

        // Return
        return null;
    }

    /**
     * Adds a decl.
     */
    public void addDecl(JavaDecl aDecl)
    {
        DeclType type = aDecl.getType();
        switch (type) {
            case Field: _fieldDecls.add((JavaField) aDecl); break;
            case Method: _methDecls.add((JavaMethod) aDecl); break;
            case Constructor: _constrDecls.add((JavaConstructor) aDecl); break;
            case Class: _innerClasses.add((JavaClass) aDecl); break;
            case TypeVar: _typeVarDecls.add((JavaTypeVariable) aDecl); break;
            default: throw new RuntimeException("JavaDeclHpr.addDecl: Invalid type " + type);
        }
    }

    /**
     * Removes a decl.
     */
    public void removeDecl(JavaDecl aDecl)
    {
        DeclType type = aDecl.getType();
        switch (type) {
            case Field: _fieldDecls.remove(aDecl); break;
            case Method: _methDecls.remove(aDecl); break;
            case Constructor: _constrDecls.remove(aDecl); break;
            case Class: _innerClasses.remove(aDecl); break;
            case TypeVar: _typeVarDecls.remove(aDecl); break;
            default: throw new RuntimeException("JavaDeclHpr.removeDecl: Invalid type " + type);
        }
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

    // Bogus class to get length
    private static class Array { public int length; }

    private static Field getLenField()
    {
        try { return Array.class.getField("length"); }
        catch (Exception e) { return null; }
    }
}