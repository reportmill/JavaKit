/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;

/**
 * This class represents a local variable definition in a statement block, for-loop declaration or catch block.
 */
public class JavaLocalVar extends JavaDecl {

    /**
     * Constructor.
     */
    public JavaLocalVar(Resolver anOwner, String aName, JavaType aType, String anId)
    {
        super(anOwner, DeclType.VarDecl);

        // Set Id, Name, SimpleName, EvalType
        _id = anId;
        _name = _simpleName = aName;
        _evalType = aType;
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
