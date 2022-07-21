/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.ParseHandler;
import snap.parse.ParseNode;
import snap.parse.Parser;
import snap.parse.Token;
import java.util.ArrayList;
import java.util.List;

/**
 * A parser for java statements.
 */
@SuppressWarnings({"unused", "StringEquality"})
public class JavaParserExpr extends Parser {

    /**
     * Expression Handler.
     */
    public static class ExpressionHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle ConditionalExpr
            if (anId == "ConditionalExpr")
                _part = aNode.getCustomNode(JExpr.class);

                // Handle Assign Op
            else if (anId == "AssignmentOp")
                _part = new JExprMath(JExprMath.Op.Assign, _part);

                // Handle Expression
            else if (anId == "Expression")
                ((JExprMath) _part).setOperand(aNode.getCustomNode(JExpr.class), 1);
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * Identifier Handler.
     */
    public static class IdentifierHandler extends JNodeParseHandler<JExprId> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            getPart().setName(aNode.getString());
        }

        protected Class<JExprId> getPartClass()  { return JExprId.class; }
    }

    /**
     * Name Handler.
     */
    public static class NameHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "Identifier")
                _part = JExpr.join(_part, aNode.getCustomNode(JExprId.class));
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * Type Handler.
     */
    public static class TypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle PrimitiveType
            if (anId == "PrimitiveType")
                _part = aNode.getCustomNode(JType.class);

                // Handle ReferenceType."["
            else if (anId == "[")
                getPart().setArrayCount(getPart().getArrayCount() + 1);

                // Handle ClassType
            else if (anId == "ClassType")
                _part = aNode.getCustomNode(JType.class);
        }

        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ClassType Handler.
     */
    public static class ClassTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle: Identifier [ TypeArgs ] ( "." Identifier [ TypeArgs ] ) *
            if (anId == "Identifier")
                if (getPart().getName() == null) getPart().setName(aNode.getString());
                else getPart().setName(getPart().getName() + '.' + aNode.getString());

                // Handle TypeArgs (ReferenceType)
            else if (aNode.getCustomNode() instanceof JType) {
                JType type = aNode.getCustomNode(JType.class);
                getPart().addTypeArg(type);
            }
        }

        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * PrimitiveType Handler.
     */
    public static class PrimitiveTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle primitive types
            if (anId == "boolean" || anId == "char" || anId == "byte" || anId == "short" ||
                    anId == "int" || anId == "long" || anId == "float" || anId == "double")
                getPart().setName(anId);
            getPart().setPrimitive(true);
        }

        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ResultType Handler.
     */
    public static class ResultTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Type
            if (anId == "Type")
                _part = aNode.getCustomNode(JType.class);

                // Handle void
            else if (anId == "void") {
                getPart().setName("void");
                getPart().setPrimitive(true);
            }
        }

        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ConditionalExpr Handler.
     */
    public static class ConditionalExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle ConditionalOrExpr
            if (anId == "ConditionalOrExpr")
                _part = aNode.getCustomNode(JExpr.class);

            // Handle Expression
            if (anId == "Expression") {
                JExpr part = aNode.getCustomNode(JExpr.class);
                JExprMath opExpr = _part instanceof JExprMath ? (JExprMath) _part : null;
                if (opExpr == null || opExpr.op != JExprMath.Op.Conditional)
                    _part = new JExprMath(JExprMath.Op.Conditional, _part, part);
                else opExpr.setOperand(part, 2);
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * ConditionalOrExpr Handler.
     */
    public static class ConditionalOrExprHandler extends BinaryExprHandler { }

    /**
     * ConditionalAndExpr Handler.
     */
    public static class ConditionalAndExprHandler extends BinaryExprHandler { }

    /**
     * InclusiveOrExpr Handler.
     */
    public static class InclusiveOrExprHandler extends BinaryExprHandler { }

    /**
     * ExclusiveOrExpr Handler.
     */
    public static class ExclusiveOrExprHandler extends BinaryExprHandler { }

    /**
     * AndExpr Handler.
     */
    public static class AndExprHandler extends BinaryExprHandler { }

    /**
     * EqualityExpr Handler.
     */
    public static class EqualityExprHandler extends BinaryExprHandler { }

    /**
     * RelationalExpr Handler.
     */
    public static class RelationalExprHandler extends BinaryExprHandler { }

    /**
     * ShiftExpr Handler.
     */
    public static class ShiftExprHandler extends BinaryExprHandler { }

    /**
     * AdditiveExpr Handler.
     */
    public static class AdditiveExprHandler extends BinaryExprHandler { }

    /**
     * MultiplicativeExpr Handler.
     */
    public static class MultiplicativeExprHandler extends BinaryExprHandler { }

    /**
     * OpExpr Handler.
     */
    public static abstract class BinaryExprHandler extends ParseHandler<JExpr> {

        // The Op
        JExprMath.Op _op;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle KeyChain
            if (aNode.getCustomNode() instanceof JExpr) {
                JExpr part = aNode.getCustomNode(JExpr.class);
                if (_part == null) _part = part;
                else {
                    _part = new JExprMath(_op, _part, part);
                    _op = null;
                }
            }

            // Handle Ops
            else if (anId == "||") _op = JExprMath.Op.Or;
            else if (anId == "&&") _op = JExprMath.Op.And;
            else if (anId == "|") _op = JExprMath.Op.BitOr;
            else if (anId == "^") _op = JExprMath.Op.BitXOr;
            else if (anId == "&") _op = JExprMath.Op.BitAnd;
            else if (anId == "==") _op = JExprMath.Op.Equal;
            else if (anId == "!=") _op = JExprMath.Op.NotEqual;
            else if (anId == "<") _op = JExprMath.Op.LessThan;
            else if (anId == ">") _op = JExprMath.Op.GreaterThan;
            else if (anId == "<=") _op = JExprMath.Op.LessThanOrEqual;
            else if (anId == ">=") _op = JExprMath.Op.GreaterThanOrEqual;
            else if (anId == "<<") _op = JExprMath.Op.ShiftLeft;
            else if (anId == "ShiftRight") _op = JExprMath.Op.ShiftRight;
            else if (anId == "ShiftRightUnsigned") _op = JExprMath.Op.ShiftRightUnsigned;
            else if (anId == "+") _op = JExprMath.Op.Add;
            else if (anId == "-") _op = JExprMath.Op.Subtract;
            else if (anId == "*") _op = JExprMath.Op.Multiply;
            else if (anId == "/") _op = JExprMath.Op.Divide;
            else if (anId == "%") _op = JExprMath.Op.Mod;
        }

        @Override
        protected Class<JExpr> getPartClass()
        {
            return JExpr.class;
        }
    }

    /**
     * UnaryExpr Handler.
     */
    public static class UnaryExprHandler extends JNodeParseHandler<JExpr> {

        // The current op
        JExprMath.Op _op;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle JavaExpression rules: PreIncrementExpr, PreDecrementExpr, UnaryExprNotPlusMinus
            if (aNode.getCustomNode() instanceof JExpr) {
                JExpr part = aNode.getCustomNode(JExpr.class);
                _part = _op == null ? part : new JExprMath(_op, part);
            }

            // Handle unary ops (ignore '+')
            else if (anId == "-") _op = JExprMath.Op.Negate;
            else if (anId == "~") _op = JExprMath.Op.BitComp;
            else if (anId == "!") _op = JExprMath.Op.Not;

                // Handle post Increment/Decrement
            else if (anId == "++" || anId == "--") {
                _op = anId == "++" ? JExprMath.Op.PostIncrement : JExprMath.Op.PostDecrement;
                if (_part != null) _part = new JExprMath(_op, _part);
            }
        }

        /**
         * Override to clear op.
         */
        public JExpr parsedAll()
        {
            _op = null;
            return super.parsedAll();
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PreIncrementExpr Handler.
     */
    public static class PreIncrementExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "PrimaryExpr")
                _part = new JExprMath(JExprMath.Op.PreIncrement, aNode.getCustomNode(JExpr.class));
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PreDecrementExpr Handler.
     */
    public static class PreDecrementExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "PrimaryExpr")
                _part = new JExprMath(JExprMath.Op.PreDecrement, aNode.getCustomNode(JExpr.class));
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * CastExpr Handler.
     */
    public static class CastExprHandler extends JNodeParseHandler<JExpr.CastExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Type node
            if (anId == "Type")
                getPart().setType(aNode.getCustomNode(JType.class));

                // Handle UnaryExpr
            else if (aNode.getCustomNode() != null)
                getPart().setExpr(aNode.getCustomNode(JExpr.class));
        }

        protected Class<JExpr.CastExpr> getPartClass()  { return JExpr.CastExpr.class; }
    }

    /**
     * InstanceOfExpr Handler.
     */
    public static class InstanceOfExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle expression
            if (aNode.getCustomNode() instanceof JExpr)
                _part = aNode.getCustomNode(JExpr.class);

            // Handle Type node
            if (anId == "Type") {
                JExpr.InstanceOfExpr ie = new JExpr.InstanceOfExpr();
                ie.setExpr(_part);
                ie.setType(aNode.getCustomNode(JType.class));
                _part = ie;
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimaryExpr Handler.
     */
    public static class PrimaryExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle PrimaryPrefix
            if (anId == "PrimaryPrefix")
                _part = aNode.getCustomNode(JExpr.class);

            // Handle PrimarySuffix: Join prefix and suffix
            if (anId == "PrimarySuffix") {
                JExpr expr = aNode.getCustomNode(JExpr.class);
                _part = JExpr.join(_part, expr);
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimaryPrefix Handler.
     */
    public static class PrimaryPrefixHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Literal
            if (anId == "Literal")
                _part = aNode.getCustomNode(JExprLiteral.class);

                // Handle Identifier of [ (Identifier ".")* this ] and [ "super" "." Identifier ]
            else if (anId == "Identifier")
                _part = JExpr.join(_part, aNode.getCustomNode(JExprId.class));

                // Handle "this"/"super" of [ (Identifier ".")* this ] and [ "super" "." Identifier ]
            else if (anId == "this" || anId == "super") {
                JExprId id = new JExprId(aNode.getString());
                id.setStartToken(aNode.getStartToken());
                id.setEndToken(aNode.getEndToken());
                _part = JExpr.join(_part, id);
            }

            // Handle ClassType (using above to handle the rest: "." "super" "." Identifier
            else if (anId == "ClassType")
                _part = new JExprType(aNode.getCustomNode(JType.class));

                // Handle LambdaExpr
            else if (anId == "LambdaExpr")
                _part = aNode.getCustomNode(JExpr.class);

                // Handle "(" Expression ")"
            else if (anId == "Expression")
                _part = aNode.getCustomNode(JExpr.class);

                // Handle AllocExpr
            else if (anId == "AllocExpr")
                _part = aNode.getCustomNode(JExpr.class);

                // Handle ResultType "." "class"
            else if (anId == "ResultType")
                _part = new JExprType(aNode.getCustomNode(JType.class));
            else if (anId == "class") {
                JExprId id = new JExprId("class");
                id.setStartToken(aNode.getStartToken());
                id.setEndToken(aNode.getEndToken());
                _part = JExpr.join(_part, id);
            }

            // Handle Name
            else if (anId == "Name") {
                JExpr namePrime = aNode.getCustomNode(JExpr.class);
                if (namePrime instanceof JExprChain) {
                    JExprChain nameChain = (JExprChain) namePrime;
                    for (int i = 0, iMax = nameChain.getExprCount(); i < iMax; i++)
                        _part = JExpr.join(_part, nameChain.getExpr(i));
                } else _part = JExpr.join(_part, namePrime);
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimarySuffix Handler.
     */
    public static class PrimarySuffixHandler extends JNodeParseHandler<JExpr> {

        boolean _methodRef;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle [ "." "super" ] and [ "." "this" ]
            if (anId == "super" || anId == "this")
                _part = new JExprId(aNode.getString());

                // Handle AllocExpr
            else if (anId == "AllocExpr")
                _part = aNode.getCustomNode(JExpr.class);

                // Handle MemberSelector: TypeArgs Identifier (currently handed below without TypeArgs)
                //else if(anId=="TypeArgs") _part = aNode.getCustomNode(JavaExpression.class);

                // Handle "[" Expression "]"
            else if (anId == "Expression")
                _part = new JExprArrayIndex(null, aNode.getCustomNode(JExpr.class));

                // Handle ("." | "::") Identifier
            else if (anId == "Identifier") {
                JExprId id = aNode.getCustomNode(JExprId.class);
                if (_methodRef) {
                    _part = new JExprMethodRef(null, id);
                    _methodRef = false;
                } else _part = id;
            }

            // Handle "::" Identifier
            else if (anId == "::") _methodRef = true;

                // Handle Arguments
            else if (anId == "Arguments")
                _part = new JExprMethodCall(null, aNode.getCustomNode(List.class));
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * Arguments Handler
     */
    public static class ArgumentsHandler extends ParseHandler<ArrayList<JExpr>> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Expression
            if (anId == "Expression")
                getPart().add(aNode.getCustomNode(JExpr.class));
            else getPart();
        }

        @Override
        protected Class getPartClass()
        {
            return ArrayList.class;
        }
    }

    /**
     * AllocExpr Handler.
     */
    public static class AllocExprHandler extends JNodeParseHandler<JExprAlloc> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle PrimitiveType
            if (anId == "PrimitiveType")
                getPart().setType(aNode.getCustomNode(JType.class));

                // Handle ArrayDimsAndInits
            else if (anId == "Expression" && getPart().getType() != null && getPart().getType().isArrayType())
                getPart().setArrayDims(aNode.getCustomNode(JExpr.class));

                // Handle ArrayDimsAndInits ArrayInit
            else if (anId == "ArrayInit")
                getPart().setArrayInits(aNode.getCustomNode(List.class));

                // Handle ClassType
            else if (anId == "ClassType")
                getPart().setType(aNode.getCustomNode(JType.class));

                // Handle TypeArgs, ArrayDimsAndInits
            else if (anId == "[" && getPart().getType() != null)
                getPart().getType().setArrayCount(getPart().getType().getArrayCount() + 1);

                // Handle Arguments
            else if (anId == "Arguments")
                getPart().setArgs(aNode.getCustomNode(List.class));

                // Handle ClassBody
            else if (anId == "ClassBody") {
                JClassDecl cd = aNode.getCustomNode(JClassDecl.class);
                cd.addExtendsType(getPart().getType());
                getPart().setClassDecl(cd);
            }
        }

        protected Class<JExprAlloc> getPartClass()  { return JExprAlloc.class; }
    }

    /**
     * ArrayInit Handler
     */
    public static class ArrayInitHandler extends ParseHandler<ArrayList<JExpr>> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Expression
            if (anId == "Expression")
                getPart().add(aNode.getCustomNode(JExpr.class));
            else getPart();
        }

        @Override
        protected Class getPartClass()  { return ArrayList.class; }
    }

    /**
     * LambdaExpr Handler.
     */
    public static class LambdaExprHandler extends JNodeParseHandler<JExprLambda> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Identifier
            if (anId == "Identifier") {
                JVarDecl vd = new JVarDecl();
                vd.setId(aNode.getCustomNode(JExprId.class));
                getPart().addParam(vd);
            }

            // Handle FormalParam
            else if (anId == "FormalParam")
                getPart().addParam(aNode.getCustomNode(JVarDecl.class));

                // Handle Expression
            else if (anId == "Expression")
                getPart().setExpr(aNode.getCustomNode(JExpr.class));

                // Handle Block
            else if (anId == "Block")
                getPart().setBlock(aNode.getCustomNode(JStmtBlock.class));
        }

        protected Class<JExprLambda> getPartClass()  { return JExprLambda.class; }
    }

    /**
     * Literal Handler.
     */
    public static class LiteralHandler extends JNodeParseHandler<JExprLiteral> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get node string
            String s = aNode.getString();

            // Handle BooleanLiteral
            if (anId == "BooleanLiteral")
                getPart().setLiteralType(JExprLiteral.LiteralType.Boolean);

                // Handle IntegerLiteral
            else if (anId == "IntegerLiteral") {
                int len = s.length();
                char c = s.charAt(len - 1);
                if (c == 'l' || c == 'L') getPart().setLiteralType(JExprLiteral.LiteralType.Long);
                else getPart().setLiteralType(JExprLiteral.LiteralType.Integer);
            }

            // Handle FloatLiteral
            else if (anId == "FloatLiteral") {
                int len = s.length();
                char c = s.charAt(len - 1);
                if (c == 'f' || c == 'F') getPart().setLiteralType(JExprLiteral.LiteralType.Float);
                else getPart().setLiteralType(JExprLiteral.LiteralType.Double);
            }

            // Handle CharacterLiteral
            else if (anId == "CharacterLiteral")
                getPart().setLiteralType(JExprLiteral.LiteralType.Character);

                // Handle StringLiteral
            else if (anId == "StringLiteral")
                getPart().setLiteralType(JExprLiteral.LiteralType.String);

            // Set value string
            getPart().setValueString(s);
        }

        protected Class<JExprLiteral> getPartClass()  { return JExprLiteral.class; }
    }

    /**
     * AnnotationDecl Handler.
     * TODO
     */
    public static class AnnotationDeclHandler extends JNodeParseHandler<JClassDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
        }

        /**
         * Override to create JClassDecl with ClassType Annotation.
         */
        public JClassDecl createPart()
        {
            JClassDecl cd = new JClassDecl();
            cd.setClassType(JClassDecl.ClassType.Enum);
            return cd;
        }
    }

    /**
     * A base ParseHandler implementation for JNodes.
     */
    public abstract static class JNodeParseHandler<T extends JNode> extends ParseHandler<T> {

        /**
         * ParseHandler method.
         */
        public final void parsedOne(ParseNode aNode)
        {
            // Do normal version
            super.parsedOne(aNode);

            // Set start/end token
            if (_part != null) {
                if (_part.getStartToken() == null) _part.setStartToken(getStartToken());
                _part.setEndToken(aNode.getEndToken());
            }
        }

        /**
         * Override to set part start.
         */
        protected T createPart()
        {
            T part = super.createPart();
            Token token = getStartToken();
            part.setStartToken(token);
            return part;
        }

        /**
         * Returns the part class.
         */
        protected Class<T> getPartClass()
        {
            throw new RuntimeException(getClass().getName() + ": getPartClass not implemented");
        }
    }
}