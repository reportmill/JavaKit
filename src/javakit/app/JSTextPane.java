/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.app;
import javakit.runner.JavaShell;
import javakit.runner.JavaShellUtils;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextStyle;
import snap.view.*;
import snap.viewx.CodeDoc;
import snap.viewx.TextPane;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * This TexPane subclass adds customizations for JavaShell.
 */
public class JSTextPane extends TextPane {

    // The JavaShell
    private JavaShellPane _javaShellPane;

    // The TextArea
    private TextArea  _textArea;

    // LineNumView
    private LineNumView  _lineNumView;

    // EvalView
    protected EvalView  _evalView;

    /**
     * Constructor.
     */
    public JSTextPane(JavaShellPane aJavaShellPane)
    {
        _javaShellPane = aJavaShellPane;
    }

    /**
     * Creates the TextArea.
     */
    protected TextArea createTextArea()
    {
        TextArea textArea = new TextArea();
        textArea.setFill(Color.WHITE);
        textArea.setEditable(true);
        textArea.setTextDoc(new CodeDoc());
        return textArea;
    }

    /**
     * Returns the default font.
     */
    public Font getDefaultFont()  { return JavaTextUtils.getCodeFont(); }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        super.initUI();
        View ui = getUI();
        ui.setPrefSize(800, 700);
        ui.setGrowHeight(true);

        _textArea = getTextArea();
        _textArea.setGrowWidth(true);
        Font codeFont = getDefaultFont();
        _textArea.getTextDoc().setDefaultStyle(new TextStyle(codeFont));
        enableEvents(_textArea, KeyRelease);
        ScrollView scroll = _textArea.getParent(ScrollView.class);

        _lineNumView = new LineNumView(_textArea);
        _lineNumView.setFont(codeFont);
        _textArea.getTextDoc().addPropChangeListener(pce -> _lineNumView.updateLines());
        _lineNumView.updateLines();

        RectView rview = new RectView(0, 0, 1, 300);
        rview.setFill(Color.LIGHTGRAY);

        // Create/config EvalView
        _evalView = new EvalView();
        ScrollView evalScroll = new ScrollView(_evalView);
        evalScroll.setPrefWidth(200);

        RowView hbox = new RowView();
        hbox.setFillHeight(true);
        hbox.setGrowHeight(true);
        hbox.setChildren(_lineNumView, _textArea, rview, evalScroll);
        scroll.setContent(hbox);

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

    /**
     * A TextArea subclass to show code evaluation.
     */
    protected class EvalView extends TextArea {

        /**
         * Creates new EvalView.
         */
        public EvalView()
        {
            setFill(new Color("#f7f7f7"));
            setTextFill(Color.GRAY); //setPrefWidth(200);
            setEditable(false);
            setFont(JSTextPane.this.getDefaultFont());
        }

        /**
         * Called to update when textView changes.
         */
        void updateLines()
        {
            // Get JavaShell and line values
            JavaShell javaShell = _javaShellPane.getJavaShell();
            Object[] lineValues = javaShell.getLineValues();

            // Get LineVals
            StringBuilder sb = new StringBuilder();

            // Iterate over LineVals and append string for each
            for (Object val : lineValues) {

                // Handle null
                if (val == null) continue;

                // Handle array
                if (val.getClass().isArray()) {
                    String arrayStr = getStringForArray(val);
                    sb.append(arrayStr);
                }

                // Handle anything else
                else sb.append(val);

                // Append newline
                sb.append('\n');
            }

            // Set text
            setText(sb.toString());
        }

        /**
         * Returns a String for given array object.
         */
        private String getStringForArray(Object anObj)
        {
            if (anObj instanceof Object[])
                return Arrays.toString((Object[]) anObj);
            if (anObj instanceof double[])
                return Arrays.toString((double[]) anObj);
            if (anObj instanceof float[])
                return Arrays.toString((float[]) anObj);
            if (anObj instanceof int[])
                return Arrays.toString((int[]) anObj);

            int len = Array.getLength(anObj);
            Object[] array = new Object[len];
            for (int i = 0; i < len; i++)
                array[i] = Array.get(anObj, i);
            return Arrays.toString(array);
        }
    }
}