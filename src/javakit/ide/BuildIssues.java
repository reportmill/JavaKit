package javakit.ide;

import snap.util.SnapList;
import snap.web.WebFile;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * A class to manage a list of BuildIssue for a project.
 */
public class BuildIssues extends SnapList<BuildIssue> {

    // The project
    Project _proj;

    // The total count of errors and warnings
    int _ec, _wc;

    // A map to track BuildIssues by WebFile
    Map<WebFile, SnapList<BuildIssue>> _fileIssues = new Hashtable();

    // An Empty array of BuildIssues
    public static final BuildIssue[] NO_ISSUES = new BuildIssue[0];

    /**
     * Creates new BuildIssues for Project.
     */
    public BuildIssues(Project aProj)
    {
        _proj = aProj;
    }

    /**
     * Returns an array of the currently tracked issues.
     */
    public BuildIssue[] getArray()
    {
        return super.getArray(BuildIssue.class);
    }

    /**
     * Returns the BuildIssues for a given file.
     */
    public BuildIssue[] getIssues(WebFile aFile)
    {
        // Get list of issues for file (just load from map) or directory (aggregate directory files)
        SnapList<BuildIssue> list = null;
        if (aFile.isFile()) list = _fileIssues.get(aFile);
        else {
            list = new SnapList();
            for (WebFile file : aFile.getFiles())
                Collections.addAll(list, getIssues(file));
        }

        // Return list (or EMPTY_LIST if null)
        return list != null ? list.getArray(BuildIssue.class) : NO_ISSUES;
    }

    /**
     * Adds a BuildIssue at sorted index.
     */
    public boolean add(BuildIssue aBI)
    {
        // Add to master list
        int index = -Collections.binarySearch(this, aBI) - 1;
        if (index < 0) return false;

        // Add to file list
        SnapList list = _fileIssues.get(aBI.getFile());
        if (list == null) _fileIssues.put(aBI.getFile(), list = new SnapList());
        list.add(aBI);

        // Update ErrorCount/WarningCount
        if (aBI.isError()) _ec++;
        else _wc++;
        add(index, aBI);
        return true;
    }

    /**
     * Removes a BuildIssue.
     */
    public void remove(BuildIssue aBI)
    {
        // Remove from file
        List list = _fileIssues.get(aBI.getFile());
        if (list != null) {
            list.remove(aBI);
            if (list.size() == 0) _fileIssues.remove(aBI.getFile());
        }

        // Update ErrorCount/WarningCount
        if (aBI.isError()) _ec--;
        else _wc--;

        // Remove from master list
        super.remove(aBI);
    }

    /**
     * Removes the build issues for a file.
     */
    public void remove(WebFile aFile)
    {
        BuildIssue[] issues = getIssues(aFile);
        for (BuildIssue i : issues) remove(i);
    }

    /**
     * Returns the number of errors currently tracked.
     */
    public int getErrorCount()
    {
        return _ec;
    }

    /**
     * Returns the number of warnings currently tracked.
     */
    public int getWarningCount()
    {
        return _wc;
    }

    /**
     * Returns the build status for a file.
     */
    public BuildIssue.Kind getBuildStatus(WebFile aFile)
    {
        // If file, iterate over file issues
        if (aFile.isFile()) {
            BuildIssue[] list = getIssues(aFile);
            if (list.length == 0) return null;
            for (BuildIssue item : list)
                if (item.getKind() == BuildIssue.Kind.Error)
                    return BuildIssue.Kind.Error;
            return BuildIssue.Kind.Warning;
        }

        // If directory, return worst status of children (if package, don't recurse)
        BuildIssue.Kind status = null;
        boolean isPkg = aFile.getType().length() == 0 && _proj.getSourceDir().contains(aFile);
        for (WebFile file : aFile.getFiles()) {
            if (file.isDir() && isPkg) continue;                  // If package, don't recurse
            BuildIssue.Kind st = getBuildStatus(file);
            if (st != null) status = st;
            if (st == BuildIssue.Kind.Error) return status;
        }
        return status;
    }

}