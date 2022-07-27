package javakit.reflect;
import java.lang.reflect.*;
import java.util.*;
import javakit.parse.*;
import snap.util.ClassUtils;

/**
 * A class that manages all the JavaDecls for a project.
 */
public class Resolver {

    // The current Resolver
    private static Resolver  _current;

    // A cache of JavaPackages by name
    public Map<String,JavaPackage>  _packages = new HashMap<>();

    // A map of class/package names to JavaDecls to provide JavaDecls for project
    public Map<String, JavaDecl>  _decls = new HashMap<>();

    // A cache of JavaParameterizedTypes by id
    public Map<String,JavaParameterizedType>  _paramTypes = new HashMap<>();

    // A cache of JavaGenericArrayType by id
    public Map<String,JavaGenericArrayType>  _arrayTypes = new HashMap<>();

    /**
     * Constructor.
     */
    public Resolver()
    {
        // Set as current resolver
        _current = this;
    }

    /**
     * Returns the ClassLoader.
     */
    public ClassLoader getClassLoader()  { return null; }

    /**
     * Returns a Class for given name.
     */
    public Class<?> getClassForName(String aName)
    {
        // Check for ParamType (should never happen)
        if (aName.indexOf('<') > 0)
            throw new RuntimeException("Resolver.getClassForName: Shouldn't happen: " + aName);

        // Get Class loader, find class
        ClassLoader classLoader = getClassLoader();
        Class<?> cls = ClassUtils.getClassForName(aName, classLoader);

        // Return
        return cls;
    }

    /**
     * Returns a JavaClass for given class name.
     */
    public JavaClass getJavaClassForName(String aClassName)
    {
        // Lookup JavaClass by name and return if already set
        JavaClass javaClass = (JavaClass) _decls.get(aClassName);
        if (javaClass != null)
            return javaClass;

        // Otherwise lookup Class for name
        Class<?> cls = getClassForName(aClassName);
        if (cls != null)
            return getJavaClassForClass(cls);

        // Return
        return null;
    }

    /**
     * Returns a JavaClass for given Class.
     */
    public JavaClass getJavaClassForClass(Class<?> aClass)
    {
        // Lookup class decl by name and return if already set
        String className = aClass.getName();
        JavaClass javaClass = (JavaClass) _decls.get(className);
        if (javaClass != null)
            return javaClass;

        // Create JavaClass and add to Decls map
        JavaDecl parDecl = getParentDecl(aClass);
        javaClass = new JavaClass(this, parDecl, aClass);

        // Add to Resolver classes cache
        //_decls.put(className, javaClass);

        // Add array alternate name?
        /*if (aClass.isArray()) {
            String altName = ResolverUtils.getIdForClass(aClass);
            if (!altName.equals(className))
                _decls.put(altName, javaClass);
        }*/

        // Return
        return javaClass;
    }

    /**
     * Returns a package decl.
     */
    public JavaPackage getJavaPackageForName(String aName)
    {
        // If bogus package name, just return
        if (aName == null || aName.length() == 0) return null;

        // Get from Packages cache and just return if found
        JavaPackage pkg = _packages.get(aName);
        if (pkg != null)
            return pkg;

        // Get parent package
        JavaPackage parent = null;
        int ind = aName.lastIndexOf('.');
        if (ind >= 0) {
            String pname = aName.substring(0, ind);
            parent = getJavaPackageForName(pname);
        }

        // Create new JavaPackage and add to Packages cache
        pkg = new JavaPackage(this, parent, aName);
        _packages.put(aName, pkg);

        // Return
        return pkg;
    }

    /**
     * Returns a JavaDecl for object.
     */
    public JavaDecl getJavaDecl(Object anObj)
    {
        // Handle String (Class or package name)
        if (anObj instanceof String) {
            String id = (String) anObj;

            // If decl exists for name, just return
            JavaDecl javaDecl = _decls.get(id);
            if (javaDecl != null)
                return javaDecl;

            // If class exists, forward to getClassDecl()
            Class<?> classForName = getClassForName(id);
            if (classForName != null)
                return getJavaClassForClass(classForName);
            return null;
        }

        // Handle Class
        JavaDecl jd;
        if (anObj instanceof Class) {
            Class<?> cls = (Class<?>) anObj;
            jd = getJavaClassForClass(cls);
        }

        // Handle Field
        else if (anObj instanceof Field) {
            Field field = (Field) anObj;
            Class<?> cls = field.getDeclaringClass();
            JavaClass decl = getJavaClassForClass(cls);
            jd = decl.getField(field);
        }

        // Handle Method
        else if (anObj instanceof Method) {
            Method meth = (Method) anObj;
            Class<?> cls = meth.getDeclaringClass();
            JavaClass decl = getJavaClassForClass(cls);
            jd = decl.getMethodDecl(meth);
        }

        // Handle Constructor
        else if (anObj instanceof Constructor) {
            Constructor<?> constr = (Constructor<?>) anObj;
            Class<?> cls = constr.getDeclaringClass();
            JavaClass decl = getJavaClassForClass(cls);
            jd = decl.getConstructorDecl(constr);
        }

        // Handle JVarDecl
        else if (anObj instanceof JVarDecl) {
            JVarDecl varDecl = (JVarDecl) anObj;
            JClassDecl varDeclClassDecl = varDecl.getParent(JClassDecl.class);
            JavaDecl varDeclClass = varDeclClassDecl.getDecl();
            jd = new JavaLocalVar(this, varDeclClass, varDecl);
        }

        // Handle Java.lang.reflect.Type
        else if (anObj instanceof Type) {
            Type type = (Type) anObj; //Class cls = getClass(type);
            jd = getTypeDecl(type);
        }

        // Complain
        else throw new RuntimeException("Resolver.getJavaDecl: Unsupported type " + anObj);

        if (jd == null)
            System.out.println("Resolver.getJavaDecl: Decl not found for " + anObj);

        // Return
        return jd;
    }

    /**
     * Returns a JavaDecl for type.
     */
    public JavaType getTypeDecl(Type aType)
    {
        // Handle Class
        if (aType instanceof Class)
            return getJavaClassForClass((Class<?>) aType);

        // Handle ParameterizedType
        if (aType instanceof ParameterizedType)
            return getParameterizedTypeDecl((ParameterizedType) aType);

        // Handle TypeVariable
        if (aType instanceof TypeVariable)
            return getTypeVariableDecl((TypeVariable<?>) aType);

        // Handle GenericArrayType
        if (aType instanceof GenericArrayType)
            return getGenericArrayTypeDecl((GenericArrayType) aType);

        // Handle WildCard
        Class<?> cls = ResolverUtils.getClassForType(aType);
        return getJavaClassForClass(cls);
        //throw new RuntimeException("Resolver.getTypeDecl: Unsupported type " + aType);
    }

    /**
     * Returns a generic ArrayType decl.
     */
    public JavaGenericArrayType getGenericArrayTypeDecl(GenericArrayType aGAT)
    {
        // Check ArrayTypes cache and return if found
        String id = ResolverUtils.getIdForGenericArrayType(aGAT);
        JavaGenericArrayType decl = _arrayTypes.get(id);
        if (decl != null)
            return decl;

        // Create and add to cache
        decl = new JavaGenericArrayType(this, null, aGAT);
        _arrayTypes.put(id, decl);

        // Return
        return decl;
    }

    /**
     * Returns a ParameterizedType Decl.
     */
    public JavaParameterizedType getParameterizedTypeDecl(ParameterizedType aPT)
    {
        // Check ParamTypes cache and return if found
        String id = ResolverUtils.getIdForParameterizedType(aPT);
        JavaParameterizedType decl = _paramTypes.get(id);
        if (decl != null)
            return decl;

        // Get RawType and ArgTypes as JavaType
        Type rawType = aPT.getRawType();
        Type[] typArgs = aPT.getActualTypeArguments();
        JavaType rawTypeDecl = getTypeDecl(rawType);
        JavaType[] typeArgDecls = getJavaTypeArrayForTypes(typArgs);

        // Create and add to cache
        decl = new JavaParameterizedType(this, rawTypeDecl, typeArgDecls);
        _paramTypes.put(id, decl);

        // Return
        return decl;
    }

    /**
     * Returns the param type with given name.
     */
    public JavaType getParameterizedTypeDeclForParts(JavaType aType, JavaType[] theTypeArgs)
    {
        // Get id and decl for id (just return if found)
        String id = ResolverUtils.getIdForParameterizedTypeParts(aType, theTypeArgs);
        JavaParameterizedType decl = _paramTypes.get(id);
        if (decl != null)
            return decl;

        // Create new decl, add to map
        decl = new JavaParameterizedType(this, aType, theTypeArgs);
        _paramTypes.put(id, decl);

        // Return
        return decl;
    }

    /**
     * Returns a ParameterizedType Decl.
     */
    public JavaTypeVariable getTypeVariableDecl(TypeVariable<?> typeVar)
    {
        // Get class or method
        GenericDeclaration classOrMethod = typeVar.getGenericDeclaration();
        JavaDecl parentDecl = getJavaDecl(classOrMethod);
        String name = typeVar.getName();

        // Handle class
        if (parentDecl instanceof JavaClass) {
            JavaClass javaClass = (JavaClass) parentDecl;
            return javaClass.getTypeVarForName(name);
        }

        // Handle Method/Constructor
        else if (parentDecl instanceof JavaExecutable) {
            JavaExecutable javaMethod = (JavaExecutable) parentDecl;
            return javaMethod.getTypeVarForName(name);
        }

        // Can't resolve
        System.out.println("Resolver.getTypeDecl: Can't resolve TypeVariable: " + name + " for " + classOrMethod);
        return null;
    }

    /**
     * Returns the parent decl for a class.
     */
    private JavaDecl getParentDecl(Class<?> aClass)
    {
        // Get parent class, get decl from parent decl
        Class<?> parentClass = aClass.getDeclaringClass();
        if (parentClass != null)
            return getJavaClassForClass(parentClass);

        // Get parent package
        Package pkg = aClass.getPackage();
        String pkgName = pkg != null ? pkg.getName() : null;
        if (pkgName != null && pkgName.length() > 0)
            return getJavaPackageForName(pkgName);

        // Return null
        return null;
    }

    /**
     * Returns a JavaType array for given java.lang.reflect.Type array.
     */
    public JavaType[] getJavaTypeArrayForTypes(Type[] theTypes)
    {
        // Create JavaTypes array
        JavaType[] javaTypes = new JavaType[theTypes.length];

        // Iterate over types and convert each to JavaType
        for (int i = 0; i < theTypes.length; i++) {
            Type type = theTypes[i];
            JavaType javaType = getTypeDecl(type);
            if (javaType == null)
                System.err.println("Resolver.getJavaTypeArray: Couldn't resolve type: " + type);
            else javaTypes[i] = javaType;
        }

        // Return
        return javaTypes;
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
    public String toStringProps()  { return ""; }

    /**
     * Returns the current resolver.
     */
    public static Resolver getCurrent()
    {
        return _current;
    }
}