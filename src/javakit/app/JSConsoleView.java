package javakit.app;
import snap.gfx.Color;
import snap.text.TextLink;
import snap.text.TextStyle;
import snap.util.SnapUtils;
import snap.viewx.ConsoleView;

/**
 * A TextView subclass to open links.
 */
public class JSConsoleView extends ConsoleView {

    // The JSConsole
    protected JSConsole _owner;

    // The error color
    private static Color ERROR_COLOR = new Color("CC0000");

    /**
     * Constructor.
     */
    public JSConsoleView()
    {
        super();
    }

    /** Override to open in browser. */
    //protected void openLink(String aLink)  { _rpanel._appPane.getBrowser().setURLString(aLink); }

    /**
     * Override to send to process.
     */
    protected void processEnterAction()
    {
        //RunApp proc = _pgc.getProcPane().getSelApp(); if(proc==null) return;
        //String str = getInput();
        //proc.sendInput(str);
    }

    /**
     * Appends to out.
     */
    public void appendOut(String aStr)
    {
        // Make sure we're in app event thread
        if (!getEnv().isEventThread()) {
            getEnv().runLater(() -> appendOut(aStr)); return; }

        // Append text in black
        appendString(aStr, Color.BLACK);
    }

    /**
     * Appends to err.
     */
    public void appendErr(String aStr)
    {
        // Make sure we're in app event thread
        if (!getEnv().isEventThread()) {
            getEnv().runLater(() -> appendErr(aStr)); return; }

        // Append text in red
        appendString(aStr, ERROR_COLOR);
    }

    /**
     * Appends text with given color.
     */
    public void appendString(String aStr, Color aColor)
    {
        // If things are out of hand, just return
        if (length() > 100000) return;

        // Get default style modified for color
        TextStyle style = getStyleForCharIndex(length());
        style = style.copyFor(aColor);

        // Look for a StackFrame reference: " at java.pkg.Class(Class.java:55)" and add as link if found
        int start = 0;
        for (int i = aStr.indexOf(".java:"); i > 0; i = aStr.indexOf(".java:", start)) {
            int s = aStr.lastIndexOf("(", i), e = aStr.indexOf(")", i);
            if (s < 0 || e < 0) {
                addChars(aStr.substring(start, i + 6), style);
                start = i + 6;
                continue;
            }
            String prefix = aStr.substring(start, s + 1);
            String linkedText = aStr.substring(s + 1, e);
            TextStyle lstyle = style.copyFor(new TextLink(getLink(prefix, linkedText)));
            addChars(prefix, style);
            addChars(linkedText, lstyle);
            start = e;
        }

        // Add remainder normally
        addChars(aStr.substring(start), style);
    }

    /**
     * Returns a link for a StackString.
     */
    String getLink(String aPrefix, String linkedText)
    {
        // Get start/end of full class path for .java
        int start = aPrefix.indexOf("at ");
        if (start < 0) return "/Unknown";
        start += 3;
        int end = aPrefix.indexOf('$');
        if (end < start)
            end = aPrefix.lastIndexOf('.');
        if (end < start)
            end = aPrefix.length() - 1;

        // Create link from path and return
        String path = aPrefix.substring(start, end);
        path = '/' + path.replace('.', '/') + ".java";
        path = getSourceURL(path);
        String lineStr = linkedText.substring(linkedText.indexOf(":") + 1);
        int line = SnapUtils.intValue(lineStr);
        if (line > 0) path += "#LineNumber=" + line;
        return path;
    }

    /**
     * Returns a source URL for path.
     */
    String getSourceURL(String aPath)
    {
        //if(aPath.startsWith("/java/") || aPath.startsWith("/javax/"))
        //    return "http://reportmill.com/jars/8u05/src.zip!" + aPath;
        //if(aPath.startsWith("/javafx/"))
        //    return "http://reportmill.com/jars/8u05/javafx-src.zip!" + aPath;
        //Project proj = Project.get(_appPane.getRootSite()); if(proj==null) return aPath;
        //WebFile file = proj.getProjectSet().getSourceFile(aPath);
        //return file!=null? file.getURL().getString() : aPath;
        return null;
    }
}
