package javakit.parse;

import java.util.*;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaType;
import snap.util.ListUtils;

/**
 * A JStmt subclass to represent an explicit constructor invocation, like: this(x) or super(y).
 * Found in first line of JContrDecl only.
 */
public class JStmtConstrCall extends JStmt {

    // The identifier
    List<JExprId> _idList = new ArrayList();

    // The args
    List<JExpr> _args;

    /**
     * Returns the list of ids.
     */
    public List<JExprId> getIds()
    {
        return _idList;
    }

    /**
     * Adds an Id.
     */
    public void addId(JExprId anId)
    {
        _idList.add(anId);
        addChild(anId);
    }

    /**
     * Returns the method arguments.
     */
    public List<JExpr> getArgs()
    {
        return _args;
    }

    /**
     * Sets the method arguments.
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
    public JavaType[] getArgEvalTypes()
    {
        List<JExpr> args = getArgs();
        JavaType[] argTypes = new JavaType[args.size()];

        for (int i = 0, iMax = args.size(); i < iMax; i++) {
            JExpr arg = args.get(i);
            argTypes[i] = arg != null ? arg.getEvalType() : null;
        }

        return argTypes;
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get class decl and constructor call arg types
        JClassDecl cd = getEnclosingClassDecl();
        JavaClass cdecl = cd.getDecl();
        if (cdecl == null) return null;
        JavaType[] argTypes = getArgEvalTypes();

        // If Super, switch to super class
        String name = getIds().get(0).getName();
        if (name.equals("super"))
            cdecl = cdecl.getSuper();

        // Get scope node class type and search for compatible method for name and arg types
        JavaDecl decl = cdecl.getCompatibleConstructor(argTypes);
        if (decl != null)
            return decl;

        // Return null since not found
        return null;
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        if (aNode.getParent() == this && aNode instanceof JExprId && ListUtils.containsId(_idList, aNode))
            return getDecl();
        return super.getDeclImpl(aNode);
    }

}