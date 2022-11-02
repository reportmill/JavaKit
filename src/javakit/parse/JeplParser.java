/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.*;

/**
 * This class.
 */
public class JeplParser extends JavaParser {

    /**
     * Constructor.
     */
    public JeplParser(JeplTextDoc aJeplTextDoc)
    {
        super();

        // Get/set rule for JeplFile
        ParseRule jeplRule = getRule("JeplFile");
        jeplRule.setHandler(new JeplFileHandler(aJeplTextDoc));
        setRule(jeplRule);
    }

    /**
     * JeplFile Handler.
     */
    public static class JeplFileHandler extends JNodeParseHandler<JFile> {

        // The JeplTextDoc that created this object
        private JeplTextDoc  _jeplTextDoc;

        // A running ivar for batches of statements
        JInitializerDecl  _initDecl;

        /**
         * Constructor.
         */
        public JeplFileHandler(JeplTextDoc aJeplTextDoc)
        {
            super();
            _jeplTextDoc = aJeplTextDoc;
        }

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

                    // Create InitDecl and add to class
                    _initDecl = new JInitializerDecl();
                    _initDecl.setStartToken(aNode.getStartToken());
                    classDecl.addMemberDecl(_initDecl);

                    // Create block statement and add to InitDecl
                    JStmtBlock blockStmt = new JStmtBlock();
                    blockStmt.setStartToken(aNode.getStartToken());
                    _initDecl.setBlock(blockStmt);
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
            ParseToken startToken = PHANTOM_TOKEN; //getStartToken();
            jfile.setStartToken(startToken);

            // Create/add JImportDecls
            JavaTextDocBuilder javaTextDocBuilder = _jeplTextDoc.getJavaTextDocBuilder();
            String[] importNames = javaTextDocBuilder.getImports();
            for (String importName : importNames)
                addImport(jfile, importName);

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
            String importPathName = isInclusive ? anImportPathName.substring(0, anImportPathName.length() - 2) : anImportPathName;

            // Create/configure/add ImportDecl
            JImportDecl importDecl = new JImportDecl();
            importDecl.setName(importPathName);
            importDecl.setInclusive(isInclusive);
            importDecl.setStartToken(PHANTOM_TOKEN);
            importDecl.setEndToken(PHANTOM_TOKEN);
            aFile.addImportDecl(importDecl);
        }

        protected Class<JFile> getPartClass()  { return JFile.class; }

        /**
         * This should never get called.
         */
        @Override
        protected ParseHandler createBackupHandler()
        {
            System.err.println("JeplParser.createBackupHandler: This should never get called");
            return new JeplFileHandler(_jeplTextDoc);
        }
    }

    // A special zero length token for programmatically created nodes at file start
    private static ParseToken PHANTOM_TOKEN = new PhantomToken();

    /**
     * A bogus zero length token for phantom programmatically created nodes at file start.
     */
    private static class PhantomToken implements ParseToken {
        public PhantomToken()  { super(); }
        public String getName()  { return "InputStart"; }
        public String getPattern()  { return""; }
        public int getStartCharIndex() { return 0; }
        public int getEndCharIndex()  { return 0; }
        public int getLineIndex()  { return 0; }
        public int getStartCharIndexInLine()  { return 0; }
        public String getString()  { return ""; }
    }
}
