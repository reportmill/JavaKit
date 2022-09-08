package javakit.parse;

import java.util.List;

/**
 * A class to convert a JNode (tree) to a String.
 */
public class JavaWriter {

    // The StringBuffer
    StringBuffer _sb = new StringBuffer();

    // The indent string
    String _indentStr = "    ";

    // The indent level
    int _indent;

    // Whether at line end
    boolean _lineStart;

    /**
     * Returns the string for JNode.
     */
    public String getString(JNode aNode)
    {
        _sb.setLength(0);
        _indent = 0;
        writeJNode(aNode);
        return _sb.toString();
    }

    /**
     * Writes a JNode.
     */
    public void writeJNode(JNode aNode)
    {
        if (aNode instanceof JFile) writeJFile((JFile) aNode);
        else if (aNode instanceof JPackageDecl) writeJPackageDecl((JPackageDecl) aNode);
        else if (aNode instanceof JImportDecl) writeJImportDecl((JImportDecl) aNode);
        else if (aNode instanceof JClassDecl) writeJClassDecl((JClassDecl) aNode);
        else if (aNode instanceof JFieldDecl) writeJFieldDecl((JFieldDecl) aNode);
        else if (aNode instanceof JMethodDecl) writeJMethodDecl((JMethodDecl) aNode);
        else if (aNode instanceof JStmt) writeJStmt((JStmt) aNode);
        else if (aNode instanceof JExpr) writeJExpr((JExpr) aNode);
        else if (aNode instanceof JType) writeJType((JType) aNode);
        else if (aNode instanceof JVarDecl) writeJVarDecl((JVarDecl) aNode);
        else if (aNode instanceof JEnumConst) writeJEnumConst((JEnumConst) aNode);
        else if (aNode instanceof JTypeVar) writeJTypeVar((JTypeVar) aNode);
        else if (aNode instanceof JModifiers) writeJModifiers((JModifiers) aNode);
        else append("JavaWriter: write" + aNode.getClass().getSimpleName() + " not implemented");
    }

    /**
     * Writes a list of JNodes joined by string.
     */
    public void writeJNodesJoined(List<? extends JNode> theNodes, String aStr)
    {
        JNode last = theNodes.size() > 0 ? theNodes.get(theNodes.size() - 1) : null;
        for (JNode node : theNodes) {
            writeJNode(node);
            if (node != last) append(aStr);
        }
    }

    /**
     * Write a JPackageDecl.
     */
    public void writeJPackageDecl(JPackageDecl aPDecl)
    {
        append("package ");
        append(aPDecl.getName());
        append(';').endln();
    }

    /**
     * Write a JFile.
     */
    public void writeJFile(JFile aJFile)
    {
        // Append Package declaration
        JPackageDecl pdecl = aJFile.getPackageDecl();
        if (pdecl != null)
            writeJPackageDecl(pdecl);

        // Append imports
        for (JImportDecl imp : aJFile.getImportDecls())
            writeJImportDecl(imp);
        endln();

        // Append class decls
        writeJClassDecl(aJFile.getClassDecl());
    }

    /**
     * Write a JImportDecl.
     */
    public void writeJImportDecl(JImportDecl anImp)
    {
        String iname = anImp.getName();
        append("import ");
        if (anImp.isStatic()) append("static ");
        append(iname);
        if (anImp.isInclusive()) append(".*");
        append(';').endln();
    }

    /**
     * Writes a JClassDecl.
     */
    public void writeJClassDecl(JClassDecl aClassDecl)
    {
        // Append class label with modifiers: public class XXX ...
        String className = aClassDecl.getSimpleName();
        JModifiers mods = aClassDecl.getMods();
        writeJModifiers(mods);
        append(aClassDecl.isClass() ? "class " : aClassDecl.isInterface() ? "interface " : "enum ");
        append(className).append(' ');

        // Append extends types
        List<JType> etypes = aClassDecl.getExtendsTypes();
        JType elast = etypes.size() > 0 ? etypes.get(etypes.size() - 1) : null;
        if (etypes.size() > 0) append("extends ");
        for (JType etyp : etypes) {
            writeJType(etyp);
            if (etyp != elast) append(", ");
            else append(' ');
        }

        // Append implements types
        List<JType> implementsTypes = aClassDecl.getImplementsTypes();
        JType lastImplType = implementsTypes.size() > 0 ? implementsTypes.get(implementsTypes.size() - 1) : null;
        if (implementsTypes.size() > 0)
            append("implements ");
        for (JType implType : implementsTypes) {
            writeJType(implType);
            if (implType != lastImplType)
                append(',');
            append(' ');
        }

        // Write class label close char
        append('{').endln().endln();
        indent();

        // Append enum constants
        List<JEnumConst> enumConstants = aClassDecl.getEnumConstants();
        if (enumConstants.size() > 0) {
            writeJNodesJoined(enumConstants, ", ");
            endln();
        }

        // Append fields
        JFieldDecl[] fieldDecls = aClassDecl.getFieldDecls();
        for (JFieldDecl fieldDecl : fieldDecls) {
            writeJFieldDecl(fieldDecl);
            endln();
        }

        // Append methods
        JMethodDecl[] methodDecls = aClassDecl.getMethodDecls();
        JMethodDecl lastMethod = methodDecls.length > 0 ? methodDecls[methodDecls.length - 1] : null;
        for (JMethodDecl methodDecl : aClassDecl.getMethodDecls()) {
            writeJMethodDecl(methodDecl);
            if (methodDecl != lastMethod)
                endln();
        }

        // Terminate
        outdent();
        append('}').endln();

        // Append inner classes
        JClassDecl[] classDecls = aClassDecl.getClassDecls();
        for (JClassDecl classDecl : classDecls) {
            endln();
            writeJClassDecl(classDecl);
        }
    }

    /**
     * Write JEnumConst.
     */
    public void writeJEnumConst(JEnumConst aConst)
    {
        append(aConst.getName());
    }

    /**
     * Write JTypeVar.
     */
    public void writeJTypeVar(JTypeVar aTV)
    {
        append(aTV.getName());
    }

    /**
     * Writes a JFieldDecl.
     */
    public void writeJFieldDecl(JFieldDecl aFDecl)
    {
        // Write modifiers
        JModifiers mods = aFDecl.getMods();
        writeJModifiers(mods);

        // Get first var decl
        JVarDecl vd = aFDecl.getVarDecls().get(0);

        // Write var decl and terminator
        writeJVarDecl(vd);
        append(';').endln();
    }

    /**
     * Writes a JMethodDecl.
     */
    public void writeJMethodDecl(JMethodDecl aMDecl)
    {
        // Write modifiers
        JModifiers mods = aMDecl.getMods();
        writeJModifiers(mods);

        // Write return type (if not empty/void)
        JType rtype = aMDecl.getType();
        if (rtype != null) writeJType(rtype);
        append(' ');

        // Write method name and args start char
        append(aMDecl.getName()).append("(");

        // Write parameters
        List<JVarDecl> params = aMDecl.getParameters();
        JVarDecl last = params.size() > 0 ? params.get(params.size() - 1) : null;
        for (JVarDecl param : aMDecl.getParameters()) {
            writeJVarDecl(param);
            if (param != last) append(", ");
        }

        // Write parameters close char
        append(") ");

        // Write method block
        writeJStmtBlock(aMDecl.getBlock(), false);
    }

    /**
     * Writes a JVarDecl.
     */
    public void writeJVarDecl(JVarDecl aVarDecl)
    {
        // Write type
        JType varDeclType = aVarDecl.getType();
        if (varDeclType != null) {
            String tstr = getTypeString(varDeclType);
            for (int i = 0, iMax = aVarDecl.getArrayCount(); i < iMax; i++)
                tstr += "[]";
            append(tstr).append(' ');
        }

        // Write name
        JExprId nameExpr = aVarDecl.getId();
        if (nameExpr != null)
            writeJExprId(nameExpr);

        // Write initializer
        JExpr initExpr = aVarDecl.getInitializer();
        if (initExpr != null) {
            append(" = ");
            writeJExpr(initExpr);
        }
    }

    /**
     * Writes a JModifiers.
     */
    public void writeJModifiers(JModifiers aMod)
    {
        String str = getJModifierString(aMod);
        append(str);
    }

    /**
     * Returns a string for JModifier.
     */
    public String getJModifierString(JModifiers aMod)
    {
        String str = "";
        if (aMod == null) return str;
        if (aMod.isPublic()) str += "public ";
        if (aMod.isStatic()) str += "static ";
        if (aMod.isAbstract()) str += "abstract ";
        return str;
    }

    /**
     * Writes a type.
     */
    public void writeJType(JType aType)
    {
        String str = getTypeString(aType);
        if (str.length() == 0) return;
        append(str);
    }

    /**
     * Returns a type string.
     */
    public String getTypeString(JType aType)
    {
        String name = aType.getName();
        if (aType.isArrayType())
            name = name + "[]";
        return name;
    }

    /**
     * Writes a JStmt.
     */
    public void writeJStmt(JStmt aStmt)
    {
        if (aStmt instanceof JStmtAssert) writeJStmtAssert((JStmtAssert) aStmt);
        else if (aStmt instanceof JStmtBlock) writeJStmtBlock((JStmtBlock) aStmt, false);
        else if (aStmt instanceof JStmtBreak) writeJStmtBreak((JStmtBreak) aStmt);
        else if (aStmt instanceof JStmtClassDecl) writeJStmtClassDecl((JStmtClassDecl) aStmt);
        else if (aStmt instanceof JStmtConstrCall) writeJStmtConstrCall((JStmtConstrCall) aStmt);
        else if (aStmt instanceof JStmtContinue) writeJStmtContinue((JStmtContinue) aStmt);
        else if (aStmt instanceof JStmtDo) writeJStmtDo((JStmtDo) aStmt);
        else if (aStmt instanceof JStmtEmpty) writeJStmtEmpty((JStmtEmpty) aStmt);
        else if (aStmt instanceof JStmtExpr) writeJStmtExpr((JStmtExpr) aStmt);
        else if (aStmt instanceof JStmtFor) writeJStmtFor((JStmtFor) aStmt);
        else if (aStmt instanceof JStmtIf) writeJStmtIf((JStmtIf) aStmt);
        else if (aStmt instanceof JStmtLabeled) writeJStmtLabeled((JStmtLabeled) aStmt);
        else if (aStmt instanceof JStmtReturn) writeJStmtReturn((JStmtReturn) aStmt);
        else if (aStmt instanceof JStmtSwitch) writeJStmtSwitch((JStmtSwitch) aStmt);
        else if (aStmt instanceof JStmtSynchronized) writeJStmtSynchronized((JStmtSynchronized) aStmt);
        else if (aStmt instanceof JStmtThrow) writeJStmtThrow((JStmtThrow) aStmt);
        else if (aStmt instanceof JStmtTry) writeJStmtTry((JStmtTry) aStmt);
        else if (aStmt instanceof JStmtVarDecl) writeJStmtVarDecl((JStmtVarDecl) aStmt);
        else if (aStmt instanceof JStmtWhile) writeJStmtWhile((JStmtWhile) aStmt);
        else throw new RuntimeException("JavaWriter.writeJStmt: Unsupported statement " + aStmt.getClass());
        //else append(aStmt.getString()).endln();
    }

    /**
     * Writes a JStmtAssert.
     */
    public void writeJStmtAssert(JStmtAssert aStmt)
    {
        JExpr cond = aStmt.getConditional();
        JExpr expr = aStmt.getExpr();
        append("assert(");
        writeJExpr(cond);
        append(");").endln();
    }

    /**
     * Writes a JStmtBlock.
     */
    public void writeJStmtBlock(JStmtBlock aBlock, boolean doSemicolon)
    {
        // Write start and indent
        append('{').endln();
        indent();

        // Write statements
        if (aBlock != null)
            for (JStmt stmt : aBlock.getStatements())
                writeJStmt(stmt);

        // Outdent and terminate
        outdent();
        append(doSemicolon ? "};" : "}").endln();
    }

    /**
     * Writes a JStmtBreak.
     */
    public void writeJStmtBreak(JStmtBreak aStmt)
    {
        append("break");
        JExpr label = aStmt.getLabel();
        if (label != null) {
            append(' ');
            writeJExpr(label);
        }
        append(';').endln();
    }

    /**
     * Writes a JStmtClassDecl.
     */
    public void writeJStmtClassDecl(JStmtClassDecl aStmt)
    {
        JClassDecl cdecl = aStmt.getClassDecl();
        writeJClassDecl(cdecl);
    }

    /**
     * Writes a JStmtConstrCall.
     */
    public void writeJStmtConstrCall(JStmtConstrCall aStmt)
    {
        // Write label (this, super, Class.this, etc.) and open char
        List<JExprId> ids = aStmt.getIds();
        writeJNodesJoined(ids, ".");
        append('(');

        // Write args and close char
        List<JExpr> args = aStmt.getArgs();
        writeJNodesJoined(args, ", ");
        append(");").endln();
    }

    /**
     * Writes a JStmtContinue.
     */
    public void writeJStmtContinue(JStmtContinue aStmt)
    {
        append("continue");
        JExpr label = aStmt.getLabel();
        if (label != null) {
            append(' ');
            writeJExpr(label);
        }
        append(';').endln();
    }

    /**
     * Writes a JStmtDo.
     */
    public void writeJStmtDo(JStmtDo aStmt)
    {
        append("do ");
        JStmt stmt = aStmt.getStatement();
        writeJStmt(stmt);
        JExpr cond = aStmt.getConditional();
        append("while(");
        writeJExpr(cond);
        append(");").endln();
    }

    /**
     * Writes a JStmtEmpty.
     */
    public void writeJStmtEmpty(JStmtEmpty aStmt)
    {
        append(';').endln();
    }

    /**
     * Writes a JStmtExpr.
     */
    public void writeJStmtExpr(JStmtExpr aStmt)
    {
        writeJExpr(aStmt.getExpr());
        append(';').endln();
    }

    /**
     * Writes a JStmtIf.
     */
    public void writeJStmtIf(JStmtIf aStmt)
    {
        // Write if(), conditional and statement
        append("if (");
        writeJExpr(aStmt.getConditional());
        append(") ");
        writeJStmt(aStmt.getStatement());

        // Write else
        if (aStmt.getElseStatement() != null) {
            append("else ");
            writeJStmt(aStmt.getElseStatement());
        }
    }

    /**
     * Writes a JStmtFor.
     */
    public void writeJStmtFor(JStmtFor aStmt)
    {
        JStmtVarDecl init = aStmt.getInitDecl();
        JVarDecl initVD = init != null ? init.getVarDecls().get(0) : null;
        JExpr cond = aStmt.getConditional();

        append("for (");

        // Handle for(each)
        if (aStmt.isForEach()) {
            writeJVarDecl(initVD);
            append(" : ");
            writeJExpr(cond);
        }

        // Handle conventional for()
        else {
            if (initVD != null)
                writeJVarDecl(initVD);
            append(';');
            if (cond != null)
                writeJExpr(cond);
            append(';');
            List<JStmtExpr> updStmts = aStmt.getUpdateStmts();
            JStmtExpr last = updStmts.size() > 0 ? updStmts.get(updStmts.size() - 1) : null;
            for (JStmtExpr updStmt : updStmts) {
                writeJExpr(updStmt.getExpr());
                if (updStmt != last) append(", ");
            }
        }

        // Write for closing paren and statement
        append(") ");
        JStmt stmt = aStmt.getStatement();
        if (stmt instanceof JStmtBlock)
            writeJStmt(stmt);
        else {
            endln().indent();
            writeJStmt(stmt);
            outdent();
        }
    }

    /**
     * Writes a JStmtLabeled.
     */
    public void writeJStmtLabeled(JStmtLabeled aStmt)
    {
        JExprId label = aStmt.getLabel();
        writeJExprId(label);
        append(':');
        JStmt stmt = aStmt.getStmt();
        if (stmt != null) {
            append(' ');
            writeJStmt(stmt);
        } else endln();
    }

    /**
     * Writes a JStmtReturn.
     */
    public void writeJStmtReturn(JStmtReturn aStmt)
    {
        append("return");
        JExpr expr = aStmt.getExpr();
        if (expr != null) {
            append(' ');
            writeJExpr(expr);
        }
        append(';').endln();
    }

    /**
     * Writes a JStmtSwitch.
     */
    public void writeJStmtSwitch(JStmtSwitch aStmt)
    {
        JExpr expr = aStmt.getExpr();
        append("switch (");
        writeJExpr(expr);
        append(") {").endln();
        indent();
        for (JStmtSwitch.SwitchLabel lbl : aStmt.getSwitchLabels())
            writeJStmtSwitchLabel(lbl);
        outdent();
        append('}').endln();
    }

    /**
     * Writes a JStmtSwitch.
     */
    public void writeJStmtSwitchLabel(JStmtSwitch.SwitchLabel aSL)
    {
        if (aSL.isDefault()) append("default: ");
        else {
            append("case ");
            writeJExpr(aSL.getExpr());
            append(": ");
        }
        for (JStmt stmt : aSL.getStatements())
            writeJStmt(stmt);
    }

    /**
     * Writes a JStmtSynchronized.
     */
    public void writeJStmtSynchronized(JStmtSynchronized aStmt)
    {
        writeJStmtBlock(aStmt.getBlock(), true);
    }

    /**
     * Writes a JStmtThrow.
     */
    public void writeJStmtThrow(JStmtThrow aStmt)
    {
        append("throw ");
        writeJExpr(aStmt.getExpr());
        append(';').endln();
    }

    /**
     * Writes a JStmtTry.
     */
    public void writeJStmtTry(JStmtTry aStmt)
    {
        JStmtBlock tryBlock = aStmt.getTryBlock();
        List<JStmtTry.CatchBlock> catchBlocks = aStmt.getCatchBlocks();
        JStmtBlock finBlock = aStmt.getFinallyBlock();

        // Write try block
        append("try ");
        writeJStmtBlock(tryBlock, false);

        // Write catch blocks
        for (JStmtTry.CatchBlock cb : catchBlocks) {
            append("catch(");
            writeJVarDecl(cb.getParameter());
            append(") ");
            writeJStmtBlock(cb.getBlock(), false);
        }

        // Write finally block
        if (finBlock != null) {
            append("finally ");
            writeJStmtBlock(finBlock, false);
        }
    }

    /**
     * Writes a JStmtVarDecl.
     */
    public void writeJStmtVarDecl(JStmtVarDecl aStmt)
    {
        for (JVarDecl vd : aStmt.getVarDecls()) {
            writeJVarDecl(vd);
            append(';').endln();
        }
    }

    /**
     * Writes a JStmtWhile.
     */
    public void writeJStmtWhile(JStmtWhile aStmt)
    {
        // Write "while", conditional and statement
        append("while(");
        writeJExpr(aStmt.getConditional());
        append(") ");
        writeJStmt(aStmt.getStatement());
    }

    /**
     * Writes a JExpr.
     */
    public void writeJExpr(JExpr aExpr)
    {
        if (aExpr instanceof JExprAlloc) writeJExprAlloc((JExprAlloc) aExpr);
        else if (aExpr instanceof JExprArrayIndex) writeJExprArrayIndex((JExprArrayIndex) aExpr);
        else if (aExpr instanceof JExprChain) writeJExprChain((JExprChain) aExpr);
        else if (aExpr instanceof JExpr.CastExpr) writeJExprCast((JExpr.CastExpr) aExpr);
        else if (aExpr instanceof JExprId) writeJExprId((JExprId) aExpr);
        else if (aExpr instanceof JExpr.InstanceOfExpr) writeJExprInstanceOf((JExpr.InstanceOfExpr) aExpr);
        else if (aExpr instanceof JExprLambda) writeJExprLambda((JExprLambda) aExpr);
        else if (aExpr instanceof JExprLiteral) writeJExprLiteral((JExprLiteral) aExpr);
        else if (aExpr instanceof JExprMath) writeJExprMath((JExprMath) aExpr);
        else if (aExpr instanceof JExprMethodCall) writeJExprMethodCall((JExprMethodCall) aExpr);
        else if (aExpr instanceof JExprMethodRef) writeJExprMethodRef((JExprMethodRef) aExpr);
        else if (aExpr instanceof JExprType) writeJExprType((JExprType) aExpr);
        else throw new RuntimeException("JavaWriter.writeJExpr: Unsupported expression " + aExpr.getClass());
        //else append(aExpr.getString());
    }

    /**
     * Writes a JExprAlloc.
     */
    public void writeJExprAlloc(JExprAlloc aExpr)
    {
        // Get type - if array, handle separate
        JType typ = aExpr.getType();
        if (typ.isArrayType()) {

            // Append 'new Array[', the dimension expression (if set) and dimension close char
            append("new ");
            writeJType(typ);
            append('[');
            JExpr dim = aExpr.getArrayDims();
            if (dim != null) writeJExpr(dim);
            append(']');

            // If array init expresions are set, append them
            List<JExpr> inits = aExpr.getArrayInits();
            if (inits.size() > 0) {
                append(" = { ");
                writeJNodesJoined(inits, ", ");
                append('}');
            }
            return;
        }

        // Append 'new' keyword, type and parameter list start char
        append("new ");
        writeJType(typ);
        append('(');

        // Append args
        List<JExpr> args = aExpr.getArgs();
        JExpr last = args.size() > 0 ? args.get(args.size() - 1) : null;
        for (JExpr arg : aExpr.getArgs()) {
            writeJExpr(arg);
            if (arg != last) append(", ");
        }

        // Append close char
        append(')');

        // Append ClassDecl
        JClassDecl cdecl = aExpr.getClassDecl();
        if (cdecl != null)
            writeJClassDecl(cdecl);
    }

    /**
     * Writes a JExprArrayIndex.
     */
    public void writeJExprArrayIndex(JExprArrayIndex aExpr)
    {
        JExpr expr = aExpr.getArrayExpr();
        writeJExpr(expr);
        JExpr iexpr = aExpr.getIndexExpr();
        append('[');
        writeJExpr(iexpr);
        append(']');
    }

    /**
     * Writes a JExpr.CastExpr.
     */
    public void writeJExprCast(JExpr.CastExpr aExpr)
    {
        append('(');
        writeJType(aExpr.getType());
        append(')');
        writeJExpr(aExpr.getExpr());
    }

    /**
     * Writes a JExprChain.
     */
    public void writeJExprChain(JExprChain aExpr)
    {
        // Write component expressions
        List<JExpr> exprs = aExpr.getExpressions();
        JExpr last = exprs.get(exprs.size() - 1);
        for (JExpr exp : exprs) {
            writeJExpr(exp);
            if (exp != last) append('.');
        }
    }

    /**
     * Writes a JExprId.
     */
    public void writeJExprId(JExprId aExpr)
    {
        append(aExpr.getName());
    }

    /**
     * Writes a JExpr.InstanceOfExpr.
     */
    public void writeJExprInstanceOf(JExpr.InstanceOfExpr aExpr)
    {
        // Get the type and type string
        JExpr expr = aExpr.getExpr();
        JType typ = aExpr.getType();
        writeJExpr(expr);
        append(" instanceof ");
        writeJType(typ);
    }

    /**
     * Writes a JExprLambda.
     */
    public void writeJExprLambda(JExprLambda aExpr)
    {
        // Write parameters
        append('(');
        List<JVarDecl> params = aExpr.getParams();
        JVarDecl last = params.size() > 0 ? params.get(params.size() - 1) : null;
        for (JVarDecl param : aExpr.getParams()) {
            append(param.getName());
            if (param != last) append(',');
        }
        append(')');

        // Write arrow
        append(" -> ");

        // Write expression or statement block
        if (aExpr.getExpr() != null)
            writeJExpr(aExpr.getExpr());
        else writeJStmtBlock(aExpr.getBlock(), false);
    }

    /**
     * Writes a JExprLiteral.
     */
    public void writeJExprLiteral(JExprLiteral aExpr)
    {
        String str = aExpr.getValueString();
        append(str);
    }

    /**
     * Writes a JExprMath.
     */
    public void writeJExprMath(JExprMath aExpr)
    {
        JExprMath.Op op = aExpr.getOp();

        // Handle basic binary operations
        if (aExpr.getOperandCount() == 2) {
            JExpr exp0 = aExpr.getOperand(0), exp1 = aExpr.getOperand(1);
            writeJExpr(exp0);
            append(' ').append(JExprMath.getOpString(op)).append(' ');
            writeJExpr(exp1);
        }

        // Handle conditional
        else if (op == JExprMath.Op.Conditional) {
            JExpr exp0 = aExpr.getOperand(0), exp1 = aExpr.getOperand(1), exp2 = aExpr.getOperand(2);
            writeJExpr(exp0);
            append("? ");
            writeJExpr(exp1);
            append(" : ");
            writeJExpr(exp2);
        }

        // Handle unary pre
        else if (op == JExprMath.Op.Negate || op == JExprMath.Op.Not ||
                op == JExprMath.Op.PreDecrement || op == JExprMath.Op.PreIncrement) {
            JExpr exp0 = aExpr.getOperand(0);
            append(JExprMath.getOpString(op));
            writeJExpr(exp0);
        }

        // Handle unary post
        else if (op == JExprMath.Op.PostDecrement || op == JExprMath.Op.PostIncrement) {
            JExpr exp0 = aExpr.getOperand(0);
            append(JExprMath.getOpString(op));
            writeJExpr(exp0);
        } else throw new RuntimeException("JavaWriter.writeJExprMath: Unsupported op: " + op);
    }

    /**
     * Writes a JExprMethodCall.
     */
    public void writeJExprMethodCall(JExprMethodCall aExpr)
    {
        // Append name and open char
        JExprId id = aExpr.getId();
        String name = id.getName();
        writeJExprId(id);
        append('(');

        // Append args
        List<JExpr> args = aExpr.getArgs();
        JExpr last = args.size() > 0 ? args.get(args.size() - 1) : null;
        for (JExpr arg : aExpr.getArgs()) {
            writeJExpr(arg);
            if (arg != last) append(", ");
        }

        // Append close char
        append(')');
    }

    /**
     * Writes a JExprMethodRef.
     */
    public void writeJExprMethodRef(JExprMethodRef aExpr)
    {
        System.out.println("JavaWriter: writeJExprMethodRef not implemented");
    }

    /**
     * Writes a JExprType.
     */
    public void writeJExprType(JExprType aExpr)
    {
        JType typ = aExpr.getType();
        writeJType(typ);
    }

    /**
     * Append String.
     */
    public JavaWriter append(String aStr)
    {
        checkIndent();
        _sb.append(aStr);
        return this;
    }

    /**
     * Append char.
     */
    public JavaWriter append(char aValue)
    {
        checkIndent();
        _sb.append(aValue);
        return this;
    }

    /**
     * Append Int.
     */
    public JavaWriter append(int aValue)
    {
        checkIndent();
        _sb.append(aValue);
        return this;
    }

    /**
     * Append Double.
     */
    public JavaWriter append(double aValue)
    {
        checkIndent();
        _sb.append(aValue);
        return this;
    }

    /**
     * Append newline.
     */
    public JavaWriter endln()
    {
        _sb.append('\n');
        _lineStart = true;
        return this;
    }

    /**
     * Append indent.
     */
    public JavaWriter indent()
    {
        _indent++;
        return this;
    }

    /**
     * Append indent.
     */
    public JavaWriter outdent()
    {
        _indent--;
        return this;
    }

    /**
     * Append indent.
     */
    public JavaWriter appendIndent()
    {
        for (int i = 0; i < _indent; i++) _sb.append(_indentStr);
        return this;
    }

    /**
     * Checks for indent.
     */
    protected void checkIndent()
    {
        if (_lineStart)
            appendIndent();
        _lineStart = false;
    }

}