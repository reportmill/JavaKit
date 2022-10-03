package javakit.shell;
import snap.gfx.Color;
import snap.view.TextArea;

/**
 * A TextArea subclass to show code evaluation.
 */
class JeplEvalView extends TextArea {

    // The JeplTextPane
    private JeplTextPane _jeplTextPane;

    /**
     * Constructor.
     */
    public JeplEvalView(JeplTextPane aJTP)
    {
        _jeplTextPane = aJTP;
        setFill(new Color("#f7f7f7"));
        setTextFill(Color.GRAY); //setPrefWidth(200);
        setEditable(false);
        setFont(getDefaultFont());
    }

    /**
     * Called to update when textView changes.
     */
    void updateLines()
    {
        // Get JavaShell and line values
        JavaShell javaShell = _jeplTextPane._javaShellPane.getJavaShell();
        Object[] lineValues = javaShell.getLineValues();

        // Get LineVals
        StringBuilder sb = new StringBuilder();

        // Iterate over LineVals and append string for each
        for (Object val : lineValues) {

            // Handle null
            if (val == null) continue;

            // Handle array
            if (val.getClass().isArray()) {
                String arrayStr = JavaShellUtils.getStringForArray(val);
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
