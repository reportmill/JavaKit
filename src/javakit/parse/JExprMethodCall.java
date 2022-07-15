package javakit.parse;

import java.util.List;

/**
 * This class represents a method call in code.
 */
public class JExprMethodCall extends JExpr {

    // The identifier
    JExprId _id;

    // The args
    List<JExpr> _args;

    /**
     * Creates a new method call.
     */
    public JExprMethodCall()
    {
    }

    /**
     * Creates a new method call for given identifier (method name) and arg list.
     */
    public JExprMethodCall(JExprId anId, List theArgs)
    {
        setId(anId);
        setArgs(theArgs);
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
        if (_id == null) addChild(_id = anId, 0);
        else replaceChild(_id, _id = anId);
        if (_id != null) setName(_id.getName());
    }

    /**
     * Returns the number of arguments.
     */
    public int getArgCount()
    {
        return _args.size();
    }

    /**
     * Returns the individual argument at index.
     */
    public JExpr getArg(int anIndex)
    {
        return _args.get(anIndex);
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
     * Returns the arg classes.
     */
    public Class[] getArgClasses()
    {
        List<JExpr> args = getArgs();
        Class classes[] = new Class[args.size()];
        for (int i = 0, iMax = args.size(); i < iMax; i++) {
            JExpr arg = args.get(i);
            classes[i] = arg != null ? arg.getEvalClass() : null;
        }
        return classes;
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
     * Tries to resolve the method declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get method name and arg types
        String name = getName();
        JavaDecl argTypes[] = getArgEvalTypes();

        // Get scope node class type and search for compatible method for name and arg types
        JNode scopeNode = getScopeNode();
        if (scopeNode == null) return null;
        JavaDecl sndecl = scopeNode.getEvalType();
        if (sndecl == null) return null;
        JavaDeclClass snct = sndecl.getClassType();
        JavaDecl decl = snct.getCompatibleMethodAll(name, argTypes);
        if (decl != null)
            return decl;

        // If scope node is class and not static, go up parent classes
        while (scopeNode instanceof JClassDecl && !snct.isStatic()) {
            scopeNode = scopeNode.getEnclosingClassDecl();
            if (scopeNode == null) break;
            sndecl = scopeNode.getDecl();
            if (sndecl == null) break;
            snct = sndecl.getClassType();
            if (snct == null) break;
            decl = snct.getCompatibleMethodAll(name, argTypes);
            if (decl != null)
                return decl;
        }

        // See if method is from static import
        decl = getFile().getImportClassMember(name, argTypes);
        if (decl != null && decl.isMethod())
            return decl;

        // Return null since not found
        return null;
    }

    /**
     * Override to handle method name.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        if (aNode == _id) return getDecl();
        return super.getDeclImpl(aNode);
    }

    /**
     * Override to resolve Decl.EvalType from ParentExpr.EvalType.
     */
    protected JavaDecl getEvalTypeImpl(JNode aNode)
    {
        // Handle MethodCall id
        if (aNode == _id) return getEvalType();

            // Handle this node
        else if (aNode == this) {
            JavaDecl mdecl = aNode.getDecl();
            if (mdecl == null) return null;
            JavaDecl etype = mdecl.getEvalType();
            JavaDecl scopeType = getScopeNodeEvalType();

            // If eval type is TypeVar, try to resolve
            if (etype.isTypeVar()) {
                String name = etype.getName();

                // See if TypeVar can be resolved by method
                JavaDecl resolvedDecl = getResolvedTypeVarForMethod(name, mdecl);
                if (resolvedDecl != null)
                    return resolvedDecl;

                // See if TypeVar can be resolved by ScopeNode.Type
                resolvedDecl = scopeType.getResolvedType(etype);
                if (resolvedDecl != null)
                    return resolvedDecl;

                // Otherwise, just return TypeVar default
                return etype.getEvalType();
            }
        }

        // Do normal version
        return super.getEvalTypeImpl(aNode);
    }

    /**
     * Resolves a TypeVar for given method decl and arg types.
     */
    public JavaDecl getResolvedTypeVarForMethod(String aName, JavaDecl aMethDecl)
    {
        // If no type var for given name, just return
        if (aMethDecl.getTypeVar(aName) == null)
            return null;

        // Iterate over method arg types to see if any can resolve the type var
        JavaDecl argTypes[] = aMethDecl.getParamTypes();
        for (int i = 0, iMax = argTypes.length; i < iMax; i++) {
            JavaDecl arg = argTypes[i];

            // If method arg is TypeVar with same name, return arg expr eval type (if not null)
            if (arg.isTypeVar() && arg.getName().equals(aName)) {
                JExpr argExpr = getArg(i);
                if (argExpr == null) continue;
                JavaDecl argEvalType = argExpr.getEvalType();
                return argEvalType;
            }

            // If method arg is ParamType with matching param TypeVar,
            if (arg.isParamType()) {

                // Iterate over ParamType params
                for (JavaDecl parg : arg.getParamTypes()) {

                    // If TypeVar with matching name, see if arg eval type can resolve
                    if (parg.isTypeVar() && parg.getName().equals(aName)) {

                        // Get arg expr and eval type
                        JExpr argExpr = getArg(i);
                        if (argExpr == null) continue;
                        JavaDecl argEvalType = argExpr.getEvalType();
                        if (argEvalType == null) continue;
                        if (argEvalType.isParamType())
                            return argEvalType.getParamTypes()[0];
                    }
                }
            }
        }

        // Return null since TypeVar name couldn't be resolved by method args
        return null;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        return "MethodCall";
    }

}