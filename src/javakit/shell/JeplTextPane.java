/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import javakit.text.JavaTextUtils;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.SubText;
import snap.text.TextStyle;
import snap.view.*;
import snap.viewx.TextPane;

/**
 * This TexPane subclass adds customizations for JavaShell.
 */
public class JeplTextPane extends TextPane {

    // The JeplDoc
    protected JeplDoc  _jeplDoc;

    // The TextArea
    private TextArea  _textArea;

    // LineNumView
    private LineNumView  _lineNumView;

    // EvalView
    protected JeplEvalView  _evalView;

    // For resetEntriesLater
    private Runnable  _resetReplValuesRun;

    // For resetEntriesLater
    private Runnable  _resetReplValuesRunReal = () -> { resetReplValues(); _resetReplValuesRun = null; };

    /**
     * Constructor.
     */
    public JeplTextPane()
    {
        super();
    }

    /**
     * Returns the JeplDoc.
     */
    public JeplDoc getJeplDoc()  { return _jeplDoc; }

    /**
     * Sets the JeplDoc.
     */
    public void setJeplDoc(JeplDoc aJeplDoc)
    {
        _jeplDoc = aJeplDoc;
    }

    /**
     * Returns the default font.
     */
    public Font getCodeFont()  { return JavaTextUtils.getCodeFont(); }

    /**
     * Reset Repl values.
     */
    public void resetReplValues()
    {
        JeplDoc jeplDoc = getJeplDoc();
        jeplDoc.updateDocValues();

        _evalView.updateLines();
    }

    /**
     * Reset Repl values.
     */
    public void resetReplValuesLater()
    {
        if (_resetReplValuesRun == null)
            runLater(_resetReplValuesRun = _resetReplValuesRunReal);
    }

    /**
     * Creates the TextArea.
     */
    protected TextArea createTextArea()
    {
        // Create/config
        JavaTextAreaX textArea = new JavaTextAreaX();
        textArea.setBorderRadius(4);
        textArea.setShowPrintMargin(false);
        textArea.setFocusPainted(true);
        //textArea.setTextDoc(_subText);
        return textArea;
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Basic config
        View ui = getUI();
        ui.setPrefSize(800, 700);
        ui.setGrowHeight(true);

        // Get/configure TextArea
        _textArea = getTextArea();
        _textArea.setGrowWidth(true);

        // Get/set/configure TextDoc
        SubText replDoc = _jeplDoc.getReplDoc();
        _textArea.setTextDoc(replDoc);

        Font codeFont = getCodeFont();
        replDoc.setDefaultStyle(new TextStyle(codeFont));
        enableEvents(_textArea, KeyRelease);

        // Create/config LineNumView
        _lineNumView = new LineNumView(_textArea);
        _lineNumView.setDefaultStyle(_lineNumView.getDefaultStyle().copyFor(codeFont));
        replDoc.addPropChangeListener(pce -> _lineNumView.updateLines());
        _lineNumView.updateLines();

        RectView rview = new RectView(0, 0, 1, 300);
        rview.setFill(Color.LIGHTGRAY);

        // Create/config EvalView
        _evalView = new JeplEvalView(this);
        ScrollView evalScroll = new ScrollView(_evalView);
        evalScroll.setPrefWidth(200);

        ScrollView textScrollView = _textArea.getParent(ScrollView.class);

        // Create RowView for LineNumView, TextArea
        RowView rowView = new RowView();
        rowView.setFillHeight(true);
        rowView.setGrowHeight(true);
        rowView.setChildren(_lineNumView, _textArea, rview, evalScroll);
        textScrollView.setContent(rowView);

        // Initialize
        //String sampleText = getSampleText();
        //_textArea.setText(sampleText);
        //_textArea.setSel(sampleText.length());
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        if (anEvent.isKeyRelease() && anEvent.isEnterKey())
            resetReplValuesLater();

        else super.respondUI(anEvent);
    }
}