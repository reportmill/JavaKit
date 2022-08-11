/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import java.util.stream.Stream;
import snap.parse.*;

/**
 * A parser for java files.
 */
@SuppressWarnings({"unused", "StringEquality"})
public class JavaParser extends JavaParserStmt {

    // The exception, if one was hit
    private Exception  _exception;

    // An expression parser created from subset of JavaParser
    private Parser  _exprParser;

    // A statement parser created from subset of JavaParser
    private Parser  _stmtParser;

    // An imports parser created from subset of JavaParser
    private Parser  _importsParser;

    // The shared parser
    private static JavaParser _shared = new JavaParser();

    /**
     * Constructor.
     */
    public JavaParser()
    {
        super();
    }

    /**
     * Returns the shared parser.
     */
    public static JavaParser getShared()
    {
        if (_shared != null) return _shared;
        return _shared;
    }

    /**
     * Returns the shared expression parser.
     */
    public Parser getExprParser()
    {
        if (_exprParser != null) return _exprParser;
        ParseRule exprRule = getShared().getRule("Expression");
        return _exprParser = new Parser(exprRule);
    }

    /**
     * Returns the shared expression parser.
     */
    public Parser getStmtParser()
    {
        if (_stmtParser != null) return _stmtParser;
        ParseRule stmtRule = getShared().getRule("Statement");
        return _stmtParser = new Parser(stmtRule);
    }

    /**
     * Returns the shared imports parser.
     */
    public Parser getImportsParser()
    {
        if (_importsParser != null) return _importsParser;
        Parser javaParser = new JavaParser();
        ParseRule importsRule = javaParser.getRule("JavaFileImports");
        javaParser.setRule(importsRule);
        return _importsParser = javaParser;
    }

    /**
     * Override so subclasses will find grammar file.
     */
    protected ParseRule createRule()
    {
        // Create rule
        ParseRule rule = ParseUtils.loadRule(JavaParser.class, null);

        // Install handlers from list
        for (Class<? extends ParseHandler<?>> handlerClass : _handlerClasses)
            ParseUtils.installHandlerForClass(handlerClass, rule);

        // Return
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
        }

        catch (ParseException e) {
            if (_exception == null) _exception = e;
        }

        catch (Exception e) {
            _exception = e;
            Token t = getToken();
            if (t != null) System.err.println("Exeption at line " + (t.getLineIndex() + 1));
            e.printStackTrace();
        }

        // Get JFile
        JFile jfile = node != null ? node.getCustomNode(JFile.class) : null;
        if (jfile == null)
            jfile = new JFile();

        // Set exception
        jfile.setException(_exception);

        // Return
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
     * Override to declare tokenizer as JavaTokenizer.
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
         * Override to make public for external use.
         */
        public Token getMultiLineCommentTokenMore(Token aSpclTkn)
        {
            return super.getMultiLineCommentTokenMore(aSpclTkn);
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

    // TeaVM needs this to exist, otherwise RuleNames.intern() != RuleName (and id == RuleName doesn't work)
    private static String[] _allRuleNames = { "JavaFile", "PackageDecl", "Annotation", "Name", "Identifier", "NormalAnnotation",
            "MemberValuePairs", "MemberValuePair", "MemberValue", "MemberValueArrayInit", "ConditionalExpr", "ConditionalOrExpr",
            "ConditionalAndExpr", "InclusiveOrExpr", "ExclusiveOrExpr", "AndExpr", "EqualityExpr", "InstanceOfExpr", "RelationalExpr",
            "ShiftExpr", "AdditiveExpr", "MultiplicativeExpr", "UnaryExpr", "PreIncrementExpr", "PrimaryExpr", "PrimaryPrefix", "Literal",
            "IntegerLiteral", "IntLiteral", "HexLiteral", "OctalLiteral", "FloatLiteral", "CharacterLiteral", "StringLiteral",
            "BooleanLiteral", "NullLiteral", "ClassType", "TypeArgs", "TypeArg", "ReferenceType", "PrimitiveType", "WildcardBounds",
            "LambdaExpr", "Expression", "AssignmentOp", "Block", "BlockStatement", "Modifiers", "Modifier", "Type", "VarDeclStmt",
            "VarDecl", "VarInit", "ArrayInit", "Statement", "LabeledStatement", "AssertStatement", "EmptyStatement", "ExprStatement",
            "PreDecrementExpr", "SwitchStatement", "SwitchLabel", "IfStatement", "WhileStatement", "DoStatement", "ForStatement",
            "ForInit", "ExprStmtList", "BreakStatement", "ContinueStatement", "ReturnStatement", "ThrowStatement", "SynchronizedStatement",
            "TryStatement", "FormalParam", "ClassDecl", "TypeParams", "TypeParam", "TypeBound", "ExtendsList", "ImplementsList",
            "ClassBody", "ClassBodyDecl", "Initializer", "MemberDecl", "EnumDecl", "EnumConstant", "Arguments", "ConstrDecl",
            "FormalParams", "ThrowsList", "ConstrCall", "FieldDecl", "MethodDecl", "ResultType", "AnnotationDecl", "AllocExpr",
            "ArrayDimsAndInits", "PrimarySuffix", "MemberSelector", "UnaryExprNotPlusMinus", "CastLook", "CastExpr", "PostfixExpr",
            "ShiftRightUnsigned", "ShiftRight", "SingleMemberAnnotation", "MarkerAnnotation", "ImportDecl", "TypeDecl",
            "JavaFileImports",
            "boolean", "char", "byte", "short", "int", "long", "float", "double",
            "public", "private", "protected", "static", "default", "abstract",
            "this", "super", "extends", "implements", "interface", "...",
            "+", "-", "*", "/", "++", "--", "==", "+=", "-=", "{", "[", "->",
    };
    public static String[] _allRuleNamesIntern = Stream.of(_allRuleNames).map(s -> s.intern()).toArray(size -> new String[size]);

}