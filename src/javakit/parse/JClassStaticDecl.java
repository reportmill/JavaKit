/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A JMemberDecl for Initializers.
 */
public class JClassStaticDecl extends JMemberDecl {
    // Whether initializer is static
    boolean isStatic;

    // The Block statement of statements
    JStmtBlock _block;

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

}