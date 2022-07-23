/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import javakit.reflect.JavaClass;
import javakit.reflect.JavaDecl;

import java.util.List;

/**
 * A JNode for type members: Initializer, TypeDecl, EnumDecl, ConstrDecl, FieldDecl, MedthodDecl, AnnotationDecl.
 * For JavaParseRule: ClassBodyDecl.
 */
public class JMemberDecl extends JNode {

    // The modifiers
    JModifiers _mods;

    // The name identifier
    JExprId _id;

    /**
     * Returns the modifiers.
     */
    public JModifiers getMods()
    {
        if (_mods == null) _mods = new JModifiers();
        return _mods;
    }

    /**
     * Sets the modifiers.
     */
    public void setMods(JModifiers aValue)
    {
        if (_mods == null) addChild(_mods = aValue, 0);
        else replaceChild(_mods, _mods = aValue);
    }

    /**
     * Returns the identifier.
     */
    public JExprId getId()
    {
        return _id;
    }

    /**
     * Sets the identifier.
     */
    public void setId(JExprId anId)
    {
        replaceChild(_id, _id = anId);
        if (_id != null) setName(_id.getName());
    }

    /**
     * Returns the type variables for this member.
     */
    public List<JTypeVar> getTypeVars()
    {
        return null;
    }

    /**
     * Returns the type variable for this member with given name.
     */
    public JTypeVar getTypeVar(String aName)
    {
        List<JTypeVar> tvars = getTypeVars();
        if (tvars == null) return null;
        for (JTypeVar tvar : tvars)
            if (tvar.getName().equals(aName))
                return tvar;
        return null;
    }

    /**
     * Returns the member that this member overrides or implements, if available.
     */
    public JavaDecl getSuperDecl()
    {
        JavaDecl decl = getDecl();
        return decl != null && (decl.isMethod() || decl.isConstructor()) ? decl.getSuper() : null;
    }

    /**
     * Returns whether super declaration is interface.
     */
    public boolean isSuperDeclInterface()
    {
        JavaDecl superDecl = getSuperDecl();
        if (superDecl == null) return false;

        JavaClass javaClass = superDecl.getClassType();
        if (javaClass == null) return false;

        return javaClass.isInterface();
    }

}