package javakit.parse;
import snap.parse.ParseNode;
import snap.parse.ParseRule;
import snap.parse.ParseToken;

/**
 * This class.
 */
public class JeplParser extends JavaParser {

    /**
     * Constructor.
     */
    public JeplParser()
    {
        super();

        // Get/set rule for JeplFile
        ParseRule jeplRule = getRule("JeplFile");
        jeplRule.setHandler(new JeplFileHandler());
        setRule(jeplRule);
    }

    /**
     * JeplFile Handler.
     */
    public static class JeplFileHandler extends JNodeParseHandler<JFile> {

        // A running ivar for batches of statements
        JInitializerDecl  _initDecl;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get JFile and update end token
            JFile jfile = getPart();
            ParseToken endToken = aNode.getEndToken();
            jfile.setEndToken(endToken);

            // Get ClassDecl - create/add if not yet set
            JClassDecl classDecl = jfile.getClassDecl();

            // Handle BlockStatement
            if (anId == "BlockStatement") {

                // If no current InitDecl, create (with statement block) and add
                if (_initDecl == null) {
                    _initDecl = new JInitializerDecl();
                    _initDecl.setStartToken(aNode.getStartToken());
                    JStmtBlock stmtBlock = new JStmtBlock();
                    stmtBlock.setStartToken(aNode.getStartToken());
                    _initDecl.setBlock(stmtBlock);
                    classDecl.addMemberDecl(_initDecl);
                }

                // Add block statement to current InitDecl.Block
                JStmtBlock initDeclBlock = _initDecl.getBlock();
                JStmt blockStmt = aNode.getCustomNode(JStmt.class);
                initDeclBlock.addStatement(blockStmt);

                // Update end tokens
                _initDecl.setEndToken(endToken);
                classDecl.setEndToken(endToken);
            }

            // Handle Modifiers
            else if (anId == "Modifiers") {
                // Ignore for now
            }

            // Handle MethodDecl
            else if (anId == "MethodDecl") {
                JMethodDecl methodDecl = aNode.getCustomNode(JMethodDecl.class);
                classDecl.addMemberDecl(methodDecl);
                jfile.setEndToken(endToken);
                _initDecl = null;
            }
        }

        /**
         * Override to create JFile with implied ClassDecl and ImportDecls.
         */
        @Override
        protected JFile createPart()
        {
            // Do normal version
            JFile jfile = super.createPart();
            ParseToken startToken = getStartToken();

            // Create/add JImportDecls
            String[] importNames = new JavaTextDocBuilder().getImports();
            for (String importName : importNames)
                addImport(jfile, importName);
            addImport(jfile,"snapcharts.data.*");
            addImport(jfile,"snapjava.app.*");

            // Create/add ClassDecl
            JClassDecl classDecl = new JClassDecl();
            classDecl.setName("JavaShellREPL");
            classDecl.setStartToken(startToken);
            jfile.addClassDecl(classDecl);

            // Add Superclass
            JType extendsType = new JType();
            extendsType.setName("snapjava.app.ChartsREPL");
            extendsType.setStartToken(startToken);
            extendsType.setEndToken(startToken);
            classDecl.addExtendsType(extendsType);

            _initDecl = null;

            // Return
            return jfile;
        }

        /**
         * Creates and adds JImportDecl to JFile for given import path.
         */
        private void addImport(JFile aFile, String anImportPathName)
        {
            // Get inclusive and path info
            boolean isInclusive = anImportPathName.endsWith(".*");
            String importPathName = isInclusive ? anImportPathName.substring(anImportPathName.length() - 2) : anImportPathName;

            // Create/configure/add ImportDecl
            JImportDecl importDecl = new JImportDecl();
            importDecl.setName(importPathName);
            importDecl.setInclusive(isInclusive);
            importDecl.setStartToken(aFile.getStartToken());
            importDecl.setEndToken(aFile.getStartToken());
            aFile.addImportDecl(importDecl);
        }

        protected Class<JFile> getPartClass()  { return JFile.class; }
    }

}
