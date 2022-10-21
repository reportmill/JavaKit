/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.app;
import snap.view.*;

/**
 * A class to hold TabView for ProblemsPane, RunConsole, DebugPane.
 */
public class JSTabPane extends ViewOwner {

    // The JavaShell
    private JavaShellPane _javaShellPane;

    // The tabview
    private TabView  _tabView;

    // The list of tab owners
    private ViewOwner[] _tabOwners;

    // Constants for tabs
    public static final int CONSOLE_PANE = 0;
    public static final int RUN_PANE = 1;
    public static final int DEBUG_PANE_VARS = 2;
    public static final int BREAKPOINTS_PANE = 4;

    /**
     * Creates a new PGTabPane for given Playground.
     */
    public JSTabPane(JavaShellPane aPG)
    {
        _javaShellPane = aPG;
    }

    /**
     * Returns the selected index.
     */
    public int getSelIndex()
    {
        return _tabView != null ? _tabView.getSelIndex() : -1;
    }

    /**
     * Sets the selected index.
     */
    public void setSelIndex(int anIndex)
    {
        _tabView.setSelIndex(anIndex);
    }

    /**
     * Creates UI for SupportTray.
     */
    protected View createUI()
    {
        // Set TabOwners
        JSConsole console = _javaShellPane.getConsole();
        _tabOwners = new ViewOwner[]{console}; //, _appPane.getRunConsole(), _appPane.getDebugVarsPane(),
        //_appPane.getDebugExprsPane(), _appPane.getBreakpointsPanel(), _appPane.getSearchPane() };

        // Create TabView, configure and return
        _tabView = new TabView();
        _tabView.setName("TabView");
        _tabView.setFont(_tabView.getFont().deriveFont(12));
        _tabView.setTabMinWidth(70);
        //_tpane.addTab("Problems", _appPane.getProblemsPane().getUI());
        _tabView.addTab("Console", console.getUI());
        _tabView.addTab("Variables", new Label("DebugVarsPane"));
        return _tabView;
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        getUI().setPrefHeight(200);
    }

    /**
     * Override to reset selected tab.
     */
    protected void resetUI()
    {
        int index = _tabView.getSelIndex();
        ViewOwner sowner = _tabOwners[index];
        if (sowner != null)
            sowner.resetLater();
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle TabView
    /*if(_tpane.getTabContent(_tpane.getSelIndex()) instanceof Label) {
        int index = _tpane.getSelIndex();
        ViewOwner sowner = _tabOwners[index];
        _tpane.setTabContent(sowner.getUI(), index);
    }*/
    }

}