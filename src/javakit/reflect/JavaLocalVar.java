/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.parse.JType;
import javakit.parse.JVarDecl;

/**
 * This class represents a local variable in a statement block, for-loop declaration or catch block.
 */
public class JavaLocalVar extends JavaDecl {

    /**
     * Constructor.
     */
    public JavaLocalVar(Resolver anOwner, JVarDecl aVarDecl)
    {
        super(anOwner);

        _type = DeclType.VarDecl;
        _name = _simpleName = aVarDecl.getName();

        // Set EvalType
        JType varDeclType = aVarDecl.getType();
        _evalType = varDeclType != null ? varDeclType.getDecl() : getJavaClassForClass(Object.class); // Can happen for Lambdas
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        // Get SimpleName, EvalType.SimpleName
        String simpleName = getSimpleName();
        JavaType evalType = getEvalType();
        String evalTypeName = evalType != null ? evalType.getSimpleName() : null;

        // Construct string: SimpleName : EvalType.SimpleName
        StringBuffer sb = new StringBuffer(simpleName);
        if (evalTypeName != null)
            sb.append(" : ").append(evalTypeName);

        // Return
        return sb.toString();
    }
}
