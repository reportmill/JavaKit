/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import snap.gfx.Font;
import snap.view.View;
import snap.view.ViewEvent;
import snap.view.ViewOwner;
import snap.viewx.ConsoleView;

/**
 * A panel to process output.
 */
public class JSConsole extends ViewOwner implements JavaShell.Console {

    // The JavaShell
    private JavaShellPane  _javaShellPane;

    // The output graphics
    private JSConsoleShelf  _shelfView;

    // The output graphics
    private View  _shelfViewMain;

    // The output text
    private JSConsoleView  _consoleView;

    /**
     * Creates a new PGConsole.
     */
    public JSConsole(JavaShellPane aJSP)
    {
        _javaShellPane = aJSP;
    }

    /**
     * Returns the console view.
     */
    public ConsoleView getConsoleView()
    {
        return _consoleView;
    }

    /**
     * Clears the RunConsole text.
     */
    public void clear()
    {
        if (_consoleView != null) _consoleView.clear();
    }

    /**
     * Appends to out.
     */
    public void appendOut(String aStr)
    {
        _consoleView.appendOut(aStr);
    }

    /**
     * Appends to err.
     */
    public void appendErr(String aStr)
    {
        _consoleView.appendErr(aStr);
    }

    /**
     * Returns the text length at any given point.
     */
    public int getTextLength()
    {
        return _consoleView.length();
    }

    /**
     * Returns a substring of appended text.
     */
    public String getTextSubstring(int aStart, int anEnd)
    {
        return _consoleView.getText().substring(aStart, anEnd);
    }

    /**
     * Appends a view.
     */
    public void updateShelf()
    {
        _shelfView.updateShelf(_javaShellPane);

        if (_shelfView.getChildCount() > 0)
            _shelfViewMain.setVisible(true);
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Get font
        String[] names = { "Monoco", "Consolas", "Courier" };
        Font defaultFont = null;
        for (int i = 0; i < names.length; i++) {
            defaultFont = new Font(names[i], 12);
            if (defaultFont.getFamily().startsWith(names[i]))
                break;
        }

        // Get ShelfViewMain
        _shelfViewMain = getView("ShelfViewMain");
        _shelfViewMain.setVisible(false);

        // Get ShelfView
        _shelfView = getView("ShelfView", JSConsoleShelf.class);

        // Get ConsoleView
        _consoleView = getView("OutputText", JSConsoleView.class);
        _consoleView._owner = this;
        _consoleView.setFont(defaultFont);
        _consoleView.setPadding(4, 4, 4, 4);
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
}