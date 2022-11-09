/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.*;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * A parser for java statements.
 */
@SuppressWarnings({"unused", "StringEquality"})
public class JavaParserStmt extends JavaParserExpr {

    /**
     * Statement Handler.
     */
    public static class StatementHandler extends JNodeParseHandler<JStmt> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle any child with JavaStatement
            if (aNode.getCustomNode() instanceof JStmt)
                _part = aNode.getCustomNode(JStmt.class);
        }

        protected Class<JStmt> getPartClass()  { return JStmt.class; }
    }

    /**
     * Modifiers Handler.
     * Modifiers { Modifier* }
     * Modifier { "public" | "static" | "protected" | "private" | "final" | "abstract" | ... | Annotation }
     */
    public static class ModifiersHandler extends JNodeParseHandler<JModifiers> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JModifiers part = getPart();
            switch (anId) {
                case "public": part.addValue(Modifier.PUBLIC); break;
                case "static": part.addValue(Modifier.STATIC); break;
                case "protected": part.addValue(Modifier.PROTECTED); break;
                case "private": part.addValue(Modifier.PRIVATE); break;
                case "final": part.addValue(Modifier.FINAL); break;
                case "abstract": part.addValue(Modifier.ABSTRACT); break;
                case "synchronized": part.addValue(Modifier.SYNCHRONIZED); break;
                case "native": part.addValue(Modifier.NATIVE); break;
                case "transient": part.addValue(Modifier.TRANSIENT); break;
                case "volatile": part.addValue(Modifier.VOLATILE); break;
                case "strictfp": part.addValue(Modifier.STRICT); break;
                case "default": break; // Should we really treat as modifier? No support in java.lang.reflect.Modifier.
                default: break; // "Modifer" or Annotation
            }
        }

        protected Class<JModifiers> getPartClass()  { return JModifiers.class; }
    }

    /**
     * AssertStatement Handler.
     */
    public static class AssertStatementHandler extends JNodeParseHandler<JStmtAssert> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle condition
            if (getPart().getConditional() == null)
                getPart().setConditional(aNode.getCustomNode(JExpr.class));

            // Handle expression
            else getPart().setExpr(aNode.getCustomNode(JExpr.class));
        }

        protected Class<JStmtAssert> getPartClass()  { return JStmtAssert.class; }
    }

    /**
     * LabeledStatement Handler.
     */
    public static class LabeledStatementHandler extends JNodeParseHandler<JStmtLabeled> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Identifier
            if (anId == "Identifier")
                getPart().setLabel(aNode.getCustomNode(JExprId.class));

            // Handle Statement
            else if (anId == "Statement")
                getPart().setStmt(aNode.getCustomNode(JStmt.class));
        }

        protected Class<JStmtLabeled> getPartClass()  { return JStmtLabeled.class; }
    }

    /**
     * Block (Statement) Handler.
     */
    public static class BlockHandler extends JNodeParseHandler<JStmtBlock> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Statements
            JStmtBlock block = getPart();
            if (aNode.getCustomNode() instanceof JStmt)
                block.addStatement(aNode.getCustomNode(JStmt.class));
        }

        protected Class<JStmtBlock> getPartClass()  { return JStmtBlock.class; }
    }

    /**
     * BlockStatement Handler - translates VarDeclStmt and ClassDecl to JavaStatements.
     */
    public static class BlockStatementHandler extends JNodeParseHandler<JStmt> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle VarDeclStmt
            if (anId == "VarDeclStmt")
                _part = aNode.getCustomNode(JStmtVarDecl.class);

            // Handle Statement
            else if (anId == "Statement")
                _part = aNode.getCustomNode(JStmt.class);

            // Handle ClassDecl
            else if (anId == "ClassDecl") {
                JStmtClassDecl scd = new JStmtClassDecl();
                scd.setClassDecl(aNode.getCustomNode(JClassDecl.class));
                _part = scd;
            }
        }

        protected Class<JStmt> getPartClass()  { return JStmt.class; }
    }

    /**
     * FormalParam Handler.
     */
    public static class FormalParamHandler extends JNodeParseHandler<JVarDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Type
            if (anId == "Type")
                getPart().setType(aNode.getCustomNode(JType.class));

            // Handle vararg
            else if (anId == "...")
                getPart().getType().setArrayCount(getPart().getType().getArrayCount() + 1);

            // Handle Identifier
            else if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

            // Handle ("[" "]")*
            else if (anId == "[")
                getPart().getType().setArrayCount(getPart().getArrayCount() + 1);
        }

        protected Class<JVarDecl> getPartClass()  { return JVarDecl.class; }
    }

    /**
     * VarDecl Handler.
     */
    public static class VarDeclHandler extends JNodeParseHandler<JVarDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Identifier
            if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

            // Handle ("[" "]")*
            else if (anId == "[")
                getPart().setArrayCount(getPart().getArrayCount() + 1);

            // Handle VarInit ArrayInit
            else if (anId == "ArrayInit")
                getPart().setArrayInits(aNode.getCustomNode(List.class));

            // Handle VarInit Expression
            else if (anId == "Expression")
                getPart().setInitializer(aNode.getCustomNode(JExpr.class));
        }

        protected Class<JVarDecl> getPartClass()  { return JVarDecl.class; }
    }

    /**
     * VarDeclStmt Handler.
     */
    public static class VarDeclStmtHandler extends JNodeParseHandler<JStmtVarDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Modifiers
            if (anId == "Modifiers")
                getPart().setMods(aNode.getCustomNode(JModifiers.class));

            // Handle Type
            else if (anId == "Type")
                getPart().setType(aNode.getCustomNode(JType.class));

            // Handle VarDecl(s)
            else if (anId == "VarDecl") {
                JVarDecl vd = aNode.getCustomNode(JVarDecl.class);
                getPart().addVarDecl(vd);
            }
        }

        protected Class<JStmtVarDecl> getPartClass()  { return JStmtVarDecl.class; }
    }

    /**
     * EmptyStatement Handler.
     */
    public static class EmptyStatementHandler extends JNodeParseHandler<JStmtEmpty> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            getPart();
        }

        protected Class<JStmtEmpty> getPartClass()  { return JStmtEmpty.class; }
    }

    /**
     * ExprStatement Handler.
     */
    public static class ExprStatementHandler extends JNodeParseHandler<JStmtExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get part and custom node
            JStmtExpr exprStmt = getPart();
            Object customNode = aNode.getCustomNode();

            // Handle JavaExpression rules
            if (customNode instanceof JExpr) {
                JExpr expr = (JExpr) customNode;

                // If no expression yet, just set
                JExpr stmtExpr = exprStmt.getExpr();
                if (stmtExpr == null)
                    exprStmt.setExpr(expr);

                // Otherwise assume it is broken assignment stmt and fill it in
                else {
                    JExprAssign assignExpr = new JExprAssign("=", stmtExpr, expr);
                    exprStmt.setExpr(assignExpr);
                }
            }

            // Handle post increment/decrement
            else if (anId == "++" || anId == "--") {
                JExpr expr = exprStmt.getExpr();
                JExprMath.Op op = anId == "++" ? JExprMath.Op.PostIncrement : JExprMath.Op.PostDecrement;
                JExprMath unaryExpr = new JExprMath(op, expr);
                exprStmt.setExpr(unaryExpr);
            }

            // Shouldn't be possible
            else System.err.println("ExprStatementHandler: Unexpected node: " + anId);
        }

        protected Class<JStmtExpr> getPartClass()  { return JStmtExpr.class; }
    }

    /**
     * SwitchStatement Handler: { "switch" "(" Expression ")" "{" (SwitchLabel BlockStatement*)* "}" }
     */
    public static class SwitchStatementHandler extends JNodeParseHandler<JStmtSwitch> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Expression
            if (anId == "Expression")
                getPart().setExpr(aNode.getCustomNode(JExpr.class));

            // Handle SwitchLabel
            else if (anId == "SwitchLabel")
                getPart().addSwitchLabel(aNode.getCustomNode(JStmtSwitch.SwitchLabel.class));

            // Handle BlockStatement
            else if (anId == "BlockStatement") {
                List<JStmtSwitch.SwitchLabel> switchLabels = getPart().getSwitchLabels();
                JStmtSwitch.SwitchLabel switchLabel = switchLabels.get(switchLabels.size() - 1);
                JStmt blockStmt = aNode.getCustomNode(JStmt.class);
                if (blockStmt != null) // Can be null when parse fails
                    switchLabel.addStatement(blockStmt);
            }

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtSwitch> getPartClass()  { return JStmtSwitch.class; }
    }

    /**
     * SwitchLabel Handler.
     */
    public static class SwitchLabelHandler extends JNodeParseHandler<JStmtSwitch.SwitchLabel> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Expression
            if (anId == "Expression")
                getPart().setExpr(aNode.getCustomNode(JExpr.class));

            // Handle "default"
            else if (anId == "default")
                getPart().setDefault(true);

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtSwitch.SwitchLabel> getPartClass()  { return JStmtSwitch.SwitchLabel.class; }
    }

    /**
     * IfStatement Handler.
     */
    public static class IfStatementHandler extends JNodeParseHandler<JStmtIf> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Expression
            if (anId == "Expression")
                getPart().setConditional(aNode.getCustomNode(JExpr.class));

            // Handle Statement
            else if (anId == "Statement") {
                JStmt stmt = aNode.getCustomNode(JStmt.class);
                if (getPart().getStatement() == null) getPart().setStatement(stmt);
                else getPart().setElseStatement(stmt);
            }

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtIf> getPartClass()  { return JStmtIf.class; }
    }

    /**
     * WhileStatement Handler.
     */
    public static class WhileStatementHandler extends JNodeParseHandler<JStmtWhile> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Expression
            if (anId == "Expression")
                getPart().setConditional(aNode.getCustomNode(JExpr.class));

            // Handle Statement
            else if (anId == "Statement")
                getPart().setStatement(aNode.getCustomNode(JStmt.class));

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtWhile> getPartClass()  { return JStmtWhile.class; }
    }

    /**
     * DoStatement Handler.
     */
    public static class DoStatementHandler extends JNodeParseHandler<JStmtDo> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Statement
            if (anId == "Statement")
                getPart().setStatement(aNode.getCustomNode(JStmt.class));

            // Handle Expression
            else if (anId == "Expression")
                getPart().setConditional(aNode.getCustomNode(JExpr.class));

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtDo> getPartClass()  { return JStmtDo.class; }
    }

    /**
     * ForStatement Handler.
     */
    public static class ForStatementHandler extends JNodeParseHandler<JStmtFor> {

        // The current part index (0 = init, 1 = conditional, 2 = update)
        private int  _partIndex = 0;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Go ahead and get ForStmt
            JStmtFor forStmt = getPart();

            // Handle Type
            if (anId == "Type") {
                JType type = aNode.getCustomNode(JType.class);
                JStmtVarDecl svd = new JStmtVarDecl();
                svd.setType(type);
                forStmt.setInitDecl(svd);
            }

            // Handle Identifier
            else if (anId == "Identifier") {
                JExprId idExpr = aNode.getCustomNode(JExprId.class);
                JVarDecl varDecl = new JVarDecl();
                varDecl.setId(idExpr);
                forStmt.getInitDecl().addVarDecl(varDecl);
            }

            // Handle ForInit VarDeclStmt
            else if (anId == "VarDeclStmt") {
                JStmtVarDecl varDeclStmt = aNode.getCustomNode(JStmtVarDecl.class);
                forStmt.setInitDecl(varDeclStmt);
            }

            // Handle ForInit ExprStatement(s) or ForUpdate ExprStatement(s)
            else if (anId == "ExprStatement") {
                JStmtExpr se = aNode.getCustomNode(JStmtExpr.class);
                if (_partIndex == 0)
                    forStmt.addInitStmt(se);
                else forStmt.addUpdateStmt(se);
            }

            // Handle init or conditional Expression
            else if (anId == "Expression") {
                JExpr condExpr = aNode.getCustomNode(JExpr.class);
                forStmt.setConditional(condExpr);
            }

            // Handle separator
            else if (anId == ";") {
                _partIndex++;
                forStmt._forEach = false;
            }

            // Handle Statement
            else if (anId == "Statement") {
                JStmt stmt = aNode.getCustomNode(JStmt.class);
                forStmt.setStatement(stmt);
            }
        }

        /**
         * Override to clear partIndex.
         */
        public JStmtFor parsedAll()
        {
            _partIndex = 0;
            return super.parsedAll();
        }

        protected Class<JStmtFor> getPartClass()  { return JStmtFor.class; }
    }

    /**
     * BreakStatement Handler.
     */
    public static class BreakStatementHandler extends JNodeParseHandler<JStmtBreak> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Identifier
            if (anId == "Identifier")
                getPart().setLabel(aNode.getCustomNode(JExprId.class));

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtBreak> getPartClass()  { return JStmtBreak.class; }
    }

    /**
     * ContinueStatement Handler.
     */
    public static class ContinueStatementHandler extends JNodeParseHandler<JStmtContinue> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Identifier
            if (anId == "Identifier")
                getPart().setLabel(aNode.getCustomNode(JExprId.class));

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtContinue> getPartClass()  { return JStmtContinue.class; }
    }

    /**
     * ReturnStatement Handler.
     */
    public static class ReturnStatementHandler extends JNodeParseHandler<JStmtReturn> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Expression
            if (anId == "Expression")
                getPart().setExpr(aNode.getCustomNode(JExpr.class));

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtReturn> getPartClass()  { return JStmtReturn.class; }
    }

    /**
     * ThrowStatement Handler.
     */
    public static class ThrowStatementHandler extends JNodeParseHandler<JStmtThrow> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Expression
            if (anId == "Expression")
                getPart().setExpr(aNode.getCustomNode(JExpr.class));

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtThrow> getPartClass()  { return JStmtThrow.class; }
    }

    /**
     * SynchronizedStatement Handler.
     */
    public static class SynchronizedStatementHandler extends JNodeParseHandler<JStmtSynchronized> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Expression
            if (anId == "Expression")
                getPart().setExpression(aNode.getCustomNode(JExpr.class));

            // Handle Block
            else if (anId == "Block")
                getPart().setBlock(aNode.getCustomNode(JStmtBlock.class));

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtSynchronized> getPartClass()  { return JStmtSynchronized.class; }
    }

    /**
     * TryStatement Handler.
     */
    public static class TryStatementHandler extends JNodeParseHandler<JStmtTry> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Block
            if (anId == "Block") {
                JStmtBlock sb = aNode.getCustomNode(JStmtBlock.class);
                getPart().addStatementBlock(sb);
            }

            // Handle FormalParam
            else if (anId == "FormalParam") {
                JStmtTry.CatchBlock cblock = new JStmtTry.CatchBlock();
                cblock.setParameter(aNode.getCustomNode(JVarDecl.class));
                getPart().addCatchBlock(cblock);
            }

            // Handle anything else
            else getPart();
        }

        protected Class<JStmtTry> getPartClass()  { return JStmtTry.class; }
    }
}