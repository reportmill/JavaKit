package javakit.shell;
import snap.gfx.Color;
import snap.view.ColView;
import snap.view.View;

/**
 * This view class shows graphics output.
 */
public class JSConsoleShelf extends ColView {

    // Constants
    private static Color BACK_FILL = new Color(226, 232, 246);

    /**
     * Constructor.
     */
    public JSConsoleShelf()
    {
        super();
        setFill(BACK_FILL);
    }

    /**
     * Updates the shelf.
     */
    public void updateShelf(JavaShell javaShell)
    {
        // Remove children
        removeChildren();

        // Get line vals
        Object[] lineVals = javaShell._evaluator._lineVals;

        // Iterate over LineVals and append string for each
        for (Object val : lineVals) {

            // Handle null
            if (val instanceof View)
                appendView((View) val);
        }
    }

    /**
     * Appends a view.
     */
    public void appendView(View aView)
    {
        if (aView.getMargin().isEmpty())
            aView.setMargin(10, 10, 10, 10);
        if (aView.getParent() == null)
            addChild(aView);
    }
}
