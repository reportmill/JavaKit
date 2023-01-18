/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.parse.*;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This JavaFileBuilder implementation simply resolves the node tree to find errors.
 */
public class JavaFileBuilderSimple extends JavaFileBuilder {

    /**
     * Constructor for given Project.
     */
    public JavaFileBuilderSimple(Project aProject)
    {
        super(aProject);
    }

    /**
     * Compiles files.
     */
    @Override
    public boolean buildFiles(TaskMonitor aTaskMonitor)
    {
        // Empty case
        if (_buildFiles.size() == 0) return true;

        // Get files
        WebFile[] javaFiles = _buildFiles.toArray(new WebFile[0]);
        _buildFiles.clear();

        // Iterate over files and build
        boolean success = true;
        for (WebFile javaFile : javaFiles)
            success &= buildFile(javaFile);

        // Return
        return success;
    }

    /**
     * Builds a file.
     */
    public boolean buildFile(WebFile javaFile)
    {
        // Get JavaAgent and JFile
        JavaAgent javaAgent = JavaAgent.getAgentForFile(javaFile);
        JFile jFile = javaAgent.getJFile();
        List<NodeError> errorsList = new ArrayList<>();

        // Find errors in JFile
        findNodeErrors(jFile, errorsList);

        // Set project errors
        boolean success = errorsList.size() == 0;
        setErrorsForJavaAgent(javaAgent, errorsList);

        // Return
        return success;
    }

    /**
     * Recurse into nodes
     */
    private static void findNodeErrors(JNode aNode, List<NodeError> theErrors)
    {
        NodeError[] errors = aNode.getErrors();
        if (errors.length > 0)
            Collections.addAll(theErrors, errors);

        if (aNode instanceof JStmtExpr)
            return;

        List<JNode> children = aNode.getChildren();
        for (JNode child : children)
            findNodeErrors(child, theErrors);
    }

    /**
     * Sets the errors for given JavaAgent and NodeError list.
     */
    private void setErrorsForJavaAgent(JavaAgent javaAgent, List<NodeError> theNodeErrors)
    {
        // Get Project.BuildIssues and clear
        Project proj = javaAgent.getProject();
        BuildIssues projBuildIssues = proj.getBuildIssues();

        // Remove old issues
        WebFile javaFile = javaAgent.getFile();
        BuildIssue[] oldIssues = projBuildIssues.getIssuesForFile(javaFile);
        for (BuildIssue buildIssue : oldIssues)
            projBuildIssues.remove(buildIssue);

        // Add new issues
        for (NodeError nodeError : theNodeErrors) {
            BuildIssue buildIssue = BuildIssue.createIssueForNodeError(nodeError, javaFile);
            if (buildIssue != null)
                projBuildIssues.add(buildIssue);
        }
    }
}
