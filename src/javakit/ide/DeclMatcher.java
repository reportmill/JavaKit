/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.ide;
import javakit.parse.*;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaField;
import javakit.resolver.JavaMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles matching typed chars to JavaDecls.
 */
public class DeclMatcher {

    // The prefix
    private String  _prefix;

    // The Matcher
    private Matcher  _matcher;

    /**
     * Constructor.
     */
    public DeclMatcher(String aPrefix)
    {
        _prefix = aPrefix;
        _matcher = getSkipCharsMatcherForLiteralString(_prefix);
    }

    /**
     * Returns the prefix string.
     */
    public String getPrefix()  { return _prefix; }

    /**
     * Returns whether this matcher matches given string.
     */
    public boolean matchesString(String aString)
    {
        return _matcher.reset(aString).lookingAt();
    }

    /**
     * Returns a regex Matcher for given literal string that allows for skipping any chars between given string chars.
     * For instance, "al" will match ArrayList with this matcher (pattern is created with CASE_INSENSITIVE).
     * Use matcher.reset(str).lookingAt() to check prefix (like string.startWith()).
     */
    private static Matcher getSkipCharsMatcherForLiteralString(String aStr)
    {
        String regexStr = getSkipCharsRegexForLiteralString(aStr);
        int flags = Character.isUpperCase(aStr.charAt(0)) ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(regexStr, flags);
        Matcher matcher = pattern.matcher("");
        return matcher;
    }

    /**
     * Returns a regex string for given literal string that allows for skipping any chars between given string chars.
     * For instance, "al" will match ArrayList.
     */
    private static String getSkipCharsRegexForLiteralString(String aStr)
    {
        // Generate prefix regex, e.g.: 'abc' turns to 'a[^b]*b[^c]c'
        StringBuffer regexSB = new StringBuffer();
        for (int i = 0; i < aStr.length(); i++) {
            char prefixChar = aStr.charAt(i);
            if (i == 0)
                regexSB.append(prefixChar);
            else regexSB.append("[^").append(prefixChar).append("]*").append(prefixChar);
        }

        // Return string
        return regexSB.toString();
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public List<JavaField> getFieldsForClass(JavaClass aClass)
    {
        // Create return list of prefix fields
        List<JavaField> fieldsWithPrefix = new ArrayList<>();

        // Iterate over classes
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {

            // Get Class fields
            List<JavaField> fields = cls.getFields();
            for (JavaField field : fields)
                if (matchesString(field.getName()))
                    fieldsWithPrefix.add(field);

            // Should iterate over class interfaces, too
        }

        // Return list of prefix fields
        return fieldsWithPrefix;
    }

    /**
     * Returns methods that match given matcher.
     */
    public List<JavaMethod> getMethodsForClass(JavaClass aClass)
    {
        // Create return list of prefix methods
        List<JavaMethod> methodsWithPrefix = new ArrayList<>();

        // Iterate over classes
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {

            // Get Class methods
            List<JavaMethod> methods = cls.getMethods();
            for (JavaMethod method : methods)
                if (matchesString(method.getName()))
                    methodsWithPrefix.add(method);

            // If interface, iterate over class interfaces, too (should probably do this anyway to catch default methods).
            if (cls.isInterface()) {
                JavaClass[] interfaces = cls.getInterfaces();
                for (JavaClass interf : interfaces) {
                    List<JavaMethod> moreMethods = getMethodsForClass(interf);
                    methodsWithPrefix.addAll(moreMethods);
                }
            }
        }

        // Return list of prefix methods
        return methodsWithPrefix;
    }

    /**
     * Finds JVarDecls for given prefix matcher and adds them to given list.
     */
    public List<JVarDecl> getVarDeclsForJNode(JNode aNode, List<JVarDecl> theVariables)
    {
        // Handle JClassDecl
        if (aNode instanceof JClassDecl)
            getVarDeclsForJClassDecl((JClassDecl) aNode, theVariables);

        // Handle JExecutableDecl
        else if (aNode instanceof JExecutableDecl)
            getVarDeclsForJExecutableDecl((JExecutableDecl) aNode, theVariables);

        // Handle JInitializerDecl
        else if (aNode instanceof JInitializerDecl)
            getVarDeclsForJInitializerDecl((JInitializerDecl) aNode, theVariables);

        // Handle JInitializerDecl
        else if (aNode instanceof JStmtBlock)
            getVarDeclsForJStmtBlock((JStmtBlock) aNode, theVariables);

        // If Parent, forward on
        JNode parent = aNode.getParent();
        if (parent != null)
            getVarDeclsForJNode(parent, theVariables);

        // Return
        return theVariables;
    }

    /**
     * Get VarDecls for JClassDecl - search Class fields.
     */
    private void getVarDeclsForJClassDecl(JClassDecl classDecl, List<JVarDecl> varDeclList)
    {
        // Iterate over FieldDecls and see if any contains matching varDecls
        JFieldDecl[] fieldDecls = classDecl.getFieldDecls();
        for (JFieldDecl fieldDecl : fieldDecls) {
            List<JVarDecl> varDecls = fieldDecl.getVarDecls();
            getVarDeclsForJVarDecls(varDecls, varDeclList);
        }
    }

    /**
     * Get VarDecls for JExecutableDecl - search method/constructor params.
     */
    private void getVarDeclsForJExecutableDecl(JExecutableDecl executableDecl, List<JVarDecl> varDeclList)
    {
        // Get Executable.Parameters and search
        List<JVarDecl> params = executableDecl.getParameters();
        getVarDeclsForJVarDecls(params, varDeclList);
    }

    /**
     * Get VarDecls for JInitializerDecl - REPL hack to check prior JInitDecls for VarDecl matching node name.
     */
    private void getVarDeclsForJInitializerDecl(JInitializerDecl anInitDecl, List<JVarDecl> varDeclList)
    {
        // Get enclosing class initDecls
        JClassDecl classDecl = anInitDecl.getEnclosingClassDecl();
        JInitializerDecl[] initDecls = classDecl.getInitDecls();

        // Iterate over initDecls
        for (JInitializerDecl initDecl : initDecls) {

            // Stop when we hit given InitDecl
            if (initDecl == anInitDecl)
                break;

            // Get InitDecl.Block and search
            JStmtBlock initDeclBlock = initDecl.getBlock();
            getVarDeclsForJStmtBlock(initDeclBlock, varDeclList);
        }
    }

    /**
     * Get VarDecls for JStmtBlock.
     */
    private void getVarDeclsForJStmtBlock(JStmtBlock blockStmt, List<JVarDecl> varDeclList)
    {
        // Get statements and search
        List<JStmt> statements = blockStmt.getStatements();

        // Iterate over statements and see if any JStmtVarDecl contains variable with that name
        for (JStmt stmt : statements) {

            // Skip non-VarDecl statements
            if (!(stmt instanceof JStmtVarDecl))
                continue;

            // Get varDeclStmt.VarDecls
            JStmtVarDecl varDeclStmt = (JStmtVarDecl) stmt;
            List<JVarDecl> varDecls = varDeclStmt.getVarDecls();
            getVarDeclsForJVarDecls(varDecls, varDeclList);
        }
    }

    /**
     * Get VarDecls for JVarDecl list.
     */
    private void getVarDeclsForJVarDecls(List<JVarDecl> varDecls, List<JVarDecl> varDeclList)
    {
        for (JVarDecl varDecl : varDecls)
            if (matchesString(varDecl.getName()))
                varDeclList.add(varDecl);
    }
}
