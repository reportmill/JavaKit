/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import javakit.parse.JClassDecl;
import javakit.parse.JMethodDecl;
import javakit.parse.JType;
import javakit.parse.JVarDecl;
import java.util.*;

/**
 * This class updates a JavaClass from JClassDecl.
 */
public class JavaClassUpdaterDecl extends JavaClassUpdater {

    // The ClassDecl
    private JClassDecl  _classDecl;

    /**
     * Constructor.
     */
    public JavaClassUpdaterDecl(JavaClass aClass, JClassDecl aClassDecl)
    {
        super(aClass);
        _classDecl = aClassDecl;
    }

    /**
     * Updates JavaDecls. Returns whether the decls changed since last update.
     */
    @Override
    public boolean updateDeclsImpl() throws SecurityException
    {
        // If first time, set decls
        if (_javaClass._fieldDecls == null)
            _javaClass._fieldDecls = new ArrayList<>();

        // Update interfaces
        //updateInterfaces();
        _javaClass._interfaces = new JavaClass[0];

        // Update type variables
        //updateTypeVariables(realClass, removedDecls);
        _javaClass._typeVarDecls = Collections.EMPTY_LIST;

        // Update inner classes
        //updateInnerClasses();
        _javaClass._innerClasses = Collections.EMPTY_LIST;

        // Update fields
        //updateFields();

        // Update methods
        updateMethods();

        // Update constructors
        //updateConstructors();
        _javaClass._constrDecls = Collections.EMPTY_LIST;

        // Return
        return true;
    }

    /**
     * Updates methods.
     */
    private void updateMethods() throws SecurityException
    {
        // Get Methods
        JMethodDecl[] methodDecls = _classDecl.getMethodDecls();
        JavaMethod.MethodBuilder mb = new JavaMethod.MethodBuilder();
        mb.init(_resolver, _javaClass.getClassName());

        // Add JavaDecl for each declared method - also make sure return/parameter types are in refs
        for (JMethodDecl methodDecl : methodDecls) {

            // Get/set name
            String methodName = methodDecl.getName();
            mb.name(methodName);

            // Get/set param types
            List<JVarDecl> paramsList = methodDecl.getParameters();
            JavaType[] params = new JavaType[paramsList.size()];
            for (int i = 0, iMax = paramsList.size(); i < iMax; i++) {
                JVarDecl paramDecl = paramsList.get(i);
                JavaType paramType = paramDecl.getEvalType();
                params[i] = paramType;
            }
            mb.paramTypes(params);

            // Get/set return type
            JType returnTypeDecl = methodDecl.getType();
            JavaType returnType = returnTypeDecl != null ? returnTypeDecl.getDecl() : null;
            if (returnType != null)
                mb.returnType(returnType);

            // Add to builder list
            mb.save();
        }

        // Set Methods
        JavaMethod[] methods = mb.buildAll();
        _javaClass._methDecls = Arrays.asList(methods);
    }
}
