/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import javakit.resolver.Resolver;
import snap.geom.HPos;
import snap.props.PropObject;
import snap.text.TextDoc;
import snap.util.SnapUtils;
import snap.view.*;

/**
 * A pane to do Java REPL (Read, Eval, Print, Loop).
 */
public class JavaShellPane extends ViewOwner {

    // A JavaShell
    protected JavaShell  _javaShell;

    // The JeplDoc
    protected JeplDoc  _jeplDoc;

    // The Console
    protected JSConsole  _console;

    // The TextPane
    private JSTextPane  _textPane = new JSTextPane(this);

    // The TabPane
    private JSTabPane  _tabPane = new JSTabPane(this);

    /**
     * Constructor.
     */
    public JavaShellPane()
    {
        // Create JavaText
        _jeplDoc = createJeplDoc();
        _javaShell = _jeplDoc.getJavaShell();

        // Create console
        _console = new JSConsole(this);
        _javaShell.setConsole(_console);
    }

    /**
     * Returns the JavaShell.
     */
    public JavaShell getJavaShell()  { return _javaShell; }

    /**
     * Returns the console.
     */
    public JSConsole getConsole()  { return _console; }

    /**
     * Runs the JavaShell.
     */
    public void play()
    {
        _jeplDoc.updateDocValues();

        // Update lines
        _textPane._evalView.updateLines();

        // Update graphics
        _console.updateShelf();
    }

    /**
     * Creates a JavaTextDoc for given Resolver.
     */
    private JeplDoc createJeplDoc()
    {
        // Create/config/set Doc
        JeplDoc jeplDoc = new JeplDoc();
        jeplDoc.setName("Untitled");

        JavaTextDoc javaTextDoc = createJavaTextDoc();
        jeplDoc.setJavaDoc(javaTextDoc);

        // Return
        return jeplDoc;
    }

    /**
     * Creates a JavaTextDoc for given Resolver.
     */
    private JavaTextDoc createJavaTextDoc()
    {
        // Get template Java text string
        JavaTextDocBuilder javaTextDocBuilder = new JavaTextDocBuilder();

        // Create JavaDoc to hold Java code
        JavaTextDoc javaTextDoc = javaTextDocBuilder.createJavaTextDoc();

        // Create/set resolver
        Resolver resolver = createResolver();
        javaTextDoc.setResolver(resolver);

        // Return
        return javaTextDoc;
    }

    /**
     * Creates the Resolver.
     */
    private Resolver createResolver()
    {
        // Create resolver
        Resolver resolver = Resolver.newResolverForClassLoader(JeplDoc.class.getClassLoader());

        // Add class paths for SnapKit
        if (!SnapUtils.isTeaVM)
            resolver.addClassPathForClass(PropObject.class);

        // Return
        return resolver;
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

        // Set TextArea to JeplDoc
        TextDoc jeplTextDoc = _jeplDoc.getReplDoc();
        TextArea textArea = _textPane.getTextArea();
        textArea.setTextDoc(jeplTextDoc);

        // Play
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
        JavaShellPane javaShellPane = new JavaShellPane();
        javaShellPane.getWindow().setTitle("JavaShell");
        javaShellPane.getWindow().setMaximized(true);
        javaShellPane.setWindowVisible(true);
    }
}