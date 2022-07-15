/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

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
        JavaDecl ptypes[] = getParamClassTypesSafe();
        if (ptypes == null) return null; // Can happen if params bogus/editing

        // Get parent JClassDecl and JavaDecl
        JClassDecl cd = getEnclosingClassDecl();
        if (cd == null) return null;
        JavaDeclClass cdecl = cd.getDecl();
        if (cdecl == null) return null;

        // If inner class and not static, add implied class type to arg types array
        if (cdecl.isMemberClass() && !cdecl.isStatic())
            ptypes = ArrayUtils.add(ptypes, cdecl.getParent(), 0);

            // If enum, add implied args types for name (String) and ordinal (int)
        else if (cdecl.isEnum()) {
            ptypes = ArrayUtils.add(ptypes, getJavaDecl(String.class), 0);
            ptypes = ArrayUtils.add(ptypes, getJavaDecl(int.class), 1);
        }

        // Return Constructor for param types
        return cdecl.getConstructorDecl(ptypes);
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