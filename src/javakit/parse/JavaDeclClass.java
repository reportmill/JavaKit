package javakit.parse;
import java.lang.reflect.*;
import java.util.*;
import javakit.resolver.Resolver;
import javakit.resolver.ResolverUtils;
import snap.util.StringUtils;

/**
 * A subclass of JavaDecl especially for Class declarations.
 */
public class JavaDeclClass extends JavaDecl {

    // The SuperClass type
    private JavaDecl _stype;

    // The super class decl
    private JavaDeclClass  _superClassDecl;

    // Whether class decl is enum, interface, primitive
    private boolean _enum, _interface, _primitive;

    // The array of interfaces
    private JavaDeclClass[] _interfaces;

    // The field decls
    private List<JavaDecl>  _fieldDecls;

    // The method decls
    private List<JavaDecl>  _methDecls = new ArrayList<>();

    // The constructor decls
    private List<JavaDecl>  _constrDecls = new ArrayList<>();

    // The inner class decls
    private List<JavaDeclClass> _innerClassDecls = new ArrayList<>();

    // The type var decls
    private List<JavaDecl> _typeVarDecls = new ArrayList<>();

    // A cached list of all decls
    private List<JavaDecl> _allDecls;

    // The Array item type (if Array)
    private JavaDecl  _arrayItemType;

    /**
     * Creates a new JavaDeclClass for given owner, parent and Class.
     */
    public JavaDeclClass(Resolver anOwner, JavaDecl aPar, Class<?> aClass)
    {
        // Do normal version
        super(anOwner, aPar, aClass);

        // Set class attributes
        _mods = aClass.getModifiers();
        _type = DeclType.Class;
        _name = ResolverUtils.getId(aClass);
        _simpleName = aClass.getSimpleName();
        _enum = aClass.isEnum();
        _interface = aClass.isInterface();
        _primitive = aClass.isPrimitive();
        _evalType = this;
        _sdecl = null; // Set by owner

        // Add to Owner.Decls map
        _resolver._decls.put(_id, this);

        // Get type super type and set in decl
        AnnotatedType superAType = aClass.getAnnotatedSuperclass();
        Type superType = superAType != null ? superAType.getType() : null;
        if (superType != null) {
            _stype = getJavaDecl(superType);
            _sdecl = _superClassDecl = _stype.getClassType();
        }

        // Handle Array
        if (aClass.isArray()) {

            // Set ArrayItemType and add alternate name to Owner.Decls map
            _arrayItemType = getJavaDecl(aClass.getComponentType());
            _resolver._decls.put(aClass.getName(), this);

            // Set Decls from Object[] for efficiency
            if (aClass != Object[].class) {
                JavaDeclClass aryDecl = (JavaDeclClass) getJavaDecl(Object[].class);
                _fieldDecls = aryDecl.getFields();
                _interfaces = aryDecl._interfaces;
                _methDecls = aryDecl._methDecls;
                _constrDecls = aryDecl._constrDecls;
                _innerClassDecls = aryDecl._innerClassDecls;
                _typeVarDecls = aryDecl._typeVarDecls;
                _allDecls = aryDecl.getAllDecls();
            }
        }
    }

    /**
     * Returns whether is a class reference.
     */
    public boolean isClass()  { return true; }

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
    public JavaDecl getArrayItemType()
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
    public JavaDeclClass getPrimitive()
    {
        if (isPrimitive()) return this;
        switch (_name) {
            case "java.lang.Boolean": return getClassDecl(boolean.class);
            case "java.lang.Byte": return getClassDecl(byte.class);
            case "java.lang.Character": return getClassDecl(char.class);
            case "java.lang.Short": return getClassDecl(short.class);
            case "java.lang.Integer": return getClassDecl(int.class);
            case "java.lang.Long": return getClassDecl(long.class);
            case "java.lang.Float": return getClassDecl(float.class);
            case "java.lang.Double": return getClassDecl(double.class);
            case "java.lang.Void": return getClassDecl(void.class);
            default: return null;
        }
    }

    /**
     * Returns the primitive counter part, if available.
     */
    public JavaDeclClass getPrimitiveAlt()
    {
        if (!isPrimitive()) return this;
        switch (_name) {
            case "boolean": return getClassDecl(Boolean.class);
            case "byte": return getClassDecl(Byte.class);
            case "char": return getClassDecl(Character.class);
            case "short": return getClassDecl(Short.class);
            case "int": return getClassDecl(Integer.class);
            case "long": return getClassDecl(Long.class);
            case "float": return getClassDecl(Float.class);
            case "double": return getClassDecl(Double.class);
            case "void": return getClassDecl(Void.class);
            default: return null;
        }
    }

    /**
     * Returns whether given type is assignable to this JavaDecl.
     */
    public boolean isAssignable(JavaDecl aDecl)
    {
        // If this decl is primitive, forward to primitive version
        if (isPrimitive())
            return isAssignablePrimitive(aDecl);

        // If given val is null or this decl is Object return true
        if (aDecl == null)
            return true;
        if (getName().equals("java.lang.Object"))
            return true;
        JavaDeclClass ctype1 = aDecl.getClassType();
        if (ctype1.isPrimitive())
            ctype1 = ctype1.getPrimitiveAlt();

        // If either are array type, check ArrayItemTypes if both are (otherwise return false)
        if (isArray() || ctype1.isArray()) {
            if (isArray() && ctype1.isArray())
                return getArrayItemType().isAssignable(ctype1.getArrayItemType());
            return false;
        }

        // Iterate up given class superclasses and check class and interfaces
        for (JavaDeclClass ct1 = ctype1; ct1 != null; ct1 = ct1.getSuper()) {

            // If classes match, return true
            if (ct1 == this)
                return true;

            // If any interface of this decl match, return true
            if (isInterface()) {
                for (JavaDeclClass infc : ct1.getInterfaces())
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
        JavaDecl ctype0 = getClassType();
        JavaDecl ctype1 = aDecl.getClassType().getPrimitive();
        if (ctype1 == null)
            return false;
        JavaDecl common = getCommonAncestorPrimitive(ctype1);
        return common == this;
    }

    /**
     * Override to return as Class type.
     */
    public JavaDeclClass getSuper()
    {
        return _superClassDecl;
    }

    /**
     * Returns a resolved type for given unresolved type (TypeVar or ParamType<TypeVar>), if this decl can resolve it.
     */
    public JavaDecl getResolvedType(JavaDecl aDecl)
    {
        // Handle ParamType and anything not a TypeVar
        if (aDecl.isParamType()) {
            System.err.println("JavaDecl.getResolvedType: ParamType not yet supported");
            return aDecl;
        }
        if (!aDecl.isTypeVar())
            return aDecl;

        // If has type var, return bounds type
        String name = aDecl.getName();
        JavaDecl typeVar = getTypeVar(name);
        if (typeVar != null)
            return typeVar.getEvalType();

        // If super has type var, return mapped type //JavaDecl sdecl = getSuper();
        /*if(sdecl!=null && sdecl.getTypeVar(name)!=null) {
            int ind = sdecl.getHpr().getTypeVarDeclIndex(name);
            if(ind>=0 && ind<_paramTypes.length) return _paramTypes[ind]; }*/

        if (_stype != null && _stype.isParamType())
            return _stype.getResolvedType(aDecl);

        return aDecl.getEvalType();
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
            _fieldDecls = new ArrayList();

        // Get eval class
        Class evalClass = getEvalClass();
        String className = getClassName();
        if (evalClass == null) {
            System.err.println("JavaDeclClass: Failed to load class: " + className);
            return false;
        }

        // Get interfaces
        Class[] interfaces = evalClass.getInterfaces();
        _interfaces = new JavaDeclClass[interfaces.length];
        for (int i = 0, iMax = interfaces.length; i < iMax; i++) {
            Class intrface = interfaces[i];
            _interfaces[i] = getClassDecl(intrface);
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
            JavaDecl decl = getTypeVar(name);
            if (decl == null) {
                decl = new JavaDecl(_resolver, this, typeVariable);
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
            JavaDecl decl = getClassDecl(innerClass.getSimpleName());
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
                decl = new JavaDecl(_resolver, this, field);
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
            JavaDecl decl = getMethodDecl(meth);
            if (decl == null) {
                decl = new JavaDecl(_resolver, this, meth);
                addDecl(decl);
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
            JavaDecl decl = getConstructorDecl(constr);
            if (decl == null) {
                decl = new JavaDecl(_resolver, this, constr);
                addDecl(decl);
                addedDecls++;
            } else removedDecls.remove(decl);
        }

        // Array.length: Handle this special for Object[]
        if (isArray() && getField("length") == null) {
            Field lenField = getLenField();
            JavaDecl decl = new JavaDecl(_resolver, this, lenField);
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
    public JavaDeclClass[] getInterfaces()
    {
        getFields();
        return _interfaces;
    }

    /**
     * Returns the fields.
     */
    public List<JavaDecl> getFields()
    {
        if (_fieldDecls == null) updateDecls();
        return _fieldDecls;
    }

    /**
     * Returns the methods.
     */
    public List<JavaDecl> getMethods()
    {
        getFields();
        return _methDecls;
    }

    /**
     * Returns the Constructors.
     */
    public List<JavaDecl> getConstructors()
    {
        getFields();
        return _constrDecls;
    }

    /**
     * Returns the inner classes.
     */
    public List<JavaDeclClass> getClasses()
    {
        getFields();
        return _innerClassDecls;
    }

    /**
     * Returns the inner classes.
     */
    public List<JavaDecl> getTypeVars2()
    {
        getFields();
        return _typeVarDecls;
    }

    /**
     * Returns the list of all decls.
     */
    public List<JavaDecl> getAllDecls()
    {
        // If already set, just return
        if (_allDecls != null) return _allDecls;

        // Create new AllDecls cached list with decls for fields, methods, constructors, inner classes and this class
        List<JavaDecl> fdecls = getFields();
        List<JavaDecl> decls = new ArrayList(fdecls.size() + _methDecls.size() + _constrDecls.size() + _innerClassDecls.size() + 1);
        decls.add(this);
        decls.addAll(_fieldDecls);
        decls.addAll(_methDecls);
        decls.addAll(_constrDecls);
        decls.addAll(_innerClassDecls);

        // Set/return
        return _allDecls = decls;
    }

    /**
     * Returns the field decl for field.
     */
    public JavaDecl getField(Field aField)
    {
        String name = aField.getName();
        JavaDecl decl = getField(name);
        if (decl == null)
            return null;

        int mods = aField.getModifiers();
        if (mods != decl.getModifiers())
            return null;

        //JavaDecl type = _cdecl._owner.getTypeDecl(aField.getGenericType(), _cdecl);
        //if(type!=decl.getEvalType()) return null;

        // Return
        return decl;
    }

    /**
     * Returns a field decl for field name.
     */
    public JavaDecl getField(String aName)
    {
        List<JavaDecl> fdecls = getFields();
        for (JavaDecl jd : fdecls)
            if (jd.getName().equals(aName))
                return jd;
        return null;
    }

    /**
     * Returns a field decl for field name.
     */
    public JavaDecl getFieldDeep(String aName)
    {
        JavaDecl decl = getField(aName);
        if (decl == null && _superClassDecl != null)
            decl = _superClassDecl.getFieldDeep(aName);
        return decl;
    }

    /**
     * Returns the method decl for method.
     */
    public JavaDecl getMethodDecl(Method aMeth)
    {
        String id = ResolverUtils.getId(aMeth);
        JavaDecl decl = getMethodDecl(id);
        if (decl == null)
            return null;

        int mods = aMeth.getModifiers();
        if (mods != decl.getModifiers())
            return null;

        // Check return type?
        return decl;
    }

    /**
     * Returns the method decl for id string.
     */
    public JavaDecl getMethodDecl(String anId)
    {
        List<JavaDecl> mdecls = getMethods();
        for (JavaDecl jd : mdecls)
            if (jd.getId().equals(anId))
                return jd;
        return null;
    }

    /**
     * Returns a method decl for method name and parameter types.
     */
    public JavaDecl getMethodDecl(String aName, JavaDecl theTypes[])
    {
        List<JavaDecl> mdecls = getMethods();
        for (JavaDecl jd : mdecls)
            if (jd.getName().equals(aName) && isClassTypesEqual(jd.getParamTypes(), theTypes))
                return jd;
        return null;
    }

    /**
     * Returns a method decl for method name and parameter types.
     */
    public JavaDecl getMethodDeclDeep(String aName, JavaDecl theTypes[])
    {
        JavaDecl decl = getMethodDecl(aName, theTypes);
        if (decl == null && _superClassDecl != null)
            decl = _superClassDecl.getMethodDeclDeep(aName, theTypes);
        return decl;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public List<JavaDecl> getPrefixFields(String aPrefix)
    {
        // Create return list of prefix fields
        List<JavaDecl> pfields = new ArrayList();

        // Iterate over classes
        for (JavaDeclClass cls = this; cls != null; cls = cls.getSuper()) {

            // Get Class fields
            List<JavaDecl> fdecls = cls.getFields();
            for (JavaDecl fd : fdecls)
                if (StringUtils.startsWithIC(fd.getName(), aPrefix))
                    pfields.add(fd);

            // Should iterate over class interfaces, too
        }

        // Return list of prefix fields
        return pfields;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public List<JavaDecl> getPrefixMethods(String aPrefix)
    {
        // Create return list of prefix methods
        List<JavaDecl> pmeths = new ArrayList();

        // Iterate over classes
        for (JavaDeclClass cls = this; cls != null; cls = cls.getSuper()) {

            // Get Class methods
            List<JavaDecl> mdecls = cls.getMethods();
            for (JavaDecl md : mdecls)
                if (StringUtils.startsWithIC(md.getName(), aPrefix))
                    pmeths.add(md);

            // If interface, iterate over class interfaces, too (should probably do this anyway to catch default methods).
            if (cls.isInterface()) {
                for (JavaDeclClass c2 : cls.getInterfaces()) {
                    List<JavaDecl> pmeths2 = c2.getPrefixMethods(aPrefix);
                    pmeths.addAll(pmeths2);
                }
            }
        }

        // Return list of prefix methods
        return pmeths;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public JavaDecl getCompatibleConstructor(JavaDecl theTypes[])
    {
        List<JavaDecl> cdecls = getConstructors();
        JavaDecl constr = null;
        int rating = 0;
        for (JavaDecl cd : cdecls) {
            int rtg = getMethodRating(cd, theTypes);
            if (rtg > rating) {
                constr = cd;
                rating = rtg;
            }
        }
        return constr;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public JavaDecl getCompatibleMethod(String aName, JavaDecl theTypes[])
    {
        List<JavaDecl> mdecls = getMethods();
        JavaDecl meth = null;
        int rating = 0;
        for (JavaDecl md : mdecls)
            if (md.getName().equals(aName)) {
                int rtg = getMethodRating(md, theTypes);
                if (rtg > rating) {
                    meth = md;
                    rating = rtg;
                }
            }
        return meth;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public JavaDecl getCompatibleMethodDeep(String aName, JavaDecl theTypes[])
    {
        // Search this class and superclasses for compatible method
        for (JavaDeclClass cls = this; cls != null; cls = cls.getSuper()) {
            JavaDecl decl = cls.getCompatibleMethod(aName, theTypes);
            if (decl != null)
                return decl;
        }
        return null;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public JavaDecl getCompatibleMethodAll(String aName, JavaDecl theTypes[])
    {
        // Search this class and superclasses for compatible method
        JavaDecl decl = getCompatibleMethodDeep(aName, theTypes);
        if (decl != null)
            return decl;

        // Search this class and superclasses for compatible interface
        for (JavaDeclClass cls = this; cls != null; cls = cls.getSuper()) {
            for (JavaDeclClass infc : cls.getInterfaces()) {
                decl = infc.getCompatibleMethodAll(aName, theTypes);
                if (decl != null)
                    return decl;
            }
        }

        // If this class is Interface, check Object
        if (isInterface()) {
            JavaDeclClass objDecl = getClassDecl(Object.class);
            return objDecl.getCompatibleMethodDeep(aName, theTypes);
        }

        // Return null since compatible method not found
        return null;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public List<JavaDecl> getCompatibleMethods(String aName, JavaDecl theTypes[])
    {
        List<JavaDecl> matches = Collections.EMPTY_LIST;
        List<JavaDecl> mdecls = getMethods();
        for (JavaDecl md : mdecls)
            if (md.getName().equals(aName)) {
                int rtg = getMethodRating(md, theTypes);
                if (rtg > 0) {
                    if (matches == Collections.EMPTY_LIST) matches = new ArrayList();
                    matches.add(md);
                }
            }
        return matches;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public List<JavaDecl> getCompatibleMethodsDeep(String aName, JavaDecl theTypes[])
    {
        // Search this class and superclasses for compatible method
        List<JavaDecl> matches = Collections.EMPTY_LIST;
        for (JavaDeclClass cls = this; cls != null; cls = cls.getSuper()) {
            List<JavaDecl> decls = cls.getCompatibleMethods(aName, theTypes);
            if (decls.size() > 0) {
                if (matches == Collections.EMPTY_LIST) matches = decls;
                else matches.addAll(decls);
            }
        }
        return matches;
    }

    /**
     * Returns a compatibile method for given name and param types.
     */
    public List<JavaDecl> getCompatibleMethodsAll(String aName, JavaDecl theTypes[])
    {
        // Search this class and superclasses for compatible method
        List<JavaDecl> matches = Collections.EMPTY_LIST;
        List<JavaDecl> decls = getCompatibleMethodsDeep(aName, theTypes);
        if (decls.size() > 0) {
            if (matches == Collections.EMPTY_LIST) matches = decls;
            else matches.addAll(decls);
        }

        // Search this class and superclasses for compatible interface
        for (JavaDeclClass cls = this; cls != null; cls = cls.getSuper()) {
            for (JavaDeclClass infc : cls.getInterfaces()) {
                decls = infc.getCompatibleMethodsAll(aName, theTypes);
                if (decls.size() > 0) {
                    if (matches == Collections.EMPTY_LIST) matches = decls;
                    else matches.addAll(decls);
                }
            }
        }

        // If this class is Interface, check Object
        if (isInterface()) {
            JavaDeclClass objDecl = getClassDecl(Object.class);
            decls = objDecl.getCompatibleMethodsDeep(aName, theTypes);
            if (decls.size() > 0) {
                if (matches == Collections.EMPTY_LIST) matches = decls;
                else matches.addAll(decls);
            }
        }

        // Remove supers and duplicates
        for (int i = 0; i < matches.size(); i++) {
            JavaDecl decl = matches.get(i);
            for (JavaDecl sd = decl.getSuper(); sd != null; sd = sd.getSuper()) matches.remove(sd);
            for (int j = i + 1; j < matches.size(); j++) if (matches.get(j) == decl) matches.remove(j);
        }

        // Return null since compatible method not found
        return matches;
    }

    /**
     * Returns whether decl class types are equal.
     */
    public boolean isClassTypesEqual(JavaDecl theTypes0[], JavaDecl theTypes1[])
    {
        int len = theTypes0.length;
        if (theTypes1.length != len) return false;
        for (int i = 0; i < len; i++) {
            JavaDecl ct0 = theTypes0[i];
            if (ct0 != null) ct0 = ct0.getClassType();
            JavaDecl ct1 = theTypes1[i];
            if (ct1 != null) ct1 = ct1.getClassType();
            if (ct0 != ct1)
                return false;
        }
        return true;
    }

    /**
     * Returns a rating of a method for given possible arg classes.
     */
    private int getMethodRating(JavaDecl aMeth, JavaDecl theTypes[])
    {
        // Handle VarArg methods special
        if (aMeth.isVarArgs()) return getMethodRatingVarArgs(aMeth, theTypes);

        // Get method param types and length (just return if given arg count doesn't match)
        JavaDecl paramTypes[] = aMeth.getParamTypes();
        int plen = paramTypes.length, rating = 0;
        if (theTypes.length != plen)
            return 0;
        if (plen == 0)
            return 1000;

        // Iterate over classes and add score based on matching classes
        // This is a punt - need to groc the docs on this: https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html
        for (int i = 0, iMax = plen; i < iMax; i++) {
            JavaDecl cls1 = paramTypes[i].getClassType(), cls2 = theTypes[i];
            if (cls2 != null) cls2 = cls2.getClassType();
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
    private int getMethodRatingVarArgs(JavaDecl aMeth, JavaDecl theTypes[])
    {
        // Get method param types and length (just return if given arg count is insufficient)
        JavaDecl paramTypes[] = aMeth.getParamTypes();
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
        JavaDecl varArgArrayType = paramTypes[vind];
        JavaDecl varArgType = varArgArrayType.getArrayItemType();

        // If only one arg and it is of array type, add 1000
        JavaDecl argType = theTypes.length == plen ? theTypes[vind] : null;
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
     * Returns the lambda method.
     */
    public JavaDecl getLambdaMethod(int argCount)
    {
        List<JavaDecl> mdecls = getMethods();
        for (JavaDecl jd : mdecls)
            if (jd.getParamCount() == argCount)
                return jd;
        return null;
    }

    /**
     * Returns the decl for constructor.
     */
    public JavaDecl getConstructorDecl(Constructor aConstr)
    {
        String id = ResolverUtils.getId(aConstr);
        JavaDecl decl = getConstructorDecl(id);
        if (decl == null) return null;
        int mods = aConstr.getModifiers();
        if (mods != decl.getModifiers()) return null;
        return decl;
    }

    /**
     * Returns the Constructor decl for id string.
     */
    public JavaDecl getConstructorDecl(String anId)
    {
        List<JavaDecl> cdecls = getConstructors();
        for (JavaDecl jd : cdecls) if (jd.getId().equals(anId)) return jd;
        return null;
    }

    /**
     * Returns a constructor decl for parameter types.
     */
    public JavaDecl getConstructorDecl(JavaDecl theTypes[])
    {
        List<JavaDecl> cdecls = getConstructors();
        for (JavaDecl jd : cdecls)
            if (isClassTypesEqual(jd.getParamTypes(), theTypes))
                return jd;
        return null;
    }

    /**
     * Returns a constructor decl for parameter types.
     */
    public JavaDecl getConstructorDeclDeep(JavaDecl theTypes[])
    {
        JavaDecl decl = getConstructorDecl(theTypes);
        if (decl == null && _superClassDecl != null)
            decl = _superClassDecl.getConstructorDeclDeep(theTypes);
        return decl;
    }

    /**
     * Returns a Class decl for inner class simple name.
     */
    public JavaDeclClass getClassDecl(String aName)
    {
        List<JavaDeclClass> icdecls = getClasses();
        for (JavaDeclClass jd : icdecls)
            if (jd.getSimpleName().equals(aName))
                return jd;
        return null;
    }

    /**
     * Returns a Class decl for inner class name.
     */
    public JavaDeclClass getClassDeclDeep(String aName)
    {
        JavaDeclClass decl = getClassDecl(aName);
        if (decl == null && _superClassDecl != null)
            decl = _superClassDecl.getClassDeclDeep(aName);
        return decl;
    }

    /**
     * Returns a TypeVar decl for inner class name.
     */
    public JavaDecl getTypeVar(String aName)
    {
        List<JavaDecl> tvdecls = getTypeVars2();
        for (JavaDecl jd : tvdecls)
            if (jd.getName().equals(aName))
                return jd;
        return null;
    }

    /**
     * Returns a TypeVar decl for inner class name.
     */
    public int getTypeVarIndex(String aName)
    {
        List<JavaDecl> tvdecls = getTypeVars2();
        for (int i = 0, iMax = tvdecls.size(); i < iMax; i++) {
            JavaDecl jd = tvdecls.get(i);
            if (jd.getName().equals(aName))
                return i;
        }
        return -1;
    }

    /**
     * Adds a decl.
     */
    public void addDecl(JavaDecl aDecl)
    {
        DeclType type = aDecl.getType();
        switch (type) {
            case Field: _fieldDecls.add(aDecl); break;
            case Method: _methDecls.add(aDecl); break;
            case Constructor: _constrDecls.add(aDecl); break;
            case Class: _innerClassDecls.add((JavaDeclClass) aDecl); break;
            case TypeVar: _typeVarDecls.add(aDecl); break;
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
            case Class: _innerClassDecls.remove(aDecl); break;
            case TypeVar: _typeVarDecls.remove(aDecl); break;
            default: throw new RuntimeException("JavaDeclHpr.removeDecl: Invalid type " + type);
        }
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return "ClassDecl { ClassName=" + getClassName() + " }";
    }

    // Bogus class to get length
    private static class Array { public int length; }

    private static Field getLenField()
    {
        try { return Array.class.getField("length"); }
        catch (Exception e) { return null; }
    }
}