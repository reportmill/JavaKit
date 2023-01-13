/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.ide;
import snap.util.SnapList;
import snap.web.WebFile;

import java.util.*;
import java.util.stream.Stream;

/**
 * A class to manage a list of BuildIssue for a project.
 */
public class BuildIssues extends SnapList<BuildIssue> {

    // The project
    private Project  _proj;

    // The total count of errors and warnings
    private int  _errorCount;

    // The total count of errors and warnings
    private int  _warningCount;

    // A map to track BuildIssues by WebFile
    Map<WebFile,List<BuildIssue>>  _fileIssues = new Hashtable<>();

    // An Empty array of BuildIssues
    public static final BuildIssue[] NO_ISSUES = new BuildIssue[0];

    /**
     * Constructor.
     */
    public BuildIssues(Project aProj)
    {
        _proj = aProj;
    }

    /**
     * Returns the number of errors currently tracked.
     */
    public int getErrorCount()  { return _errorCount; }

    /**
     * Returns the number of warnings currently tracked.
     */
    public int getWarningCount()  { return _warningCount; }

    /**
     * Returns an array of the currently tracked issues.
     */
    public BuildIssue[] getIssues()
    {
        return super.getArray(BuildIssue.class);
    }

    /**
     * Adds a BuildIssue at sorted index.
     */
    public boolean add(BuildIssue aBI)
    {
        // Get insertion index (just return if already in list)
        int index = -Collections.binarySearch(this, aBI) - 1;
        if (index < 0)
            return false;

        // Add to file list
        WebFile buildIssueFile = aBI.getFile();
        List<BuildIssue> buildIssuesForFile = _fileIssues.computeIfAbsent(buildIssueFile, k -> new ArrayList<>());
        buildIssuesForFile.add(aBI);

        // Update ErrorCount/WarningCount
        if (aBI.isError())
            _errorCount++;
        else _warningCount++;

        // Add issue
        add(index, aBI);

        // Return
        return true;
    }

    /**
     * Removes a BuildIssue.
     */
    public void remove(BuildIssue aBI)
    {
        // Remove from file
        WebFile buildIssueFile = aBI.getFile();
        List<BuildIssue> buildIssues = _fileIssues.get(buildIssueFile);
        if (buildIssues != null) {
            buildIssues.remove(aBI);
            if (buildIssues.size() == 0)
                _fileIssues.remove(buildIssueFile);
        }

        // Update ErrorCount/WarningCount
        if (aBI.isError())
            _errorCount--;
        else _warningCount--;

        // Remove from master list
        super.remove(aBI);
    }

    /**
     * Override to clear FileIssues cache.
     */
    @Override
    public void clear()
    {
        super.clear();
        _fileIssues.clear();
    }

    /**
     * Returns the BuildIssues for a given file.
     */
    public BuildIssue[] getIssuesForFile(WebFile aFile)
    {
        List<BuildIssue> buildIssues;

        // Handle file: Just load from map
        if (aFile.isFile())
            buildIssues = _fileIssues.get(aFile);

            // Handle directory: aggregate directory files
        else {
            buildIssues = new ArrayList<>();
            for (WebFile file : aFile.getFiles())
                Collections.addAll(buildIssues, getIssuesForFile(file));
        }

        // Return list
        return buildIssues != null ? buildIssues.toArray(new BuildIssue[0]) : NO_ISSUES;
    }

    /**
     * Removes the build issues for a file.
     */
    public void removeIssuesForFile(WebFile aFile)
    {
        BuildIssue[] issues = getIssuesForFile(aFile);
        for (BuildIssue i : issues) remove(i);
    }

    /**
     * Returns the build status for a file.
     */
    public BuildIssue.Kind getBuildStatusForFile(WebFile aFile)
    {
        // Handle file: Get build issues and return worst kind
        if (aFile.isFile()) {

            // Get build issues - just return null if none
            BuildIssue[] buildIssues = getIssuesForFile(aFile);
            if (buildIssues.length == 0)
                return null;

            // If any issue is error, return error
            boolean containsError = Stream.of(buildIssues).anyMatch(issue -> issue.getKind() == BuildIssue.Kind.Error);
            if (containsError)
                    return BuildIssue.Kind.Error;

            // Return warning
            return BuildIssue.Kind.Warning;
        }

        // If directory, return worst status of children (if package, don't recurse)
        BuildIssue.Kind status = null;
        boolean isPkg = aFile.getType().length() == 0 && _proj.getSourceDir().contains(aFile);

        // Iterate over directory files
        WebFile[] dirFiles = aFile.getFiles();
        for (WebFile childFile : dirFiles) {

            // Skip packages
            if (childFile.isDir() && isPkg) continue;

            // Get status for child file
            BuildIssue.Kind childStatus = getBuildStatusForFile(childFile);
            if (childStatus != null)
                status = childStatus;
            if (childStatus == BuildIssue.Kind.Error)
                return status;
        }

        // Return
        return status;
    }
}