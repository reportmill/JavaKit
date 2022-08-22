package javakit.shell;
import javakit.parse.JFile;
import javakit.parse.JavaParser;
import snap.util.ArrayUtils;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaText {

    // The array of imports
    private String[]  _imports = DEFAULT_IMPORTS;

    // The super class name
    private String  _superClassName = "Object";

    // The body text
    private String  _bodyText;

    // Constants for imports
    private static final String IMPORT1 = "java.util.*";
    private static final String IMPORT2 = "java.util.stream.*";
    private static final String IMPORT3 = "snap.view.*";
    private static final String[] DEFAULT_IMPORTS = { IMPORT1, IMPORT2, IMPORT3 };

    /**
     * Constructor.
     */
    public JavaText()
    {
        super();
    }

    /**
     * Returns the text of the file.
     */
    public String getText()
    {
        String headerText = getHeaderText();
        String bodyText = getBodyText();
        String tailText = "\n}\n}";
        return headerText + bodyText + tailText;
    }

    /**
     * Adds an import.
     */
    public void addImport(String anImportStr)
    {
        _imports = ArrayUtils.add(_imports, anImportStr);
    }

    /**
     * Sets the base class name.
     */
    public void setSuperClassName(String aName)
    {
        _superClassName = aName;
    }

    /**
     * Returns the Java file header text.
     */
    public String getHeaderText()
    {
        // Get import declarations string
        String importsDecl = "";
        for (String importDecl : _imports)
            importsDecl += "import " + importDecl + ";\n";
        importsDecl += '\n';

        // Construct class/method wrapper for statements
        String classDecl = "public class JavaShellREPL extends " + _superClassName + " {\n\n";
        String methodDecl = "void body() {\n\n";
        return importsDecl + classDecl + methodDecl;
    }

    /**
     * Returns the body.
     */
    public String getBodyText()  { return _bodyText; }

    /**
     * Sets the body.
     */
    public void setBodyText(String bodyText)
    {
        _bodyText = bodyText;
    }

    /**
     * Returns the body lines.
     */
    public String[] getBodyLines()
    {
        String[] lines = _bodyText.split("\n");
        return lines;
    }

    /**
     * Returns a JFile of the JavaText without body text.
     */
    public JFile getEmptyJFile()
    {
        // Construct class/method wrapper for statements
        String javaHeader = getHeaderText();
        String javaTextStr = javaHeader + "\n}\n}";

        // Parse JavaText to JFile
        JavaParser javaParser = JavaParser.getShared();
        javaParser.setInput(javaTextStr);
        JFile jfile = javaParser.parseCustom(JFile.class);
        //jfile.setResolver(_resolver);

        // Return
        return jfile;
    }

}
