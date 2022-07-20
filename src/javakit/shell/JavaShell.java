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
     * Runs the playground.
     */
    public void play()
    {
        _evaluator.eval(_textPane.getTextArea().getText());
        _textPane._evalView.updateLines();
    }

    /**
     * Creates the UI.
     */
    protected View createUI()
    {
        //Button rbtn = new Button("Run"); rbtn.setName("RunButton"); rbtn.setPrefSize(60,20);
        //HBox hbox = new HBox(); hbox.setChildren(rbtn);
        //VBox vbox = new VBox(); vbox.setFillWidth(true); vbox.setChildren(_textPane.getUI(), hbox);
        SplitView split = new SplitView();
        split.setPrefSize(1000, 900);
        split.setVertical(true);
        split.setItems(_textPane.getUI(), _console.getUI()); //(vbox,_tabPane.getUI());
        _console.getUI().setPrefHeight(180);
        return split;
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        super.initUI();

        // Add Button to TextPane
        Button rbtn = new Button("Run");
        rbtn.setName("RunButton");
        rbtn.setPrefSize(100, 20);
        rbtn.setLeanX(HPos.RIGHT);
        rbtn.setOwner(this);
        _textPane.getToolBarPane().addChild(rbtn);
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
        JavaShell pg = new JavaShell();
        pg.getWindow().setTitle("Java Playground");
        pg.setWindowVisible(true);
    }

}