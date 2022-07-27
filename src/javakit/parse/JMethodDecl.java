/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import java.util.*;

import javakit.reflect.JavaDecl;
import javakit.reflect.JavaClass;
import javakit.reflect.JavaType;
import snap.util.*;

/**
 * A Java member for MethodDeclaration.
 */
public class JMethodDecl extends JMemberDecl {
    // The type/return-type
    JType _type;

    // Type variables
    List<JTypeVar> _typeVars;

    // The formal parameters
    List<JVarDecl> _params = new ArrayList();

    // The throws names list
    List<JExpr> _throwsNameList = new ArrayList();

    // The statement Block
    JStmtBlock _block;

    /**
     * Returns the field type.
     */
    public JType getType()
    {
        return _type;
    }

    /**
     * Sets the field type.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the method type variables.
     */
    public List<JTypeVar> getTypeVars()
    {
        return _typeVars;
    }

    /**
     * Sets the method type variables.
     */
    public void setTypeVars(List<JTypeVar> theTVs)
    {
        if (_typeVars != null) for (JTypeVar tvar : _typeVars) removeChild(tvar);
        _typeVars = theTVs;
        if (_typeVars != null) for (JTypeVar tvar : _typeVars) addChild(tvar, -1);
    }

    /**
     * Returns the number of formal parameters.
     */
    public int getParamCount()
    {
        return _params.size();
    }

    /**
     * Returns the individual formal parameter at given index.
     */
    public JVarDecl getParam(int anIndex)
    {
        return _params.get(anIndex);
    }

    /**
     * Returns the list of formal parameters.
     */
    public List<JVarDecl> getParameters()
    {
        return _params;
    }

    /**
     * Returns the list of formal parameters.
     */
    public void addParam(JVarDecl aVD)
    {
        if (aVD == null) {
            System.err.println("JMethodDecl.addParam: Add null param!");
            return;
        }
        _params.add(aVD);
        addChild(aVD, -1);
    }

    /**
     * Returns the parameter with given name.
     */
    public JVarDecl getParam(String aName)
    {
        for (JVarDecl vd : _params) if (SnapUtils.equals(vd.getName(), aName)) return vd;
        return null;
    }

    /**
     * Returns the throws list.
     */
    public List<JExpr> getThrowsList()
    {
        return _throwsNameList;
    }

    /**
     * Sets the throws list.
     */
    public void setThrowsList(List<JExpr> theThrows)
    {
        if (_throwsNameList != null) for (JExpr t : _throwsNameList) removeChild(t);
        _throwsNameList = theThrows;
        if (_throwsNameList != null) for (JExpr t : _throwsNameList) addChild(t, -1);
    }

    /**
     * Returns whether statement has a block associated with it.
     */
    public boolean isBlock()
    {
        return true;
    }

    /**
     * Returns the block.
     */
    public JStmtBlock getBlock()
    {
        return _block;
    }

    /**
     * Sets the block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        replaceChild(_block, _block = aBlock);
    }

    /**
     * Override to get decl from method.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get method name and param types
        String name = getName();
        if (name == null) return null;
        JavaType[] ptypes = getParamClassTypesSafe();
        if (ptypes == null) return null; // Can happen if params are bogus

        // Get parent JClassDecl and JavaDecl
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        if (enclosingClassDecl == null) return null;
        JavaClass javaClass = enclosingClassDecl.getDecl();
        if (javaClass == null) return null;

        // Return method for name and param types
        return javaClass.getMethodForNameAndTypes(name, ptypes);
    }

    /**
     * Override to check formal parameters.
     */
    protected JavaDecl getDeclImpl(JNode aNode)
    {
        // If node is method name, return method decl
        if (aNode == _id) return getDecl();

        // Handle parameter name id: return param decl
        String name = aNode.getName();
        if (aNode instanceof JExprId) {
            JVarDecl param = getParam(name);
            if (param != null)
                return param.getDecl();
        }

        // Handle TypeVar name: return typevar decl
        JTypeVar tvar = getTypeVar(name);
        if (tvar != null)
            return tvar.getDecl();

        // Do normal version
        return super.getDeclImpl(aNode);
    }

    /**
     * Returns array of parameter class types suitable to resolve method.
     */
    protected JavaType[] getParamClassTypesSafe()
    {
        // Declare array for return types
        JavaType[] paramTypes = new JavaType[_params.size()];

        // Iterate over params to get types
        for (int i = 0, iMax = _params.size(); i < iMax; i++) {
            JVarDecl varDecl = _params.get(i);

            // Get current type and TypeVar (if type is one)
            JType varDeclType = varDecl.getType();
            JTypeVar typeVar = getTypeVar(varDeclType.getName());

            // If type is TypeVar, set to TypeVar.BoundsType
            if (typeVar != null)
                paramTypes[i] = typeVar.getBoundsType();
            else paramTypes[i] = varDeclType.getBaseDecl();

            // If param type is null, just return (can happen if params are bogus (being edited))
            if (paramTypes[i] == null) return null;

            // If array, get array type instead
            if (varDeclType.getArrayCount() > 0)
                for (int j = 0, jMax = varDeclType.getArrayCount(); j < jMax; j++)
                    paramTypes[i] = paramTypes[i].getArrayType();
        }

        // Return
        return paramTypes;
    }

    /**
     * Returns a variable with given name.
     */
    public List<JVarDecl> getVarDecls(String aPrefix, List<JVarDecl> theVariables)
    {
        // Iterate over statements and see if any JStmtVarDecl contains variable with that name
        if (_block != null)
            for (JStmt s : _block.getStatements()) {
                if (s instanceof JStmtVarDecl) {
                    JStmtVarDecl lvds = (JStmtVarDecl) s;
                    for (JVarDecl v : lvds.getVarDecls())
                        if (StringUtils.startsWithIC(v.getName(), aPrefix))
                            theVariables.add(v);
                }
            }

        // Iterate over formalParams
        for (JVarDecl v : _params)
            if (StringUtils.startsWithIC(v.getName(), aPrefix))
                theVariables.add(v);

        // Do normal version
        return super.getVarDecls(aPrefix, theVariables);
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        return "MethodDecl";
    }

}