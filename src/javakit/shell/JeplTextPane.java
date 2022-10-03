/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import javakit.text.JavaTextUtils;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextStyle;
import snap.view.*;
import snap.viewx.CodeView;
import snap.viewx.TextPane;

/**
 * This TexPane subclass adds customizations for JavaShell.
 */
public class JeplTextPane extends TextPane {

    // The JavaShell
    protected JavaShellPane  _javaShellPane;

    // The TextArea
    private TextArea  _textArea;

    // LineNumView
    private LineNumView  _lineNumView;

    // EvalView
    protected JeplEvalView  _evalView;

    /**
     * Constructor.
     */
    public JeplTextPane(JavaShellPane aJavaShellPane)
    {
        _javaShellPane = aJavaShellPane;
    }

    /**
     * Returns the default font.
     */
    public Font getJeplFont()  { return JavaTextUtils.getCodeFont(); }

    /**
     * Creates the TextArea.
     */
    protected TextArea createTextArea()
    {
        return new CodeView();
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
        Font devFont = getJeplFont();
        _textArea.getTextDoc().setDefaultStyle(new TextStyle(devFont));
        enableEvents(_textArea, KeyRelease);

        // Create/config LineNumView
        _lineNumView = new LineNumView(_textArea);
        _lineNumView.setFont(devFont);
        _textArea.getTextDoc().addPropChangeListener(pce -> _lineNumView.updateLines());
        _lineNumView.updateLines();

        RectView rview = new RectView(0, 0, 1, 300);
        rview.setFill(Color.LIGHTGRAY);

        // Create/config EvalView
        _evalView = new JeplEvalView(this);
        ScrollView evalScroll = new ScrollView(_evalView);
        evalScroll.setPrefWidth(200);

        // Create RowView for LineNumView, TextArea
        RowView rowView = new RowView();
        rowView.setFillHeight(true);
        rowView.setGrowHeight(true);
        rowView.setChildren(_lineNumView, _textArea, rview, evalScroll);
        ScrollView textScrollView = _textArea.getParent(ScrollView.class);
        textScrollView.setContent(rowView);

        // Initialize
        String sampleText = getSampleText();
        _textArea.setText(sampleText);
        _textArea.setSel(sampleText.length());
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        if (anEvent.isKeyRelease() && anEvent.isEnterKey())
            runLater(() -> _javaShellPane.play());

        else super.respondUI(anEvent);
    }

    /**
     * Sample starter text.
     */
    private static String getSampleText()
    {
        String t1 = "x = \"Hi World\"\n\n";
        String t2 = "y = x.replace(\"Hi\", \"Hello\")\n\n";
        String t3 = "z = new double[] { 1,2,3 }\n\n";
        String t4 = "System.out.println(y + \": \" + z[2])\n\n";
        String t5 = "zSquared = DoubleStream.of(z).map(d -> d * d).toArray()\n\n";
        return '\n' + t1 + t2 + t3 + t4 + t5;
        //return "1 + 1\n\n2 + 2\n\n";
    }
}