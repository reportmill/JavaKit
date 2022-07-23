package javakit.resolver;
import java.lang.reflect.*;
import java.util.*;

import javakit.parse.*;
import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaParameterizedType;
import javakit.reflect.JavaType;
import snap.util.ClassUtils;

/**
 * A class that manages all the JavaDecls for a project.
 */
public class Resolver {

    // The current Resolver
    private static Resolver  _current;

    // A map of class/package names to JavaDecls to provide JavaDecls for project
    public Map<String, JavaDecl> _decls = new HashMap<>();

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
            JavaDecl jd = _decls.get(id);
            if (jd != null) return jd;

            // If class exists, forward to getClassDecl()
            Class<?> cls = getClassForName(id);
            if (cls != null)
                return getClassDecl(cls);
            return null;
        }

        // Handle Class
        JavaDecl jd;
        if (anObj instanceof Class) {
            Class<?> cls = (Class<?>) anObj;
            jd = getClassDecl(cls);
        }

        // Handle Field
        else if (anObj instanceof Field) {
            Field field = (Field) anObj;
            Class<?> cls = field.getDeclaringClass();
            JavaClass decl = getClassDecl(cls);
            jd = decl.getField(field);
        }

        // Handle Method
        else if (anObj instanceof Method) {
            Method meth = (Method) anObj;
            Class<?> cls = meth.getDeclaringClass();
            JavaClass decl = getClassDecl(cls);
            jd = decl.getMethodDecl(meth);
        }

        // Handle Constructor
        else if (anObj instanceof Constructor) {
            Constructor constr = (Constructor) anObj;
            Class<?> cls = constr.getDeclaringClass();
            JavaClass decl = getClassDecl(cls);
            jd = decl.getConstructorDecl(constr);
        }

        // Handle JVarDecl
        else if (anObj instanceof JVarDecl) {
            JVarDecl vd = (JVarDecl) anObj;
            JClassDecl cd = vd.getParent(JClassDecl.class);
            JavaDecl decl = cd.getDecl();
            jd = new JavaDecl(this, decl, vd);
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
     * Returns a JavaDeclClass for object.
     */
    public JavaClass getJavaDeclClass(Object anObj)
    {
        JavaDecl cd = getJavaDecl(anObj);
        return cd instanceof JavaClass ? (JavaClass) cd : null;
    }

    /**
     * Returns a JavaDecl for type.
     */
    public JavaType getTypeDecl(Type aType)
    {
        String id = ResolverUtils.getId(aType);
        JavaType decl = (JavaType) _decls.get(id);
        if (decl != null)
            return decl;

        // Handle ParameterizedType
        if (aType instanceof ParameterizedType) {
            decl = new JavaParameterizedType(this, null, (ParameterizedType) aType);
            return decl;
        }

        // Handle TypeVariable
        if (aType instanceof TypeVariable) {
            TypeVariable typeVar = (TypeVariable) aType;
            String name = typeVar.getName();
            GenericDeclaration gdecl = typeVar.getGenericDeclaration();
            JavaDecl pdecl = getJavaDecl(gdecl);
            decl = pdecl.getTypeVar(name);
            return decl;
        }

        // Handle Class
        if (aType instanceof Class) {
            Class cls = ResolverUtils.getClassForType(aType);
            return getClassDecl(cls);
        }

        throw new RuntimeException("Resolver.getTypeDecl: Unsupported type " + aType);
    }

    /**
     * Returns a class decl.
     */
    private JavaClass getClassDecl(Class aClass)
    {
        // Lookup class decl by name and return if already set
        String cname = aClass.getName();
        JavaClass decl = (JavaClass) _decls.get(cname);
        if (decl != null)
            return decl;

        // Create decl and return
        JavaDecl parDecl = getParentDecl(aClass);
        decl = new JavaClass(this, parDecl, aClass);
        return decl;
    }

    /**
     * Returns the parent decl for a class.
     */
    private JavaDecl getParentDecl(Class aClass)
    {
        // If declaring class, get decl from parent decl
        Class dcls = aClass.getDeclaringClass();
        if (dcls != null)
            return getJavaDecl(dcls);

        // Get parent decl
        Package pkg = aClass.getPackage();
        String pkgName = pkg != null ? pkg.getName() : null;
        if (pkgName != null && pkgName.length() > 0)
            return getPackageDecl(pkgName);

        // Return null since no declaring class or package
        return null;
    }

    /**
     * Returns a package decl.
     */
    private JavaDecl getPackageDecl(String aName)
    {
        if (aName == null || aName.length() == 0) return null;  // If bogus package name, just return
        JavaDecl pdecl = _decls.get(aName);
        if (pdecl == null)
            _decls.put(aName, pdecl = createPackageDecl(aName));
        return pdecl;
    }

    /**
     * Creates a package decl.
     */
    private JavaDecl createPackageDecl(String aName)
    {
        // Get parent decl
        JavaDecl parDecl = null;
        int ind = aName.lastIndexOf('.');
        if (ind >= 0) {
            String pname = aName.substring(0, ind);
            parDecl = getPackageDecl(pname);
        }

        // Create new decl and return
        return new JavaDecl(this, parDecl, aName);
    }

    /**
     * Returns the param type with given name.
     */
    public JavaType getParamTypeDecl(JavaDecl aDecl, JavaType[] theTypeDecls)
    {
        // Get id and decl for id (just return if found)
        String id = ResolverUtils.getParamTypeId(aDecl, theTypeDecls);
        JavaType jd = (JavaType) _decls.get(id);
        if (jd != null)
            return jd;

        // Create new decl, add to map
        jd = new JavaParameterizedType(this, aDecl, theTypeDecls);
        _decls.put(id, jd);

        // Return
        return jd;
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
     * Returns reference nodes in given JNode that match given JavaDecl.
     */
    public static void getMatches(JNode aNode, JavaDecl aDecl, List<JNode> theMatches)
    {
        // If JType check name
        if (aNode instanceof JType || aNode instanceof JExprId) {
            JavaDecl decl = isPossibleMatch(aNode, aDecl) ? aNode.getDecl() : null;
            if (decl != null && aDecl.matches(decl))
                theMatches.add(aNode);
        }

        // Recurse
        for (JNode child : aNode.getChildren())
            getMatches(child, aDecl, theMatches);
    }

    /**
     * Returns reference nodes in given JNode that match given JavaDecl.
     */
    public static void getRefMatches(JNode aNode, JavaDecl aDecl, List<JNode> theMatches)
    {
        // If JType check name
        if (aNode instanceof JType || aNode instanceof JExprId) {
            if (isPossibleMatch(aNode, aDecl) && !aNode.isDecl()) {
                JavaDecl decl = aNode.getDecl();
                if (decl != null && aDecl.matches(decl) && aNode.getParent(JImportDecl.class) == null)
                    theMatches.add(aNode);
            }
        }

        // Recurse
        for (JNode child : aNode.getChildren())
            getRefMatches(child, aDecl, theMatches);
    }

    /**
     * Returns declaration nodes in given JNode that match given JavaDecl.
     */
    public static JNode getDeclMatch(JNode aNode, JavaDecl aDecl)
    {
        List<JNode> matches = new ArrayList();
        getDeclMatches(aNode, aDecl, matches);
        return matches.size() > 0 ? matches.get(0) : null;
    }

    /**
     * Returns declaration nodes in given JNode that match given JavaDecl.
     */
    public static void getDeclMatches(JNode aNode, JavaDecl aDecl, List<JNode> theMatches)
    {
        // If JType check name
        if (aNode instanceof JType || aNode instanceof JExprId) {
            JavaDecl decl = aNode.isDecl() && isPossibleMatch(aNode, aDecl) ? aNode.getDecl() : null;
            if (decl != null && aDecl.matches(decl))
                theMatches.add(aNode);
        }

        // Recurse
        for (JNode child : aNode.getChildren())
            getDeclMatches(child, aDecl, theMatches);
    }

    /**
     * Returns whether node is a possible match.
     */
    private static boolean isPossibleMatch(JNode aNode, JavaDecl aDecl)
    {
        // If Node is type and Decl is type and Decl.SimpleName contains Node.SimpleName
        if (aNode instanceof JType && aDecl.isType()) {
            JType type = (JType) aNode;
            String sname = type.getSimpleName();
            return aDecl.getSimpleName().contains(sname);
        }

        // If Node is identifier and Decl.Name contains Node.Name
        if (aNode instanceof JExprId)
            return aDecl.getName().contains(aNode.getName());

        return false;
    }

    /**
     * Returns a simple class name.
     */
    public static String getSimpleName(String cname)
    {
        int i = cname.lastIndexOf('$');
        if (i < 0) i = cname.lastIndexOf('.');
        if (i > 0) cname = cname.substring(i + 1);
        return cname;
    }

    /**
     * Returns the current resolver.
     */
    public static Resolver getCurrent()
    {
        return _current;
    }
}