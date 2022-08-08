/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import snap.geom.HPos;
import snap.view.*;

/**
 * A pane to do Java REPL (Read, Eval, Print, Loop).
 */
public class JavaShell extends ViewOwner {

    // The TextPane
    JSTextPane _textPane = new JSTextPane(this);

    // The TabPane
    JSTabPane _tabPane = new JSTabPane(this);

    // The Console
    JSConsole _console = new JSConsole(this);

    // The evaluator
    JSEvaluator _evaluator = new JSEvaluator(this);

    /**
     * Creates a new Playground.
     */
    public JavaShell()
    {
    }

    /**
     * Returns the console.
     */
    public JSConsole getConsole()
    {
        return _console;
    }

    /**
     * Runs the JavaShell.
     */
    public void play()
    {
        // Get Java text and eval
        String javaText = _textPane.getTextArea().getText();
        _evaluator.eval(javaText);

        // Update lines
        _textPane._evalView.updateLines();

        // Update graphics
        _console.updateShelf();
    }

    /**
     * Creates the UI.
     */
    protected View createUI()
    {
        // Create/config SplitView
        SplitView split = new SplitView();
        split.setPrefSize(1000, 900);
        split.setVertical(true);
        split.setItems(_textPane.getUI(), _console.getUI()); //(vbox,_tabPane.getUI());

        // Configure Console
        _console.getUI().setPrefHeight(240);

        // Return
        return split;
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        super.initUI();

        // Add Button to TextPane
        Button runButton = new Button("Run");
        runButton.setName("RunButton");
        runButton.setPrefSize(100, 20);
        runButton.setLeanX(HPos.RIGHT);
        runButton.setOwner(this);
        _textPane.getToolBarPane().addChild(runButton);
        _textPane.getToolBarPane().setPadding(0, 30, 0, 4);
        runLater(() -> play());
    }

    /**
     * Respond to UI.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        if (anEvent.equals("RunButton"))
            play();
    }

    /**
     * Standard main method.
     */
    public static void main(String[] args)
    {
        JavaShell javaShell = new JavaShell();
        javaShell.getWindow().setTitle("JavaShell");
        javaShell.getWindow().setMaximized(true);
        javaShell.setWindowVisible(true);
    }

}