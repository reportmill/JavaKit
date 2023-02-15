package javakit.project;
import snap.util.ListUtils;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a project and the set of projects it depends on.
 */
public class ProjectSet {

    // The master project
    private Project  _proj;

    // The list of projects this project depends on
    private Project[]  _projects;

    // The array of class paths
    private String[]  _classPaths;

    // The array of library paths
    private String[]  _libPaths;

    /**
     * Creates a new ProjectSet for given Project.
     */
    public ProjectSet(Project aProj)
    {
        _proj = aProj;
        _projects = aProj.getProjects();
    }

    /**
     * Returns the list of projects this project depends on.
     */
    public Project[] getProjects()  { return _projects; }

    /**
     * Returns a file for given path.
     */
    public WebFile getFile(String aPath)
    {
        // Check main project
        WebFile file = _proj.getFile(aPath);
        if (file != null)
            return file;

        // Get projects
        Project[] projects = getProjects();

        // Check projects
        for (Project proj : projects) {
            file = proj.getFile(aPath);
            if (file != null)
                return file;
        }

        // Return not found
        return null;
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath)
    {
        // Look for file in root project, then dependent projects
        WebFile file = _proj.getSourceFile(aPath, false, false);
        if (file != null)
            return file;

        // Get projects
        Project[] projects = getProjects();

        // Check projects
        for (Project proj : projects) {
            file = proj.getSourceFile(aPath, false, false);
            if (file != null)
                return file;
        }

        // Return not found
        return null;
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getClassPaths()
    {
        // If already set, just return
        if (_classPaths != null) return _classPaths;

        // Get Project ClassPaths
        String[] classPaths = _proj.getClassPaths();

        // Get projects
        Project[] projects = getProjects();

        // Get list for LibPaths with base paths
        List<String> classPathsList = new ArrayList<>();
        Collections.addAll(classPathsList, classPaths);

        // Iterate over projects and add Project.ClassPaths for each
        for (Project proj : projects) {
            String[] projClassPaths = proj.getClassPaths();
            ListUtils.addAllUnique(classPathsList, projClassPaths);
        }

        // Set/return
        return _classPaths = classPathsList.toArray(new String[0]);
    }

    /**
     * Returns the paths needed to compile/run project, except build directory.
     */
    public String[] getLibPaths()
    {
        // If already set, just return
        if (_libPaths != null) return _libPaths;

        // Get LibPaths for this proj
        ProjectConfig projConfig = _proj.getProjectConfig();
        String[] libPaths = projConfig.getLibPathsAbsolute();

        // Get projects
        Project[] projects = getProjects();

        // Get list for LibPaths with base paths
        List<String> libPathsList = new ArrayList<>();
        Collections.addAll(libPathsList, libPaths);

        // Iterate over projects and add Project.ClassPaths for each
        for (Project proj : projects) {
            String[] projClassPaths = proj.getClassPaths();
            ListUtils.addAllUnique(libPathsList, projClassPaths);
        }

        // Set/return
        return _libPaths = libPathsList.toArray(new String[0]);
    }

    /**
     * Returns a Java file for class name.
     */
    public WebFile getJavaFileForClassName(String aClassName)
    {
        // Check main project
        ProjectFiles projFiles = _proj.getProjectFiles();
        WebFile file = projFiles.getJavaFileForClassName(aClassName);
        if (file != null)
            return file;

        // Check subprojects
        Project[] projects = getProjects();
        for (Project proj : projects) {
            projFiles = proj.getProjectFiles();
            file = projFiles.getJavaFileForClassName(aClassName);
            if (file != null)
                return file;
        }

        // Return not found
        return null;
    }
}