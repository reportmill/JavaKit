/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import java.util.*;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import snap.util.ListUtils;

/**
 * A JNode to represent a defined variable.
 * Found in JFieldDecl, JStmtVarDecl, Method/Catch FormalParam(s)
 */
public class JVarDecl extends JNode {

    // The type
    JType _type;

    // The variable name
    JExprId _id;

    // The variable dimension (if defined with variable instead of type)
    int _arrayCount;

    // The initializer
    JExpr _initializer;

    // The array initializer (if array)
    List<JExpr> _arrayInits = Collections.EMPTY_LIST;

    /**
     * Returns whether type is set.
     */
    public boolean isTypeSet()
    {
        return _type != null;
    }

    /**
     * Returns the type.
     */
    public JType getType()
    {
        if (_type != null) return _type;

        // Get parent type from JFieldDecl, JStmtVarDecl
        _type = getParentType();
        if (_type == null) return null;

        // If array count is set, replace with type to account for it
        if (_arrayCount > 0) {
            JType type2 = new JType();
            type2._name = _type._name;
            type2._startToken = type2._endToken = _startToken;
            type2._primitive = _type._primitive;
            type2._arrayCount = _type._arrayCount + _arrayCount;
            _type = type2;
            _type._parent = this;
        }

        // Return type
        return _type;
    }

    /**
     * Sets the type.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the parent type (JFieldDecl, JStmtVarDecl).
     */
    private JType getParentType()
    {
        // Handle parent is Field or VarDecl statement: return type from parent
        JNode par = getParent();
        if (par instanceof JFieldDecl) return ((JFieldDecl) par).getType();
        if (par instanceof JStmtVarDecl) return ((JStmtVarDecl) par).getType();

        // Handle parent is JExprLambda: Get decl for this param and create new type
        if (par instanceof JExprLambda) {
            JExprLambda lmda = (JExprLambda) par;

            // Get decl for this param (resolve if needed)
            JavaDecl meth = lmda.getMethod();
            if (meth == null) return null;
            int ind = ListUtils.indexOfId(lmda.getParams(), this);
            if (ind < 0 || ind >= meth.getParamCount()) return null;
            JavaDecl tdecl = meth.getParamType(ind);
            if (!tdecl.isResolvedType()) {
                JavaDecl ltype = lmda.getDecl();
                tdecl = ltype.getResolvedType(tdecl);
            }

            // Create type for type decl and return
            JType type = new JType();
            type._name = tdecl.getSimpleName();
            type._startToken = type._endToken = _startToken;
            type._decl = tdecl;
            type._primitive = tdecl.isPrimitive();
            type._parent = this;
            return type;
        }

        return null;
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
     * Returns the array count.
     */
    public int getArrayCount()
    {
        return _arrayCount;
    }

    /**
     * Sets the array count.
     */
    public void setArrayCount(int aValue)
    {
        _arrayCount = aValue;
    }

    /**
     * Returns the initializer.
     */
    public JExpr getInitializer()
    {
        return _initializer;
    }

    /**
     * Sets the initializer.
     */
    public void setInitializer(JExpr anExpr)
    {
        replaceChild(_initializer, _initializer = anExpr);
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
     * Returns the declaring class, if field variable.
     */
    public Class getDeclaringClass()
    {
        return getParent() instanceof JFieldDecl ? getParent(JClassDecl.class).getEvalClass() : null;
    }

    /**
     * Tries to resolve the class declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get name - if not set, just bail
        String name = getName();
        if (name == null) return null;

        // If part of a JFieldDecl, get JavaDecl for field
        JNode par = getParent();
        if (par instanceof JFieldDecl) {
            JClassDecl cd = getEnclosingClassDecl();
            if (cd == null) return null;
            JavaClass cdecl = cd.getDecl();
            if (cdecl == null) return null;
            JavaDecl fdecl = cdecl.getField(name);
            return fdecl;
        }

        // Otherwise, return JavaDecl for this JVarDecl
        return getJavaDecl(this);
    }

    /**
     * Override to resolve id node.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        if (aNode == _id) return getDecl();
        return super.getDeclImpl(aNode);
    }

}