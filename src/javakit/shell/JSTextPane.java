/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.shell;
import snap.geom.HPos;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextLineStyle;
import snap.text.TextStyle;
import snap.view.*;
import snap.viewx.CodeView;
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
    private LineNumView  _lineNumView = new LineNumView();

    // EvalView
    protected EvalView  _evalView;

    // Font
    private Font  _defaultFont;

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
        return new CodeView();
    }

    /**
     * Returns the default font.
     */
    public Font getDefaultFont()
    {
        if (_defaultFont == null) {
            String[] names = { "Monaco", "Consolas", "Courier"};
            for (String name : names) {
                _defaultFont = new Font(name, 12);
                if (_defaultFont.getFamily().startsWith(name))
                    break;
            }
        }

        // Return
        return _defaultFont;
    }

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
        _textArea.getTextDoc().setDefaultStyle(new TextStyle(getDefaultFont()));
        enableEvents(_textArea, KeyRelease);
        ScrollView scroll = _textArea.getParent(ScrollView.class);

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
     * A TextArea subclass to show line numbers.
     */
    protected class LineNumView extends TextArea {

        /**
         * Creates new LineNumView.
         */
        public LineNumView()
        {
            setPlainText(true);
            setDefaultLineStyle(TextLineStyle.DEFAULT.copyFor(HPos.RIGHT));
            setFill(new Color("#f7f7f7"));
            setTextFill(new Color(.6f));
            setPrefWidth(25);
            setPadding(2, 4, 2, 2);
            setEditable(false);
            setFont(JSTextPane.this.getDefaultFont());
        }

        /**
         * Called to update when TextArea changes.
         */
        void updateLines()
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 1, iMax = _textArea.getLineCount(); i <= iMax; i++)
                sb.append(i).append('\n');
            setText(sb.toString());
        }
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
                    String arrayStr = toStringArray(val);
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
    }

    /**
     * Returns a String for given array object.
     */
    private static String toStringArray(Object anObj)
    {
        if (anObj instanceof Object[]) return Arrays.toString((Object[]) anObj);
        else if (anObj instanceof double[]) return Arrays.toString((double[]) anObj);
        else if (anObj instanceof float[]) return Arrays.toString((float[]) anObj);
        else if (anObj instanceof int[]) return Arrays.toString((int[]) anObj);

        int len = Array.getLength(anObj);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) array[i] = Array.get(anObj, i);
        return Arrays.toString(array);
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
     * Sample starter text.
     */
    private static String getSampleText2()
    {
        String t1 = "double[] x = new double[] { 1,2,3 };\n\n";
        String t2 = "DoubleStream xStr = DoubleStream.of(x);\n\n";
        String t3 = "DoubleStream yStr = xStr.map(d -> d * d);\n\n";
        String t4 = "double[] y = yStr.toArray();\n\n";
        return '\n' + t1 + t2 + t3 + t4;
    }
}