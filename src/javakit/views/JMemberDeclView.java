package javakit.views;

import javakit.parse.JConstrDecl;
import javakit.parse.JMemberDecl;
import javakit.parse.JMethodDecl;
import javakit.parse.JNode;
import snap.view.*;

/**
 * A SnapPart subclass for JMethodDecl.
 */
public class JMemberDeclView<JNODE extends JMemberDecl> extends JNodeView<JNODE> {

    /**
     * Creates a SnapPart for a JNode.
     */
    public static JNodeView createView(JNode aNode)
    {
        JNodeView np = null;
        if (aNode instanceof JConstrDecl) np = new ConstructorDecl();
        else if (aNode instanceof JMethodDecl) np = new MethodDecl();
        else return null;
        np.setJNode(aNode);
        return np;
    }

    /**
     * Subclass for JMethodDecl.
     */
    public static class MethodDecl<JNODE extends JMethodDecl> extends JMemberDeclView<JNODE> {

        /**
         * Override.
         */
        protected void updateUI()
        {
            // Do normal version and set type to MemberDecl
            super.updateUI();
            setType(Type.MemberDecl);
            setColor(MemberDeclColor);

            // Configure HBox
            RowView hbox = getHBox();
            hbox.setPadding(0, 0, 0, 8);
            hbox.setMinSize(120, PieceHeight);

            // Add label for method name
            JMethodDecl md = getJNode();
            Label label = createLabel(md.getName());
            label.setFont(label.getFont().deriveFont(14));
            hbox.addChild(label);
        }

        /**
         * Returns a string describing the part.
         */
        public String getPartString()
        {
            return "Method";
        }

        /**
         * Drops a node.
         */
        protected void dropNode(JNode aNode, double anX, double aY)
        {
            if (getJNodeViewCount() == 0) getEditor().insertNode(getJNode(), aNode, 0);
            else if (aY < getHeight() / 2) getJNodeView(0).dropNode(aNode, anX, 0);
            else getJNodeViewLast().dropNode(aNode, anX, getJNodeViewLast().getHeight());
        }
    }

    /**
     * Subclass for JConstructorDecl.
     */
    public static class ConstructorDecl<JNODE extends JConstrDecl> extends MethodDecl<JNODE> {

        /**
         * Returns a string describing the part.
         */
        public String getPartString()
        {
            return "Constructor";
        }
    }
}