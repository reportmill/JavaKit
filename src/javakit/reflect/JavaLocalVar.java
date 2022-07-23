/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.reflect;
import javakit.parse.JType;
import javakit.parse.JVarDecl;
import javakit.resolver.Resolver;

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
        _evalType = varDeclType != null ? varDeclType.getDecl() : getClassDecl(Object.class); // Can happen for Lambdas
    }
}
