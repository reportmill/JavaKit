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
    public JavaLocalVar(Resolver anOwner, JavaDecl aPar, JVarDecl aVarDecl)
    {
        super(anOwner, aPar, aVarDecl);

        _type = DeclType.VarDecl;
        _name = _simpleName = aVarDecl.getName();
        JType varDeclType = aVarDecl.getType();
        _evalType = varDeclType != null ? varDeclType.getDecl() : getJavaClassForClass(Object.class); // Can happen for Lambdas
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        StringBuffer sb = new StringBuffer(getSimpleName());

        JavaType evalType = getEvalType();
        if (evalType != null)
            sb.append(" : ").append(evalType.getSimpleName());
        String className = getClassSimpleName();
        if (className != null)
            sb.append(" - ").append(className);

        // Return string
        return sb.toString();
    }
}
