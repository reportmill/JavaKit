package javakit.reflect;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This class updates a JavaClass from resolver.
 */
public class JavaClassUpdater {

    // The JavaClass
    private JavaClass  _javaClass;

    // The Resolver that produced this decl
    protected Resolver  _resolver;

    // A cached list of all decls
    private List<JavaDecl>  _allDecls;

    /**
     * Constructor.
     */
    public JavaClassUpdater(JavaClass aClass)
    {
        _javaClass = aClass;
        _resolver = aClass._resolver;
    }

    /**
     * Updates JavaDecls. Returns whether the decls changed since last update.
     */
    public boolean updateDecls()
    {
        // If first time, set decls
        if (_javaClass._fieldDecls == null)
            _javaClass._fieldDecls = new ArrayList<>();

        // Get ClassName
        String className = _javaClass.getClassName();

        // Get real class
        Class<?> realClass = _javaClass.getRealClass();
        if (realClass == null) {
            System.err.println("JavaClass: Failed to load class: " + className);
            return false;
        }

        // Get interfaces
        Class<?>[] interfaces = realClass.getInterfaces();
        _javaClass._interfaces = new JavaClass[interfaces.length];
        for (int i = 0, iMax = interfaces.length; i < iMax; i++) {
            Class<?> intrface = interfaces[i];
            _javaClass._interfaces[i] = _javaClass.getJavaClassForClass(intrface);
        }

        // Create set for added/removed decls
        int addedDecls = 0;
        HashSet<JavaDecl> removedDecls = new HashSet<>(getAllDecls());

        // Make sure class decl is up to date
        if (_javaClass.getModifiers() != realClass.getModifiers())
            _javaClass._mods = realClass.getModifiers();

        // Get TypeVariables
        TypeVariable<?>[] typeVariables;
        try { typeVariables = realClass.getTypeParameters(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each Type parameter
        for (TypeVariable<?> typeVariable : typeVariables) {
            String name = typeVariable.getName();
            JavaDecl decl = _javaClass.getTypeVarForName(name);
            if (decl == null) {
                decl = new JavaTypeVariable(_resolver, _javaClass, typeVariable);
                addDecl(decl);
                addedDecls++;
            }
            else removedDecls.remove(decl);
        }

        // Get Inner Classes
        Class<?>[] innerClasses;
        try { innerClasses = realClass.getDeclaredClasses(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each inner class
        for (Class<?> innerClass : innerClasses) {   //if(icls.isSynthetic()) continue;
            JavaDecl decl = _javaClass.getInnerClassForName(innerClass.getSimpleName());
            if (decl == null) {
                decl = _resolver.getJavaClassForClass(innerClass);
                addDecl(decl);
                addedDecls++;
            }
            else removedDecls.remove(decl);
        }

        // Get Fields
        Field[] fields;
        try { fields = realClass.getDeclaredFields(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each declared field - also make sure field type is in refs
        for (Field field : fields) {
            JavaDecl decl = getField(field);
            if (decl == null) {
                decl = new JavaField(_resolver, _javaClass, field);
                addDecl(decl);
                addedDecls++;
            }
            else removedDecls.remove(decl);
        }

        // Get Methods
        Method[] methods;
        try { methods = realClass.getDeclaredMethods(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each declared method - also make sure return/parameter types are in refs
        for (Method meth : methods) {
            if (meth.isSynthetic()) continue;
            JavaMethod decl = getMethodDecl(meth);
            if (decl == null) {
                decl = new JavaMethod(_resolver, _javaClass, meth);
                addDecl(decl);
                decl.initTypes(meth);
                addedDecls++;
            } else removedDecls.remove(decl);
        }

        // Get Constructors
        Constructor<?>[] constructors;
        try { constructors = realClass.getDeclaredConstructors(); }
        catch (Throwable e) {
            System.err.println(e + " in " + className);
            return false;
        }

        // Add JavaDecl for each constructor - also make sure parameter types are in refs
        for (Constructor<?> constr : constructors) {
            if (constr.isSynthetic()) continue;
            JavaConstructor decl = getConstructorDecl(constr);
            if (decl == null) {
                decl = new JavaConstructor(_resolver, _javaClass, constr);
                addDecl(decl);
                decl.initTypes(constr);
                addedDecls++;
            }
            else removedDecls.remove(decl);
        }

        // Array.length: Handle this special for Object[]
        if (_javaClass.isArray() && _javaClass.getFieldForName("length") == null) {
            Field lenField = getLenField();
            JavaDecl decl = new JavaField(_resolver, _javaClass, lenField);
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
     * Returns the list of all decls.
     */
    public List<JavaDecl> getAllDecls()
    {
        // If already set, just return
        if (_allDecls != null) return _allDecls;

        // Create new AllDecls cached list with decls for fields, methods, constructors, inner classes and this class
        List<JavaField> fdecls = _javaClass.getFields();
        int memberCount = fdecls.size() + _javaClass._methDecls.size() + _javaClass._constrDecls.size();
        int declCount = memberCount + _javaClass._innerClasses.size() + 1;
        List<JavaDecl> decls = new ArrayList<>(declCount);
        decls.add(_javaClass);
        decls.addAll(_javaClass._fieldDecls);
        decls.addAll(_javaClass._methDecls);
        decls.addAll(_javaClass._constrDecls);
        decls.addAll(_javaClass._innerClasses);

        // Set/return
        return _allDecls = decls;
    }

    /**
     * Returns the field decl for field.
     */
    public JavaField getField(Field aField)
    {
        String name = aField.getName();
        JavaField field = _javaClass.getFieldForName(name);
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
        List<JavaMethod> methods = _javaClass.getMethods();
        for (JavaMethod method : methods)
            if (method.getId().equals(anId))
                return method;

        // Return
        return null;
    }

    /**
     * Returns the decl for constructor.
     */
    public JavaConstructor getConstructorDecl(Constructor<?> aConstr)
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
        List<JavaConstructor> constructors = _javaClass.getConstructors();
        for (JavaConstructor constructor : constructors)
            if (constructor.getId().equals(anId))
                return constructor;

        // Return
        return null;
    }

    /**
     * Returns a JavaMember for given java.lang.reflect.Member.
     */
    public JavaMember getJavaMemberForMember(Member aMember)
    {
        // Handle Field
        if (aMember instanceof Field)
            return getField((Field) aMember);

        // Handle Method
        if (aMember instanceof Method)
            return getMethodDecl((Method) aMember);

        // Handle Constructor
        if (aMember instanceof Constructor)
            return getConstructorDecl((Constructor<?>) aMember);

        // Handle MemberName
        throw new RuntimeException("JavaClassUpdater.getJavaMemberForMember: " + aMember);
    }

    /**
     * Adds a decl.
     */
    public void addDecl(JavaDecl aDecl)
    {
        JavaDecl.DeclType type = aDecl.getType();
        switch (type) {
            case Field: _javaClass._fieldDecls.add((JavaField) aDecl); break;
            case Method: _javaClass._methDecls.add((JavaMethod) aDecl); break;
            case Constructor: _javaClass._constrDecls.add((JavaConstructor) aDecl); break;
            case Class: _javaClass._innerClasses.add((JavaClass) aDecl); break;
            case TypeVar: _javaClass._typeVarDecls.add((JavaTypeVariable) aDecl); break;
            default: throw new RuntimeException("JavaDeclHpr.addDecl: Invalid type " + type);
        }
    }

    /**
     * Removes a decl.
     */
    public void removeDecl(JavaDecl aDecl)
    {
        JavaDecl.DeclType type = aDecl.getType();
        switch (type) {
            case Field: _javaClass._fieldDecls.remove(aDecl); break;
            case Method: _javaClass._methDecls.remove(aDecl); break;
            case Constructor: _javaClass._constrDecls.remove(aDecl); break;
            case Class: _javaClass._innerClasses.remove(aDecl); break;
            case TypeVar: _javaClass._typeVarDecls.remove(aDecl); break;
            default: throw new RuntimeException("JavaDeclHpr.removeDecl: Invalid type " + type);
        }
    }

    /**
     * Returns a bogus field for Array.length.
     */
    private static Field getLenField()
    {
        try { return Array.class.getField("length"); }
        catch (Exception e) { return null; }
    }

    // Bogus class to get length
    private static class Array { public int length; }
}
