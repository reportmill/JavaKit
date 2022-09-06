/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.reflect.JavaDecl;
import java.util.List;

/**
 * A JMemberDecl for Initializer declarations.
 */
public class JInitializerDecl extends JMemberDecl {

    // Whether initializer is static
    protected boolean  isStatic;

    // The Block statement of statements
    protected JStmtBlock  _block;

    /**
     * Returns whether is static.
     */
    public boolean isStatic()
    {
        return isStatic;
    }

    /**
     * Sets whether is static.
     */
    public void setStatic(boolean aValue)
    {
        isStatic = aValue;
    }

    /**
     * Returns the statement block.
     */
    public JStmtBlock getBlock()
    {
        return _block;
    }

    /**
     * Sets the statement block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        replaceChild(_block, _block = aBlock);
    }

    /**
     * Override to add REPL hack to check prior JInitDecls for VarDecl matching node name.
     */
    @Override
    protected JavaDecl getDeclForChildNode(JNode aNode)
    {
        // Do normal version - just return if successful
        JavaDecl decl = super.getDeclForChildNode(aNode);
        if (decl != null)
            return decl;

        // Get enclosing class initDecls
        JClassDecl classDecl = getEnclosingClassDecl();
        JInitializerDecl[] initDecls = classDecl.getInitDecls();

        // Iterate over initDecls
        for (JInitializerDecl initDecl : initDecls) {
            if (initDecl == this)
                break;
            JStmtBlock initDeclBlock = initDecl.getBlock();
            List<JStmt> initDeclStmts = initDeclBlock.getStatements();
            JVarDecl varDecl = JStmtBlock.getVarDeclForNameFromStatements(aNode, initDeclStmts);
            if (varDecl != null)
                return varDecl.getDecl();
        }

        // Return not found
        return null;
    }

    /**
     * Override to add REPL hack to check prior JInitDecls for VarDecl matching node name.
     */
    @Override
    public List<JVarDecl> getVarDeclsForPrefix(String aPrefix, List<JVarDecl> varDeclList)
    {
        // Do normal version
        super.getVarDeclsForPrefix(aPrefix, varDeclList);

        // Get enclosing class initDecls
        JClassDecl classDecl = getEnclosingClassDecl();
        JInitializerDecl[] initDecls = classDecl.getInitDecls();

        // Iterate over initDecls
        for (JInitializerDecl initDecl : initDecls) {
            if (initDecl == this)
                break;
            JStmtBlock initDeclBlock = initDecl.getBlock();
            List<JStmt> initDeclStmts = initDeclBlock.getStatements();
            JStmtBlock.getVarDeclsForPrefixFromStatements(aPrefix, initDeclStmts, varDeclList);
        }

        // Return
        return varDeclList;
    }
}