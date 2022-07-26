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
                return getJavaClass(classForName);
            return null;
        }

        // Handle Class
        JavaDecl jd;
        if (anObj instanceof Class) {
            Class<?> cls = (Class<?>) anObj;
            jd = getJavaClass(cls);
        }

        // Handle Field
        else if (anObj instanceof Field) {
            Field field = (Field) anObj;
            Class<?> cls = field.getDeclaringClass();
            JavaClass decl = getJavaClass(cls);
            jd = decl.getField(field);
        }

        // Handle Method
        else if (anObj instanceof Method) {
            Method meth = (Method) anObj;
            Class<?> cls = meth.getDeclaringClass();
            JavaClass decl = getJavaClass(cls);
            jd = decl.getMethodDecl(meth);
        }

        // Handle Constructor
        else if (anObj instanceof Constructor) {
            Constructor<?> constr = (Constructor<?>) anObj;
            Class<?> cls = constr.getDeclaringClass();
            JavaClass decl = getJavaClass(cls);
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
     * Returns a JavaClass for given Class.
     */
    public JavaClass getJavaClass(Class<?> aClass)
    {
        // Lookup class decl by name and return if already set
        String className = aClass.getName();
        JavaClass javaClass = (JavaClass) _decls.get(className);
        if (javaClass != null)
            return javaClass;

        // Create JavaClass and add to Decls map
        JavaDecl parDecl = getParentDecl(aClass);
        javaClass = new JavaClass(this, parDecl, aClass);
        _decls.put(className, javaClass);

        // Return
        return javaClass;
    }

    /**
     * Returns a JavaDecl for type.
     */
    public JavaType getTypeDecl(Type aType)
    {
        // Handle ParameterizedType
        if (aType instanceof ParameterizedType)
            return getParameterizedTypeDecl((ParameterizedType) aType);

        // Handle TypeVariable
        if (aType instanceof TypeVariable)
            return getTypeVariableDecl((TypeVariable) aType);

        // Handle GenericArrayType
        if (aType instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) aType;
            Class<?> arrayClass = ResolverUtils.getClassForType(aType);
        }

        String id = ResolverUtils.getId(aType);
        JavaType decl = (JavaType) _decls.get(id);
        if (decl != null)
            return decl;

        // Handle Class
        if (aType instanceof Class) {
            Class<?> cls = (Class<?>) aType;
            return getJavaClass(cls);
        }

        // This is lame
        Class<?> cls = ResolverUtils.getClassForType(aType);
        return getJavaClass(cls);
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

        // Create and add to cache
        decl = new JavaParameterizedType(this, null, aPT);
        _paramTypes.put(id, decl);

        // Return
        return decl;
    }

    /**
     * Returns the param type with given name.
     */
    public JavaType getParameterizedTypeDeclForParts(JavaDecl aDecl, JavaType[] theTypeDecls)
    {
        // Get id and decl for id (just return if found)
        String id = ResolverUtils.getIdForParameterizedTypeParts(aDecl, theTypeDecls);
        JavaParameterizedType decl = _paramTypes.get(id);
        if (decl != null)
            return decl;

        // Create new decl, add to map
        decl = new JavaParameterizedType(this, aDecl, theTypeDecls);
        _paramTypes.put(id, decl);

        // Return
        return decl;
    }

    /**
     * Returns a ParameterizedType Decl.
     */
    public JavaTypeVariable getTypeVariableDecl(TypeVariable typeVar)
    {
        // Get class or method
        GenericDeclaration classOrMethod = typeVar.getGenericDeclaration();
        JavaDecl parentDecl = getJavaDecl(classOrMethod);
        String name = typeVar.getName();

        // Handle class
        if (parentDecl instanceof JavaClass) {
            JavaClass javaClass = (JavaClass) parentDecl;
            return javaClass.getTypeVar(name);
        }

        // Handle Method/Constructor
        else if (parentDecl instanceof JavaExecutable) {
            JavaExecutable javaMethod = (JavaExecutable) parentDecl;
            return javaMethod.getTypeVar(name);
        }

        // Can't resolve
        System.out.println("Resolver.getTypeDecl: Can't resolve TypeVariable: " + name + " for " + parentDecl);
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
            return getJavaDecl(parentClass);

        // Get parent package
        Package pkg = aClass.getPackage();
        String pkgName = pkg != null ? pkg.getName() : null;
        if (pkgName != null && pkgName.length() > 0)
            return getPackageDecl(pkgName);

        // Return null
        return null;
    }

    /**
     * Returns a package decl.
     */
    private JavaDecl getPackageDecl(String aName)
    {
        // If bogus package name, just return
        if (aName == null || aName.length() == 0) return null;

        // Get from Decls map and just return if found
        JavaPackage pkg = (JavaPackage) _decls.get(aName);
        if (pkg != null)
            return pkg;

        // Get parent decl
        JavaDecl parDecl = null;
        int ind = aName.lastIndexOf('.');
        if (ind >= 0) {
            String pname = aName.substring(0, ind);
            parDecl = getPackageDecl(pname);
        }

        // Create new decl and return
        pkg = new JavaPackage(this, parDecl, aName);
        _decls.put(aName, pkg);

        // Return
        return pkg;
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
        if (aName.indexOf('<') > 0) {
            int ind = aName.indexOf('<');
            System.err.println("Resolver.getClass: Shouldn't happen: " + aName);
            aName = aName.substring(0, ind);
        }

        // Get Class loader, find class and return
        ClassLoader cldr = getClassLoader();
        Class<?> cls = ClassUtils.getClassForName(aName, cldr);
        return cls;
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