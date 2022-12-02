/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.JavaDecl;

/**
 * A Java statement for TryStatement.
 */
public class JStmtTry extends JStmt {

    // The statement block
    protected JStmtBlock  _tryBlock;

    // The catch blocks
    protected List<CatchBlock>  _catchBlocks = new ArrayList();

    // The finally block
    protected JStmtBlock  _finallyBlock;

    /**
     * Returns the try block.
     */
    public JStmtBlock getTryBlock()
    {
        return _tryBlock;
    }

    /**
     * Sets the try block.
     */
    public void setTryBlock(JStmtBlock aBlock)
    {
        replaceChild(_tryBlock, _tryBlock = aBlock);
    }

    /**
     * Returns the catch blocks.
     */
    public List<CatchBlock> getCatchBlocks()
    {
        return _catchBlocks;
    }

    /**
     * Adds a catch block.
     */
    public void addCatchBlock(CatchBlock aBlock)
    {
        _catchBlocks.add(aBlock);
        addChild(aBlock, -1);
    }

    /**
     * Returns the finally block.
     */
    public JStmtBlock getFinallyBlock()
    {
        return _finallyBlock;
    }

    /**
     * Sets the finally block.
     */
    public void setFinallyBlock(JStmtBlock aBlock)
    {
        replaceChild(_finallyBlock, _finallyBlock = aBlock);
    }

    /**
     * Adds a statement block.
     */
    public void addStatementBlock(JStmtBlock aBlock)
    {
        // If TryBlock not set, set it
        if (_tryBlock == null) {
            setTryBlock(aBlock);
            return;
        }

        // If last CatchBlock doesn't have StatementBlock, set it
        int ccount = _catchBlocks.size();
        if (ccount > 0) {
            CatchBlock lcatch = _catchBlocks.get(ccount - 1);
            if (lcatch.getBlock() == null) {
                lcatch.setBlock(aBlock);
                return;
            }
        }

        // Otherwise set Finally Block
        setFinallyBlock(aBlock);
    }

    /**
     * A JNode for a catch block
     */
    public static class CatchBlock extends JNode {

        // The formal parameter
        JVarDecl _param;

        // The catch block
        JStmtBlock _block;

        /**
         * Returns the parameter.
         */
        public JVarDecl getParameter()
        {
            return _param;
        }

        /**
         * Sets the parameter.
         */
        public void setParameter(JVarDecl aVD)
        {
            replaceChild(_param, _param = aVD);
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
        public void setBlock(JStmtBlock aStmtBlock)
        {
            replaceChild(_block, _block = aStmtBlock);
        }

        /**
         * Override to check param.
         */
        @Override
        protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
        {
            // Check params
            String name = anExprId.getName();
            if (_param != null && Objects.equals(_param.getName(), name))
                return _param.getDecl();

            // Do normal version
            return super.getDeclForChildExprIdNode(anExprId);
        }
    }
}