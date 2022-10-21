package javakit.app;
import javakit.parse.JavaReplDoc;
import snap.geom.Insets;
import snap.gfx.Color;
import snap.view.ColView;
import snap.view.View;

/**
 * A TextArea subclass to show code evaluation.
 */
class JeplEvalView extends ColView {

    // The JeplTextPane
    private JeplTextPane _jeplTextPane;

    /**
     * Constructor.
     */
    public JeplEvalView(JeplTextPane aJTP)
    {
        _jeplTextPane = aJTP;
        setSpacing(6);
        setFill(new Color(.98));

        // Set Padding to match TextArea
        Insets textPadding = aJTP.getTextArea().getPadding();
        setPadding(textPadding);
    }

    /**
     * Called to update when textView changes.
     */
    void updateLines()
    {
        removeChildren();

        // Get JavaShell and line values
        JavaReplDoc jeplDoc = _jeplTextPane._jeplDoc;
        View[] lineViews = jeplDoc.getReplViews();
        if (lineViews == null)
            return;

        // Iterate over LineVals and append string for each
        for (View view : lineViews) {

            // Handle null
            if (view == null) continue;
            addChild(view);
        }
    }
}
