/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaType;
import snap.util.SnapUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * A Java member for MethodDeclaration.
 */
public class JExecutableDecl extends JMemberDecl {

    // The type/return-type
    protected JType  _type;

    // Type variables
    protected List<JTypeVar>  _typeVars;

    // The formal parameters
    protected List<JVarDecl>  _params = new ArrayList<>();

    // The throws names list
    protected List<JExpr>  _throwsNameList = new ArrayList<>();

    // The statement Block
    protected JStmtBlock  _block;

    /**
     * Constructor.
     */
    public JExecutableDecl()
    {
        super();
    }

    /**
     * Returns the field type.
     */
    public JType getType()  { return _type; }

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
    public List<JTypeVar> getTypeVars()  { return _typeVars; }

    /**
     * Sets the method type variables.
     */
    public void setTypeVars(List<JTypeVar> theTVs)
    {
        if (_typeVars != null)
            for (JTypeVar tvar : _typeVars)
                removeChild(tvar);

        _typeVars = theTVs;

        if (_typeVars != null)
            for (JTypeVar tvar : _typeVars)
                addChild(tvar, -1);
    }

    /**
     * Returns the number of formal parameters.
     */
    public int getParamCount()  { return _params.size(); }

    /**
     * Returns the individual formal parameter at given index.
     */
    public JVarDecl getParam(int anIndex)  { return _params.get(anIndex); }

    /**
     * Returns the list of formal parameters.
     */
    public List<JVarDecl> getParameters()  { return _params; }

    /**
     * Returns the list of formal parameters.
     */
    public void addParam(JVarDecl aVarDecl)
    {
        if (aVarDecl == null) {
            System.err.println("JExecutableDecl.addParam: Add null param!");
            return;
        }
        _params.add(aVarDecl);
        addChild(aVarDecl, -1);
    }

    /**
     * Returns the parameter with given name.
     */
    public JVarDecl getParam(String aName)
    {
        for (JVarDecl varDecl : _params)
            if (SnapUtils.equals(varDecl.getName(), aName))
                return varDecl;

        // Return not found
        return null;
    }

    /**
     * Returns the throws list.
     */
    public List<JExpr> getThrowsList()  { return _throwsNameList; }

    /**
     * Sets the throws list.
     */
    public void setThrowsList(List<JExpr> theThrows)
    {
        if (_throwsNameList != null)
            for (JExpr t : _throwsNameList)
                removeChild(t);

        _throwsNameList = theThrows;

        if (_throwsNameList != null)
            for (JExpr t : _throwsNameList)
                addChild(t, -1);
    }

    /**
     * Returns whether statement has a block associated with it.
     */
    public boolean isBlock()  { return true; }

    /**
     * Returns the block.
     */
    public JStmtBlock getBlock()  { return _block; }

    /**
     * Sets the block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        replaceChild(_block, _block = aBlock);
    }

    /**
     * Override to check formal parameters.
     */
    protected JavaDecl getDeclForChildNode(JNode aNode)
    {
        // If node is method name, return method decl
        if (aNode == _id)
            return getDecl();

        // Handle parameter name id: return param decl
        String name = aNode.getName();
        if (aNode instanceof JExprId) {
            JVarDecl param = getParam(name);
            if (param != null)
                return param.getDecl();
        }

        // Handle TypeVar name: return typevar decl
        JTypeVar typeVar = getTypeVar(name);
        if (typeVar != null)
            return typeVar.getDecl();

        // Do normal version
        return super.getDeclForChildNode(aNode);
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
            int arrayCount = varDeclType.getArrayCount();
            for (int j = 0; j < arrayCount; j++)
                paramTypes[i] = paramTypes[i].getArrayType();
        }

        // Return
        return paramTypes;
    }

    /**
     * Override to search method/constructor params and VarDecl statements.
     */
    @Override
    public List<JVarDecl> getVarDeclsForMatcher(Matcher aMatcher, List<JVarDecl> varDeclList)
    {
        // Add VarDecls for formal params
        for (JVarDecl varDecl : _params)
            if (aMatcher.reset(varDecl.getName()).lookingAt())
                varDeclList.add(varDecl);

        // Add VarDecls for block statements
        if (_block != null) {
            List<JStmt> statements = _block.getStatements();
            JStmtBlock.getVarDeclsForMatcherFromStatements(aMatcher, statements, varDeclList);
        }

        // Do normal version
        return super.getVarDeclsForMatcher(aMatcher, varDeclList);
    }
}