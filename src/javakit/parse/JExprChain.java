/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaType;
import snap.util.ClassUtils;
import java.lang.reflect.Field;
import java.util.*;

/**
 * A class to represent a chain of expressions.
 */
public class JExprChain extends JExpr {

    /**
     * Creates a new JExprChain.
     */
    public JExprChain()
    {
    }

    /**
     * Creates a new JExprChain for given parts.
     */
    public JExprChain(JExpr... theExprs)
    {
        for (JExpr expr : theExprs) addExpr(expr);
    }

    /**
     * Returns the number of expressions.
     */
    public int getExprCount()
    {
        return _children.size();
    }

    /**
     * Returns the individual expression at given index.
     */
    public JExpr getExpr(int anIndex)
    {
        return (JExpr) _children.get(anIndex);
    }

    /**
     * Returns the individual expression at given index.
     */
    public JExpr getExprLast()
    {
        int pc = getExprCount();
        return pc > 0 ? getExpr(pc - 1) : null;
    }

    /**
     * Returns the expressions list.
     */
    public List<JExpr> getExpressions()
    {
        return (List) _children;
    }

    /**
     * Adds a expression to this JExprChain.
     */
    public void addExpr(JExpr anExpr)
    {
        addChild(anExpr, getChildCount());
    }

    /**
     * Tries to resolve the class declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        JExpr p = getExprLast();
        return p != null ? p.getDecl() : null;
    }

    /**
     * Override to resolve names in chain.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        // Get node info
        String name = aNode.getName();
        boolean isId = aNode instanceof JExprId, isType = !isId;
        if (isType)
            return super.getDeclImpl(aNode);

        // Get parent expression - if not found (first in chain) do normal version
        JExprId id = (JExprId) aNode;
        JExpr parExpr = id.getParentExpr();
        if (parExpr == null)
            return super.getDeclImpl(aNode);

        // Get parent declaration
        JavaDecl parDecl = parExpr.getDecl();
        if (parDecl == null) {
            System.err.println("JExprChain.resolve: No parent decl for " + getName() + " in " + getName());
            return null;
        }

        // Handle Parent is Package: Look for package sub-package or package class
        if (parDecl.isPackage()) {
            String pname = parDecl.getPackageName(), cpath = pname + '.' + name;
            JavaDecl decl = getJavaDecl(cpath);
            if (decl != null)
                return decl;
        }

        // Handle Parent is Class: Look for ".this", ".class", static field or inner class
        else if (parDecl.isClass()) {
            Class pclass = parExpr.getEvalClass();

            // Handle Class.this: Return parent declaration
            if (name.equals("this"))
                return parDecl; // was FieldName

            // Handle Class.class: Return ParamType for Class<T>
            if (name.equals("class")) {
                JavaDecl cdecl = getJavaDecl(Class.class);
                return cdecl.getParamTypeDecl((JavaType) parDecl);
            }

            // Handle inner class
            Class cls = pclass != null ? ClassUtils.getInnerClassForName(pclass, name) : null;
            if (cls != null)
                return getJavaDecl(cls);

            // Handle Field
            Field field = pclass != null ? ClassUtils.getFieldForName(pclass, name) : null;
            if (field != null) // && Modifier.isStatic(field.getModifiers()))
                return getJavaDecl(field); // was FieldName
        }

        // Handle any parent with class: Look for field
        else if (parExpr.getEvalType() != null) {
            JavaType pdecl = parExpr.getEvalType();

            if (pdecl.isArray() && name.equals("length"))
                return getJavaDecl(int.class); // was FieldName;

            if (pdecl.isParamType())
                pdecl = (JavaType) pdecl.getParent();

            if (pdecl.isClass()) {
                JavaClass cdecl = (JavaClass) pdecl;
                JavaDecl fd = cdecl.getFieldDeep(name);
                if (fd != null)
                    return fd;
            }
        }

        // Do normal version
        return super.getDeclImpl(aNode);
    }

    /**
     * Returns the resolved eval type for child node, if this ancestor can.
     */
    protected JavaType getEvalTypeImpl()
    {
        JExpr p = getExprLast();
        return p != null ? p.getEvalType() : null;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        return "ExprChain";
    }

}