/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.lang.reflect.Field;
import java.util.*;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaType;
import javakit.reflect.JavaTypeVariable;
import snap.util.*;

/**
 * A Java member for ClassDecl.
 */
public class JClassDecl extends JMemberDecl {
    // The type of class (Class, Interface, Enum, Annotation)
    ClassType _classType = ClassType.Class;

    // TypeVars
    List<JTypeVar> _typeVars;

    // The extends list
    List<JType> _extendsTypes = new ArrayList();

    // The implements list
    List<JType> _implementsTypes = new ArrayList();

    // The list of fields, methods, enums annotations and child classes
    List<JMemberDecl> _members = new ArrayList();

    // The enum constants (if ClassType Enum)
    List<JEnumConst> _enumConstants = new ArrayList();

    // The field declarations
    JFieldDecl _fieldDecls[];

    // The constructor declarations
    JConstrDecl _constrDecls[];

    // The method declarations
    JMethodDecl _methodDecls[];

    // An array of class declarations that are members of this class
    JClassDecl _classDecls[];

    // The class type
    public enum ClassType {Class, Interface, Enum, Annotation}

    /**
     * Returns the simple name.
     */
    public String getSimpleName()
    {
        return getName();
    }

    /**
     * Returns the JTypeVar(s).
     */
    public List<JTypeVar> getTypeVars()
    {
        return _typeVars;
    }

    /**
     * Sets the JTypeVar(s).
     */
    public void setTypeVars(List<JTypeVar> theTVs)
    {
        if (_typeVars != null) for (JNode n : _typeVars) removeChild(n);
        _typeVars = theTVs;
        if (_typeVars != null) for (JNode n : _typeVars) addChild(n, -1);
    }

    /**
     * Returns the extends list.
     */
    public List<JType> getExtendsTypes()
    {
        return _extendsTypes;
    }

    /**
     * Returns the extends list.
     */
    public void addExtendsType(JType aType)
    {
        _extendsTypes.add(aType);
        addChild(aType, -1);
    }

    /**
     * Returns the implements list.
     */
    public List<JType> getImplementsTypes()
    {
        return _implementsTypes;
    }

    /**
     * Returns the implements list.
     */
    public void addImplementsType(JType aType)
    {
        _implementsTypes.add(aType);
        addChild(aType, -1);
    }

    /**
     * Returns the list of enum constants.
     */
    public List<JEnumConst> getEnumConstants()
    {
        return _enumConstants;
    }

    /**
     * Adds an enum constant.
     */
    public void addEnumConstant(JEnumConst anEC)
    {
        _enumConstants.add(anEC);
        addChild(anEC, -1);
    }

    /**
     * Returns the superclass.
     */
    public Class getSuperClass()
    {
        Class sc = _extendsTypes.size() > 0 ? _extendsTypes.get(0).getEvalClass() : null;
        return sc != null ? sc : Object.class;
    }

    /**
     * Returns implemented interfaces.
     */
    public Class[] getInterfaces()
    {
        List<Class> classes = new ArrayList();
        for (JType interfType : _implementsTypes) {
            Class iclass = interfType.getEvalClass();
            if (iclass != null)
                classes.add(iclass);
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Returns the class type.
     */
    public ClassType getClassType()
    {
        return _classType;
    }

    /**
     * Sets the class type.
     */
    public void setClassType(ClassType aType)
    {
        _classType = aType;
    }

    /**
     * Returns whether class type is Class.
     */
    public boolean isClass()
    {
        return getClassType() == ClassType.Class;
    }

    /**
     * Returns whether class type is Interface.
     */
    public boolean isInterface()
    {
        return getClassType() == ClassType.Interface;
    }

    /**
     * Returns whether class type is Enum.
     */
    public boolean isEnum()
    {
        return getClassType() == ClassType.Enum;
    }

    /**
     * Returns whether class is anonymous class.
     */
    public boolean isAnonymousClass()
    {
        return getId() == null;
    }

    /**
     * Returns the list of member declarations.
     */
    public List<JMemberDecl> getMemberDecls()
    {
        return _members;
    }

    /**
     * Adds a member declaration.
     */
    public void addMemberDecl(JMemberDecl aDecl)
    {
        _members.add(aDecl);
        addChild(aDecl, -1);
    }

    /**
     * Sets the list of member declarations.
     */
    public void setMemberDecls(List<JMemberDecl> theMDs)
    {
        for (JMemberDecl md : _members) removeChild(md);
        _members = theMDs;
        for (JMemberDecl md : _members) addChild(md, -1);
    }

    /**
     * Returns the class field declarations.
     */
    public JFieldDecl[] getFieldDecls()
    {
        if (_fieldDecls != null) return _fieldDecls;
        List<JFieldDecl> fds = new ArrayList();
        for (JMemberDecl member : _members)
            if (member instanceof JFieldDecl)
                fds.add((JFieldDecl) member);
        return _fieldDecls = fds.toArray(new JFieldDecl[fds.size()]);
    }

    /**
     * Returns the class constructor declarations.
     */
    public JConstrDecl[] getConstructorDecls()
    {
        if (_constrDecls != null) return _constrDecls;
        List<JConstrDecl> cds = new ArrayList();
        for (JMemberDecl member : _members)
            if (member instanceof JConstrDecl)
                cds.add((JConstrDecl) member);
        return _constrDecls = cds.toArray(new JConstrDecl[cds.size()]);
    }

    /**
     * Returns the JConstructorDecl for given name.
     */
    public JConstrDecl getConstructorDecl(String aName, Class theClasses[])
    {
        for (JConstrDecl cd : getConstructorDecls())
            if (cd.getName().equals(aName))
                return cd;
        return null;
    }

    /**
     * Returns the class method declarations.
     */
    public JMethodDecl[] getMethodDecls()
    {
        if (_methodDecls != null) return _methodDecls;
        List<JMethodDecl> mds = new ArrayList();
        for (JMemberDecl member : _members)
            if (member instanceof JMethodDecl && !(member instanceof JConstrDecl))
                mds.add((JMethodDecl) member);
        return _methodDecls = mds.toArray(new JMethodDecl[mds.size()]);
    }

    /**
     * Returns the JMethodDecl for given name.
     */
    public JMethodDecl[] getMethodDecls(String aName, boolean isStatic)
    {
        List<JMethodDecl> mdecls = new ArrayList();
        for (JMethodDecl md : getMethodDecls())
            if (md.getName().equals(aName) && md.getMods().isStatic() == isStatic)
                mdecls.add(md);
        return mdecls.toArray(new JMethodDecl[mdecls.size()]);
    }

    /**
     * Returns the JMethodDecl for given name.
     */
    public JMethodDecl getMethodDecl(String aName, Class theClasses[])
    {
        for (JMethodDecl md : getMethodDecls())
            if (md.getName().equals(aName))
                return md;
        return null;
    }

    /**
     * Returns the class constructor declarations.
     */
    public JClassDecl[] getClassDecls()
    {
        if (_classDecls != null) return _classDecls;
        List<JClassDecl> cds = new ArrayList();
        for (JMemberDecl mbr : _members) getClassDecls(mbr, cds);
        return _classDecls = cds.toArray(new JClassDecl[cds.size()]); // Return class declarations
    }

    /**
     * Returns the class constructor declarations.
     */
    private void getClassDecls(JNode aNode, List<JClassDecl> theCDs)
    {
        if (aNode instanceof JClassDecl)
            theCDs.add((JClassDecl) aNode);
        else for (JNode c : aNode.getChildren())
            getClassDecls(c, theCDs);
    }

    /**
     * Returns the class declaration for class name.
     */
    public JClassDecl getClassDecl(String aName)
    {
        int index = aName.indexOf('.');
        String name = index > 0 ? aName.substring(0, index) : aName;
        String remainder = index >= 0 ? aName.substring(index + 1) : null;
        for (JClassDecl cd : getClassDecls())
            if (cd.getSimpleName().equals(name))
                return remainder != null ? cd.getClassDecl(remainder) : cd;
        return null;
    }

    /**
     * Returns the simple name.
     */
    protected String getNameImpl()
    {
        // If enclosing class, see if we are inner class
        JClassDecl ecd = getEnclosingClassDecl();
        if (ecd != null) {
            JClassDecl classDecls[] = ecd.getClassDecls();
            for (int i = 0, iMax = classDecls.length, j = 1; i < iMax; i++) {
                JClassDecl cd = classDecls[i];
                if (cd == this) return Integer.toString(j);
                if (cd.isAnonymousClass()) j++;
            }
        }

        // Return null since not found
        System.err.println("JClassDecl.createName: Name not found");
        return null;
    }

    /**
     * Returns the class declaration.
     */
    public JavaClass getDecl()
    {
        return (JavaClass) super.getDecl();
    }

    /**
     * Returns the class declaration.
     */
    protected JavaDecl getDeclImpl()
    {
        // If enclosing class declaration, return ThatClassName$ThisName, otherwise return JFile.Name
        String className = getName();

        // If enclosing class, get name from it
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        if (enclosingClassDecl != null) {
            String enclosingClassName = enclosingClassDecl.getEvalClassName();
            if (enclosingClassName != null)
                className = enclosingClassName + '$' + className;
        }

        // Otherwise get full name from file
        else {
            JFile jfile = getFile();
            String packageName = jfile.getPackageName();
            if (packageName != null && packageName.length() > 0)
                className = packageName + '.' + className;
        }

        // Return class name
        return className != null ? getJavaDecl(className) : null;
    }

    /**
     * Override to check field declarations for id.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        // If class id, return class declaration
        if (aNode == _id)
            return getDecl();

        // Handle JType in Extends or Implements lists: forward on
        if (aNode instanceof JType) {

            // Get parent of nested type
            JType type = (JType) aNode;
            JType parentType = type;
            while (parentType.getParent() instanceof JType)
                parentType = (JType) parentType.getParent();

            // If parent of nested type is this JClassDecl, either check for TypeVar or forward to file
            if (parentType.getParent() == this) {

                // Check for TypeVar
                if (type.getParent() instanceof JType) {
                    JType par = (JType) type.getParent();
                    JavaType baseType = par.getBaseDecl();
                    if (baseType instanceof JavaClass) {
                        JavaClass baseClass = (JavaClass) baseType;
                        String typeName = type.getName();
                        JavaDecl typeVarType = baseClass.getTypeVarForName(typeName);
                        if (typeVarType != null)
                            return typeVarType;
                    }
                }

                // Forward to file
                return super.getDeclImpl(aNode);
            }
        }

        // If it's "this", set class and return ClassField
        String name = aNode.getName();
        boolean isId = aNode instanceof JExprId, isType = !isId;
        if (name.equals("this"))
            return getDecl();

        // If it's "super", set class and return ClassField
        if (name.equals("super")) {
            Class<?> superClass = getSuperClass();
            return superClass != null ? getJavaClassForClass(superClass) : null;
        }

        // Iterate over fields and return declaration if found
        if (isId) {
            JFieldDecl[] fieldDecls = getFieldDecls();
            for (JFieldDecl fieldDecl : fieldDecls) {
                List<JVarDecl> fieldVarDecls = fieldDecl.getVarDecls();
                for (JVarDecl fieldVarDecl : fieldVarDecls)
                    if (SnapUtils.equals(fieldVarDecl.getName(), name))
                        return fieldVarDecl.getDecl();
            }
        }

        // Iterate over enum constants
        if (isId && isEnum()) {
            List<JEnumConst> enumConstants = getEnumConstants();
            for (JEnumConst enumConst : enumConstants) {
                if (name.equals(enumConst.getName()))
                    return enumConst.getDecl();
            }
        }

        // See if it's a field reference from superclass
        Class superClass = getSuperClass();
        if (superClass != null) {
            Field field = ClassUtils.getFieldForName(superClass, name);
            if (field != null)
                return getJavaDecl(field);
        }

        // Check interfaces:  Not sure what's going on here
        if (isId) {
            List<JType> implementsTypes = getImplementsTypes();
            for (JType implementsType : implementsTypes) {
                Class interf = implementsType.getEvalClass();
                Field field2 = interf != null ? ClassUtils.getFieldForName(interf, name) : null;
                if (field2 != null)
                    return getJavaDecl(field2);
            }
        }

        // Look for JTypeVar for given name
        JTypeVar typeVar = getTypeVar(name);
        if (typeVar != null)
            return typeVar.getDecl();

        // Look for InnerClass of given name
        Class evalClass = getEvalClass();
        if (evalClass != null) {
            Class innerClass = ClassUtils.getInnerClassForName(evalClass, name);
            if (innerClass != null)
                return getJavaDecl(innerClass);
        }

        // Do normal version
        return super.getDeclImpl(aNode);
    }

    /**
     * Override to resolve Decl.EvalType from ParentExpr.EvalType.
     */
    protected JavaType getEvalTypeImpl(JNode aNode)
    {
        // Handle JType: See if class decl can resolve
        if (aNode instanceof JType) {

            // Get type
            JType type = (JType) aNode;
            JavaType typeType = type.getDecl();
            JavaType evalType = typeType.getEvalType();

            // If eval type is TypeVar, see if it corresponds to this class
            if (evalType instanceof JavaTypeVariable) {
                JavaClass javaClass = getDecl();
                JavaType evalType2 = javaClass.getResolvedType(evalType);
                if (evalType2 != evalType.getEvalType())
                    return evalType2;
            }
        }

        // Do normal version
        return super.getEvalTypeImpl(aNode);
    }

    /**
     * Returns a variable with given name.
     */
    public List<JVarDecl> getVarDecls(String aPrefix, List<JVarDecl> theVDs)
    {
        // Iterate over statements and see if any JStmtVarDecl contains variable with that name
        for (JMemberDecl memberDecl : _members) {
            if (memberDecl instanceof JFieldDecl) {
                JFieldDecl field = (JFieldDecl) memberDecl;
                List<JVarDecl> fieldVarDecls = field.getVarDecls();
                for (JVarDecl fieldVarDecl : fieldVarDecls)
                    if (StringUtils.startsWithIC(fieldVarDecl.getName(), aPrefix))
                        theVDs.add(fieldVarDecl);
            }
        }

        // Do normal version
        return super.getVarDecls(aPrefix, theVDs);
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        switch (getClassType()) {
            case Interface: return "InterfaceDecl";
            case Enum: return "EnumDecl";
            case Annotation: return "AnnotationDecl";
            default: return "ClassDecl";
        }
    }
}