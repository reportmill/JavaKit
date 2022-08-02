/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextLink;
import snap.text.TextStyle;
import snap.util.SnapUtils;
import snap.view.ViewEvent;
import snap.view.ViewOwner;
import snap.viewx.ConsoleView;

/**
 * A panel to process output.
 */
public class JSConsole extends ViewOwner {

    // The Playground
    JavaShell _pg;

    // The output text
    JSConsoleView _tview;

    // The error color
    static Color ERROR_COLOR = new Color("CC0000");

    /**
     * Creates a new PGConsole.
     */
    public JSConsole(JavaShell aPG)
    {
        _pg = aPG;
    }

    /**
     * Returns the Playground.
     */
    public JavaShell getPlayground()
    {
        return _pg;
    }

    /**
     * Returns the console view.
     */
    public ConsoleView getConsoleView()
    {
        return _tview;
    }

    /**
     * Clears the RunConsole text.
     */
    public void clear()
    {
        if (_tview != null) _tview.clear();
    }

    /**
     * Appends to out.
     */
    public void appendOut(String aStr)
    {
        if (!isEventThread()) {
            runLater(() -> appendOut(aStr));
            return;
        }  // Make sure we're in app event thread
        appendString(aStr, Color.BLACK);                               // Append string in black
    }

    /**
     * Appends to err.
     */
    public void appendErr(String aStr)
    {
        if (!isEventThread()) {
            runLater(() -> appendErr(aStr));
            return;
        }  // Make sure we're in app event thread
        appendString(aStr, ERROR_COLOR);                                   // Append string in red
    }

    /**
     * Appends text with given color.
     */
    void appendString(String aStr, Color aColor)
    {
        // Get default style modified for color
        TextStyle style = _tview.getStyleAt(_tview.length());
        if (_tview.length() > 100000) return;
        style = style.copyFor(aColor);

        // Look for a StackFrame reference: " at java.pkg.Class(Class.java:55)" and add as link if found
        int start = 0;
        for (int i = aStr.indexOf(".java:"); i > 0; i = aStr.indexOf(".java:", start)) {
            int s = aStr.lastIndexOf("(", i), e = aStr.indexOf(")", i);
            if (s < 0 || e < 0) {
                _tview.addChars(aStr.substring(start, i + 6), style);
                start = i + 6;
                continue;
            }
            String prefix = aStr.substring(start, s + 1);
            String linkedText = aStr.substring(s + 1, e);
            TextStyle lstyle = style.copyFor(new TextLink(getLink(prefix, linkedText)));
            _tview.addChars(prefix, style);
            _tview.addChars(linkedText, lstyle);
            start = e;
        }

        // Add remainder normally
        _tview.addChars(aStr.substring(start), style);
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
        if (end < start) end = aPrefix.lastIndexOf('.');
        if (end < start) end = aPrefix.length() - 1;

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

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Get font
        String[] names = {"Monoco", "Consolas", "Courier"};
        Font defaultFont = null;
        for (int i = 0; i < names.length; i++) {
            defaultFont = new Font(names[i], 12);
            if (defaultFont.getFamily().startsWith(names[i]))
                break;
        }

        // Get output text
        _tview = getView("OutputText", JSConsoleView.class);
        _tview._pgc = this;
        _tview.setFont(defaultFont);
        _tview.setPlainText(false);
        _tview.setPadding(4, 4, 4, 4);
    }

    @Override
    protected void resetUI()
    {
    }

    /**
     * Respond to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle ClearButton
        if (anEvent.equals("ClearButton"))
            clear(); //getProcPane().getSelApp().clearOutput();

        // Handle TerminateButton
        //if(anEvent.equals("TerminateButton")) getProcPane().getSelApp().terminate();
    }

    /**
     * A TextView subclass to open links.
     */
    public static class JSConsoleView extends ConsoleView {

        JSConsole _pgc;

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
    }

}