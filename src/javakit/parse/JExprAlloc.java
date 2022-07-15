/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import java.util.*;

import snap.util.ArrayUtils;

/**
 * A JExpr subclass for Allocation expressions.
 */
public class JExprAlloc extends JExpr {

    // The Allocation type
    JType _type;

    // The allocation args
    List<JExpr> _args = Collections.EMPTY_LIST;

    // The dimensions expression, if array
    JExpr _arrayDims;

    // The array init expressionss, if array
    List<JExpr> _arrayInits = Collections.EMPTY_LIST;

    // The allocation JClassDecl
    JClassDecl _classDecl;

    /**
     * Returns the allocation JType.
     */
    public JType getType()
    {
        return _type;
    }

    /**
     * Sets the allocation JType.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the allocation arguments.
     */
    public List<JExpr> getArgs()
    {
        return _args;
    }

    /**
     * Sets the allocation arguments.
     */
    public void setArgs(List<JExpr> theArgs)
    {
        if (_args != null) for (JExpr arg : _args) removeChild(arg);
        _args = theArgs;
        if (_args != null) for (JExpr arg : _args) addChild(arg, -1);
    }

    /**
     * Returns the arg eval types.
     */
    public JavaDecl[] getArgEvalTypes()
    {
        List<JExpr> args = getArgs();
        JavaDecl etypes[] = new JavaDecl[args.size()];
        for (int i = 0, iMax = args.size(); i < iMax; i++) {
            JExpr arg = args.get(i);
            etypes[i] = arg != null ? arg.getEvalType() : null;
        }
        return etypes;
    }

    /**
     * Returns the array dimensions.
     */
    public JExpr getArrayDims()
    {
        return _arrayDims;
    }

    /**
     * Sets the array dimension.
     */
    public void setArrayDims(JExpr theDims)
    {
        replaceChild(_arrayDims, _arrayDims = theDims);
    }

    /**
     * Returns the array init expressions, if array.
     */
    public List<JExpr> getArrayInits()
    {
        return _arrayInits;
    }

    /**
     * Sets the array init expressions, if array.
     */
    public void setArrayInits(List<JExpr> theArrayInits)
    {
        if (_arrayInits != null) for (JExpr expr : _arrayInits) removeChild(expr);
        _arrayInits = theArrayInits;
        if (_arrayInits != null) for (JExpr expr : _arrayInits) addChild(expr, -1);
    }

    /**
     * Returns the allocation ClassBodyDecl.
     */
    public JClassDecl getClassDecl()
    {
        return _classDecl;
    }

    /**
     * Sets the allocation ClassBodyDecl.
     */
    public void setClassDecl(JClassDecl aCD)
    {
        replaceChild(_classDecl, _classDecl = aCD);
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        // If array alloc, just return Type decl
        JType type = getType();
        if (_arrayDims != null || _arrayInits.size() > 0)
            return type.getDecl();

        // Get class decl and constructor call arg types
        JavaDecl tdecl = type.getDecl();
        if (tdecl == null) return null;
        JavaDeclClass cdecl = tdecl.getClassType();
        JavaDecl argTypes[] = getArgEvalTypes();

        // If inner class and not static, add implied class type to arg types array
        if (cdecl.isMemberClass() && !cdecl.isStatic())
            argTypes = ArrayUtils.add(argTypes, cdecl.getParent(), 0);

        // Get scope node class type and search for compatible method for name and arg types
        JavaDecl decl = cdecl.getCompatibleConstructor(argTypes);
        if (decl != null)
            return decl;

        // Return null since not found
        return null;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        return "Allocation";
    }

}