/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaType;
import snap.util.ArrayUtils;

/**
 * A Java member for ConstrDecl.
 */
public class JConstrDecl extends JMethodDecl {

    /**
     * Override to get declaration from actual Constructor.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get param types
        JavaType[] ptypes = getParamClassTypesSafe();
        if (ptypes == null) return null; // Can happen if params bogus/editing

        // Get parent JClassDecl and JavaDecl
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        if (enclosingClassDecl == null) return null;
        JavaClass javaClass = enclosingClassDecl.getDecl();
        if (javaClass == null) return null;

        // If inner class and not static, add implied class type to arg types array
        if (javaClass.isMemberClass() && !javaClass.isStatic()) {
            JavaType parentClass = (JavaType) javaClass.getParent();
            ptypes = ArrayUtils.add(ptypes, parentClass, 0);
        }

            // If enum, add implied args types for name (String) and ordinal (int)
        else if (javaClass.isEnum()) {
            ptypes = ArrayUtils.add(ptypes, getJavaClass(String.class), 0);
            ptypes = ArrayUtils.add(ptypes, getJavaClass(int.class), 1);
        }

        // Return Constructor for param types
        return javaClass.getConstructorDecl(ptypes);
    }

    /**
     * Override to check field declarations for id.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        if (aNode == _id) return getDecl();
        return super.getDeclImpl(aNode);
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        return "ConstrDecl";
    }

}