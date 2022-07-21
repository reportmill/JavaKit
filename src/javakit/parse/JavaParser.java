/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.lang.reflect.*;
import java.util.*;
import snap.parse.*;

/**
 * A parser for java files.
 */
@SuppressWarnings("unused")
public class JavaParser extends Parser {

    // The exception, if one was hit
    Exception _exception;

    // The expression parser and statement parser
    Parser _ep, _sp, _ip;

    // The shared parser
    static JavaParser _shared = new JavaParser();

    /**
     * Constructor.
     */
    public JavaParser()
    {
        super();
        installHandlers();
    }

    /**
     * Returns the shared parser.
     */
    public static JavaParser getShared()
    {
        return _shared;
    }

    /**
     * Returns the shared expression parser.
     */
    public Parser getExprParser()
    {
        return _ep != null ? _ep : (_ep = new Parser(_shared.getRule("Expression")));
    }

    /**
     * Returns the shared expression parser.
     */
    public Parser getStmtParser()
    {
        return _sp != null ? _sp : (_sp = new Parser(_shared.getRule("Statement")));
    }

    /**
     * Returns the shared imports parser.
     */
    public Parser getImportsParser()
    {
        if (_ip != null) return _ip;
        Parser ip = new JavaParser();
        ip.setRule(ip.getRule("JavaFileImports"));
        return _ip = ip;
    }

    /**
     * Installs handlers.
     */
    protected void installHandlers()
    {
        // Get Main rule
        ParseRule mainRule = getRule();

        // Install handlers from list
        for (Class<? extends ParseHandler<?>> handlerClass : _handlerClasses)
            ParseUtils.installHandlerForClass(handlerClass, mainRule);
    }

    /**
     * Override so subclasses will find grammar file.
     */
    protected ParseRule createRule()
    {
        ParseRule rule = ParseUtils.loadRule(JavaParser.class, null);
        return rule;
    }

    /**
     * Returns a JavaFile for input Java.
     */
    public JFile getJavaFile(CharSequence anInput)
    {
        // Clear exception
        _exception = null;

        // Get parse node
        ParseNode node = null;
        try {
            node = anInput != null && anInput.length() > 0 ? parse(anInput) : null;
        } catch (ParseException e) {
            if (_exception == null) _exception = e;
        } catch (Exception e) {
            _exception = e;
            Token t = getToken();
            if (t != null) System.err.println("Exeption at line " + (t.getLineIndex() + 1));
            e.printStackTrace();
        }

        // Set JFile
        JFile jfile = node != null ? node.getCustomNode(JFile.class) : null;
        if (jfile == null) jfile = new JFile();
        jfile.setException(_exception);
        return jfile;
    }

    /**
     * Override to ignore exception.
     */
    protected void parseFailed(ParseRule aRule, ParseHandler aHandler)
    {
        if (_exception == null) {
            _exception = new ParseException(this, aRule);
            //System.err.println("JavaParse: " + _exception);
        }
    }

    /**
     * Override to declare tokenizer as JavaTokenzier.
     */
    public JavaTokenizer getTokenizer()
    {
        return (JavaTokenizer) super.getTokenizer();
    }

    /**
     * Returns the tokenizer.
     */
    protected Tokenizer createTokenizerImpl()
    {
        return new JavaTokenizer();
    }

    /**
     * A tokenizer for Java input.
     */
    public static class JavaTokenizer extends Tokenizer {

        /**
         * Creates a JavaTokenizer.
         */
        public JavaTokenizer()
        {
            setReadSingleLineComments(true);
            setReadMultiLineComments(true);
        }

        /**
         * Returns a token from the current char to multi-line comment termination or input end.
         */
        public Token getMultiLineCommentTokenMore(Token aSpclTkn)
        {
            return super.getMultiLineCommentTokenMore(aSpclTkn);
        }
    }

    /**
     * Java File Handler.
     */
    public static class JavaFileHandler extends JNodeParseHandler<JFile> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle PackageDecl
            if (anId == "PackageDecl")
                getPart().setPackageDecl(aNode.getCustomNode(JPackageDecl.class));

                // Handle ImportDecl
            else if (anId == "ImportDecl")
                getPart().addImportDecl(aNode.getCustomNode(JImportDecl.class));

                // Handle TypeDecl
            else if (anId == "TypeDecl") {
                if (aNode.getCustomNode() != null)
                    getPart().addClassDecl(aNode.getCustomNode(JClassDecl.class));
            }
        }

        protected Class<JFile> getPartClass()  { return JFile.class; }
    }

    /**
     * Java File Handler.
     */
    public static class JavaFileImportsHandler extends JNodeParseHandler<JFile> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle PackageDecl
            if (anId == "PackageDecl")
                getPart().setPackageDecl(aNode.getCustomNode(JPackageDecl.class));

                // Handle ImportDecl
            else if (anId == "ImportDecl")
                getPart().addImportDecl(aNode.getCustomNode(JImportDecl.class));
        }

        protected Class<JFile> getPartClass()  { return JFile.class; }
    }

    /**
     * PackageDecl Handler.
     */
    public static class PackageDeclHandler extends JNodeParseHandler<JPackageDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Modifiers
            //if(anId=="Modifiers") getPart().setMods(aNode.getCustomNode(JModifiers.class));

            // Handle Name
            if (anId == "Name")
                getPart().setNameExpr(aNode.getCustomNode(JExpr.class));

                // Otherwise ensure part is available
            else getPart();
        }

        protected Class<JPackageDecl> getPartClass()  { return JPackageDecl.class; }
    }

    /**
     * ImportDecl Handler.
     */
    public static class ImportDeclHandler extends JNodeParseHandler<JImportDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle static
            if (anId == "static")
                getPart().setStatic(true);

                // Handle Name
            else if (anId == "Name")
                getPart().setNameExpr(aNode.getCustomNode(JExpr.class));

                // Handle '*'
            else if (anId == "*")
                getPart().setInclusive(true);

                // Otherwise ensure part is available
            else getPart();
        }

        protected Class<JImportDecl> getPartClass()  { return JImportDecl.class; }
    }

    /**
     * TypeDecl Handler.
     */
    public static class TypeDeclHandler extends JNodeParseHandler<JClassDecl> {

        // The TypeDecl Modifiers
        JModifiers _mods;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle modifiers
            if (anId == "Modifiers")
                _mods = aNode.getCustomNode(JModifiers.class);

                // Handle ClassDecl, EnumDecl or AnnotationDecl
            else if (aNode.getCustomNode() instanceof JClassDecl) {
                _part = aNode.getCustomNode(JClassDecl.class);
                _part.setMods(_mods);
                _mods = null;
            }
        }

        protected Class<JClassDecl> getPartClass()  { return JClassDecl.class; }
    }

    /**
     * ClassDecl Handler.
     */
    public static class ClassDeclHandler extends JNodeParseHandler<JClassDecl> {

        // Whether in 'extends' mode (as opposed to 'implements' mode
        boolean _extending;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle "class" or "interface"
            if (anId == "interface")
                getPart().setClassType(JClassDecl.ClassType.Interface);

                // Handle Identifier
            else if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

                // Handle TypeParams
            else if (anId == "TypeParams")
                getPart().setTypeVars(aNode.getCustomNode(List.class));

                // Handle ExtendsList or ImplementsList mode and extendsList/implementsList
            else if (anId == "extends") _extending = true;
            else if (anId == "implements") _extending = false;
            else if (anId == "ClassType") {
                JType type = aNode.getCustomNode(JType.class);
                if (_extending) getPart().addExtendsType(type);
                else getPart().addImplementsType(type);
            }

            // Handle ClassBody
            else if (anId == "ClassBody") {
                JClassDecl body = aNode.getCustomNode(JClassDecl.class);
                _part.setMemberDecls(body.getMemberDecls());
            }
        }

        protected Class<JClassDecl> getPartClass()  { return JClassDecl.class; }
    }

    /**
     * ClassBody Handler.
     */
    public static class ClassBodyHandler extends JNodeParseHandler<JClassDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Make sure part is created at first node (aNode.getPattern()=="{")
            getPart();

            // Handle ClassBodyDecl (JavaMembers): ClassDecl, EnumDecl,
            // ConstrDecl, FieldDecl, MethodDecl, AnnotationDecl
            if (aNode.getCustomNode() instanceof JMemberDecl)
                getPart().addMemberDecl(aNode.getCustomNode(JMemberDecl.class));
        }

        protected Class<JClassDecl> getPartClass()  { return JClassDecl.class; }
    }

    /**
     * ClassBodyDecl Handler.
     */
    public static class ClassBodyDeclHandler extends JNodeParseHandler<JMemberDecl> {

        // Modifiers
        JModifiers _mods;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Modifiers
            if (anId == "Modifiers")
                _mods = aNode.getCustomNode(JModifiers.class);

                // Handle Member
            else if (aNode.getCustomNode() instanceof JMemberDecl) {
                _part = aNode.getCustomNode(JMemberDecl.class);
                _part.setMods(_mods);
                _mods = null;
            }
        }

        protected Class<JMemberDecl> getPartClass()  { return JMemberDecl.class; }
    }

    /**
     * Initializer Handler.
     */
    public static class InitializerHandler extends JNodeParseHandler<JClassStaticDecl> {
        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle "static"
            if (anId == "static")
                getPart().setStatic(true);

                // Handle Block
            else if (anId == "Block")
                getPart().setBlock(aNode.getCustomNode(JStmtBlock.class));
        }

        protected Class<JClassStaticDecl> getPartClass()  { return JClassStaticDecl.class; }
    }

    /**
     * EnumDecl Handler.
     */
    public static class EnumDeclHandler extends JNodeParseHandler<JClassDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle MethodDeclarator Identifier
            if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

                // Handle ImplementsList ClassType
            else if (anId == "ClassType")
                getPart().getImplementsTypes().add(aNode.getCustomNode(JType.class));

                // Handle EnumConstant
            else if (anId == "EnumConstant")
                getPart().addEnumConstant(aNode.getCustomNode(JEnumConst.class));

                // Handle ClassBodyDecl (JMemberDecl): ClassDecl, EnumDecl, ConstrDecl, FieldDecl, MethodDecl, AnnotationDecl
            else if (aNode.getCustomNode() instanceof JMemberDecl)
                getPart().addMemberDecl(aNode.getCustomNode(JMemberDecl.class));
        }

        /**
         * Override to set ClassType Enum.
         */
        public JClassDecl createPart()
        {
            JClassDecl cd = new JClassDecl();
            cd.setClassType(JClassDecl.ClassType.Enum);
            return cd;
        }
    }

    /**
     * EnumConstant Handler.
     */
    public static class EnumConstantHandler extends JNodeParseHandler<JEnumConst> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Modifiers
            if (anId == "Modifiers")
                getPart().setMods(aNode.getCustomNode(JModifiers.class));

                // Handle name Identifier
            else if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

                // Handle Arguments
            else if (anId == "Arguments")
                getPart().setArgs(aNode.getCustomNode(List.class));

                // Handle ClassBody
            else if (anId == "ClassBody")
                getPart().setClassBody(aNode.getString());
        }

        protected Class<JEnumConst> getPartClass()  { return JEnumConst.class; }
    }

    /**
     * TypeParam Handler.
     */
    public static class TypeParamHandler extends JNodeParseHandler<JTypeVar> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Identifier
            if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

                // Handle ClassType
            else if (anId == "ClassType")
                getPart().addType(aNode.getCustomNode(JType.class));
        }

        protected Class<JTypeVar> getPartClass()  { return JTypeVar.class; }
    }

    /**
     * TypeParams Handler.
     */
    public static class TypeParamsHandler extends ParseHandler<ArrayList<JTypeVar>> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle TypeParam
            if (anId == "TypeParam")
                getPart().add(aNode.getCustomNode(JTypeVar.class));
        }

        @Override
        protected Class getPartClass()
        {
            return ArrayList.class;
        }
    }

    /**
     * FieldDecl Handler.
     */
    public static class FieldDeclHandler extends JNodeParseHandler<JFieldDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Type
            if (anId == "Type")
                getPart().setType(aNode.getCustomNode(JType.class));

                // Handle VarDecl(s)
            else if (anId == "VarDecl") {
                JVarDecl vd = aNode.getCustomNode(JVarDecl.class);
                getPart().addVarDecl(vd);
            }
        }

        protected Class<JFieldDecl> getPartClass()  { return JFieldDecl.class; }
    }

    /**
     * MethodDecl Handler.
     */
    public static class MethodDeclHandler extends JNodeParseHandler<JMethodDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle TypeParams
            if (anId == "TypeParams")
                getPart().setTypeVars(aNode.getCustomNode(List.class));

                // Handle ResultType
            else if (anId == "ResultType")
                getPart().setType(aNode.getCustomNode(JType.class));

                // Handle MethodDeclarator Identifier
            else if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

                // Handle MethodDeclarator FormalParam
            else if (anId == "FormalParam")
                getPart().addParam(aNode.getCustomNode(JVarDecl.class));

                // Handle ThrowsList
            else if (anId == "ThrowsList")
                getPart().setThrowsList(aNode.getCustomNode(List.class));

                // Handle Block
            else if (anId == "Block")
                getPart().setBlock(aNode.getCustomNode(JStmtBlock.class));
        }

        protected Class<JMethodDecl> getPartClass()  { return JMethodDecl.class; }
    }

    /**
     * ConstrDecl Handler.
     */
    public static class ConstrDeclHandler extends JNodeParseHandler<JConstrDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle TypeParams
            if (anId == "TypeParams")
                getPart().setTypeVars(aNode.getCustomNode(List.class));

                // Handle Identifier
            else if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

                // Handle FormalParam
            else if (anId == "FormalParam")
                getPart().addParam(aNode.getCustomNode(JVarDecl.class));

                // Handle ThrowsList
            else if (anId == "ThrowsList")
                getPart().setThrowsList(aNode.getCustomNode(List.class));

                // Handle BlockStatement start "{"
            else if (anId == "{") {
                JStmtBlock block = new JStmtBlock();
                block.setStartToken(aNode.getStartToken());
                block.setEndToken(aNode.getEndToken());
                getPart().setBlock(block);
            }

            // Handle ConstrCall
            else if (anId == "ConstrCall")
                getPart().getBlock().addStatement(aNode.getCustomNode(JStmtConstrCall.class));

                // Handle BlockStatement
            else if (anId == "BlockStatement")
                getPart().getBlock().addStatement(aNode.getCustomNode(JStmt.class));

                // Handle BlockStatement end
            else if (aNode.getPattern() == "}")
                getPart().getBlock().setEndToken(aNode.getEndToken());
        }

        protected Class<JConstrDecl> getPartClass()  { return JConstrDecl.class; }
    }

    /**
     * ThrowsList Handler.
     */
    public static class ThrowsListHandler extends ParseHandler<ArrayList<JExpr>> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "Name")
                getPart().add(aNode.getCustomNode(JExpr.class));
        }

        @Override
        protected Class getPartClass()  { return ArrayList.class; }
    }

    /**
     * ConstrCall Handler.
     */
    public static class ConstrCallHandler extends JNodeParseHandler<JStmtConstrCall> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Identifier
            if (anId == "Identifier")
                getPart().addId(aNode.getCustomNode(JExprId.class));

                // Handle "this"/"super"
            else if (anId == "this" || anId == "super") {
                JExprId id = new JExprId(aNode.getString());
                id.setStartToken(aNode.getStartToken());
                id.setEndToken(aNode.getEndToken());
                getPart().addId(id);
            }

            // Handle Arguments
            else if (anId == "Arguments")
                getPart().setArgs(aNode.getCustomNode(List.class));
        }

        protected Class<JStmtConstrCall> getPartClass()  { return JStmtConstrCall.class; }
    }

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
            // Handle Expression
            if (getPart().getConditional() == null)
                getPart().setConditional(aNode.getCustomNode(JExpr.class));
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
            // Handle JavaExpression rules
            if (aNode.getCustomNode() instanceof JExpr && _part == null)
                getPart().setExpr(aNode.getCustomNode(JExpr.class));

                // Handle post increment/decrement
            else if (anId == "++")
                getPart().setExpr(new JExprMath(JExprMath.Op.PostIncrement, getPart().getExpr()));
            else if (anId == "--")
                getPart().setExpr(new JExprMath(JExprMath.Op.PostDecrement, getPart().getExpr()));

                // Handle Assign Expression
            else if (anId == "Expression") {
                JExpr expr = aNode.getCustomNode(JExpr.class);
                getPart().setExpr(new JExprMath(JExprMath.Op.Assign, getPart().getExpr(), expr));
            }
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
                getPart().setStmt(aNode.getCustomNode(JStmt.class));

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

        // The current part index (0=init, 1=conditional, 2=update)
        int partIndex = 0;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Type
            if (anId == "Type") {
                JType type = aNode.getCustomNode(JType.class);
                JStmtVarDecl svd = new JStmtVarDecl();
                svd.setType(type);
                getPart().setInitDecl(svd);
            }

            // Handle Identifier
            else if (anId == "Identifier") {
                JVarDecl vd = new JVarDecl();
                vd.setId(aNode.getCustomNode(JExprId.class));
                getPart().getInitDecl().addVarDecl(vd);
            }

            // Handle ForInit VarDeclStmt
            else if (anId == "VarDeclStmt")
                getPart().setInitDecl(aNode.getCustomNode(JStmtVarDecl.class));

                // Handle ForInit ExprStatement(s) or ForUpdate ExprStatement(s)
            else if (anId == "ExprStatement") {
                JStmtExpr se = aNode.getCustomNode(JStmtExpr.class);
                if (partIndex == 0) getPart().addInitStmt(se);
                else getPart().addUpdateStmt(se);
            }

            // Handle init or conditional Expression
            else if (anId == "Expression")
                getPart().setConditional(aNode.getCustomNode(JExpr.class));

                // Handle separator
            else if (anId == ";") {
                partIndex++;
                getPart()._forEach = false;
            }

            // Handle Statement
            else if (anId == "Statement")
                getPart().setStatement(aNode.getCustomNode(JStmt.class));

                // Handle anything else
            else getPart();
        }

        /**
         * Override to clear partIndex.
         */
        public JStmtFor parsedAll()
        {
            partIndex = 0;
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
    public static class ConditionalOrExprHandler extends BinaryExprHandler {
    }

    /**
     * ConditionalAndExpr Handler.
     */
    public static class ConditionalAndExprHandler extends BinaryExprHandler {
    }

    /**
     * InclusiveOrExpr Handler.
     */
    public static class InclusiveOrExprHandler extends BinaryExprHandler {
    }

    /**
     * ExclusiveOrExpr Handler.
     */
    public static class ExclusiveOrExprHandler extends BinaryExprHandler {
    }

    /**
     * AndExpr Handler.
     */
    public static class AndExprHandler extends BinaryExprHandler {
    }

    /**
     * EqualityExpr Handler.
     */
    public static class EqualityExprHandler extends BinaryExprHandler {
    }

    /**
     * RelationalExpr Handler.
     */
    public static class RelationalExprHandler extends BinaryExprHandler {
    }

    /**
     * ShiftExpr Handler.
     */
    public static class ShiftExprHandler extends BinaryExprHandler {
    }

    /**
     * AdditiveExpr Handler.
     */
    public static class AdditiveExprHandler extends BinaryExprHandler {
    }

    /**
     * MultiplicativeExpr Handler.
     */
    public static class MultiplicativeExprHandler extends BinaryExprHandler {
    }

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
        protected Class getPartClass()
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
    private abstract static class JNodeParseHandler<T extends JNode> extends ParseHandler<T> {

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

    /**
     * Handler classes (from ParseUtils.printHandlerClassesForParentClass()).
     */
    private Class<? extends ParseHandler<?>>[]  _handlerClasses = new Class[] {
        AnnotationDeclHandler.class, LiteralHandler.class, LambdaExprHandler.class,
        ArrayInitHandler.class, AllocExprHandler.class, ArgumentsHandler.class,
        PrimarySuffixHandler.class, PrimaryPrefixHandler.class, PrimaryExprHandler.class,
        InstanceOfExprHandler.class, CastExprHandler.class, PreDecrementExprHandler.class,
        PreIncrementExprHandler.class, UnaryExprHandler.class, MultiplicativeExprHandler.class,
        AdditiveExprHandler.class, ShiftExprHandler.class, RelationalExprHandler.class,
        EqualityExprHandler.class, AndExprHandler.class, ExclusiveOrExprHandler.class,
        InclusiveOrExprHandler.class, ConditionalAndExprHandler.class, ConditionalOrExprHandler.class,
        ConditionalExprHandler.class, ResultTypeHandler.class, PrimitiveTypeHandler.class,
        ClassTypeHandler.class, TypeHandler.class, NameHandler.class,
        IdentifierHandler.class, ExpressionHandler.class, TryStatementHandler.class,
        SynchronizedStatementHandler.class, ThrowStatementHandler.class, ReturnStatementHandler.class,
        ContinueStatementHandler.class, BreakStatementHandler.class, ForStatementHandler.class,
        DoStatementHandler.class, WhileStatementHandler.class, IfStatementHandler.class,
        SwitchLabelHandler.class, SwitchStatementHandler.class, ExprStatementHandler.class,
        EmptyStatementHandler.class, VarDeclStmtHandler.class, VarDeclHandler.class,
        FormalParamHandler.class, BlockStatementHandler.class, BlockHandler.class,
        LabeledStatementHandler.class, AssertStatementHandler.class, ModifiersHandler.class,
        StatementHandler.class, ConstrCallHandler.class, ThrowsListHandler.class,
        ConstrDeclHandler.class, MethodDeclHandler.class, FieldDeclHandler.class,
        TypeParamsHandler.class, TypeParamHandler.class, EnumConstantHandler.class,
        EnumDeclHandler.class, InitializerHandler.class, ClassBodyDeclHandler.class,
        ClassBodyHandler.class, ClassDeclHandler.class, TypeDeclHandler.class,
        ImportDeclHandler.class, PackageDeclHandler.class, JavaFileImportsHandler.class,
        JavaFileHandler.class
    };
}