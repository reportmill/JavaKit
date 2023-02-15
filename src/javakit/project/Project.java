/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.resolver.Resolver;
import snap.props.PropChange;
import snap.props.PropObject;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages all aspects of a project.
 */
public class Project extends PropObject {

    // The Workspace that owns this project
    private Workspace  _workspace;

    // The encapsulated data site
    protected WebSite  _site;

    // The child projects this project depends on
    private Project[]  _projects;

    // ProjectConfig
    protected ProjectConfig  _projConfig;

    // ProjectFiles
    protected ProjectFiles  _projFiles;

    // The ProjectBuilder
    protected ProjectBuilder  _projBuilder;

    // The set of projects this project depends on
    private ProjectSet  _projSet;

    // Constants for properties
    private static final String Projects_Prop = "Projects";

    /**
     * Creates a new Project for WebSite.
     */
    public Project(Workspace aWorkspace, WebSite aSite)
    {
        _workspace = aWorkspace;

        // Set site
        setSite(aSite);

        // If site doesn't exist, create root directory, src, bin and default .classpath file
        if (!aSite.getExists()) {
            aSite.getRootDir().save();
            aSite.createFileForPath("/src", true).save();
            aSite.createFileForPath("/bin", true).save();
        }

        // Create/set ProjectFiles, ProjectBuilder
        _projFiles = new ProjectFiles(this);
        _projBuilder = new ProjectBuilder(this);

        // Get child projects
        _projects = getProjectsFromConfig();
    }

    /**
     * Returns the Workspace that manages this project.
     */
    public Workspace getWorkspace()  { return _workspace; }

    /**
     * Returns root project if part of hierarchy.
     */
    public Project getRootProject()  { return this; }

    /**
     * Returns the project name.
     */
    public String getName()
    {
        return _site.getName();
    }

    /**
     * Returns the encapsulated WebSite.
     */
    public WebSite getSite()  { return _site; }

    /**
     * Sets the encapsulated WebSite.
     */
    protected void setSite(WebSite aSite)
    {
        _site = aSite;
        _site.setProp(Project.class.getSimpleName(), this);
    }

    /**
     * Returns the ProjectConfig that manages project properties.
     */
    public ProjectConfig getProjectConfig()
    {
        // If already set, just return
        if (_projConfig != null) return _projConfig;

        // Create ProjectConfig
        ProjectConfig projConfig = createProjectConfig();

        // Add PropChangeListener to clear ClassPathInfo when changed
        projConfig.addPropChangeListener(pc -> projConfigDidPropChange(pc));

        // Set, return
        return _projConfig = projConfig;
    }

    /**
     * Returns the ProjectConfig that manages project properties.
     */
    protected ProjectConfig createProjectConfig()
    {
        return new ProjectConfig(this);
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getClassPaths()  { return _projConfig.getClassPaths(); }

    /**
     * Returns the set of projects this project depends on.
     */
    public ProjectSet getProjectSet()
    {
        if (_projSet != null) return _projSet;
        return _projSet = new ProjectSet(this);
    }

    /**
     * Returns an array of projects that this project depends on.
     */
    public Project[] getProjects()  { return _projects; }

    /**
     * Adds a project.
     */
    public void addProject(Project aProj)
    {
        // If already set, just return
        if (ArrayUtils.containsId(_projects, aProj)) return;

        // Add project
        _projects = ArrayUtils.add(_projects, aProj);
        _projSet = null;

        // Fire prop change
        int index = _projects.length - 1;
        firePropChange(Projects_Prop, null, aProj, index);
    }

    /**
     * Removes a project.
     */
    public void removeProject(Project aProj)
    {
        // If not present, just return
        int index = ArrayUtils.indexOfId(_projects, aProj);
        if (index < 0)
            return;

        // Remove project
        _projects = ArrayUtils.remove(_projects, index);
        _projSet = null;

        // Fire prop change
        firePropChange(Projects_Prop, aProj, null, index);
    }

    /**
     * Returns a project for given path.
     */
    public Project getProjectForPath(String projectPath)
    {
        // Get parent site
        WebSite projectSite = getSite();
        WebURL parentSiteURL = projectSite.getURL();
        WebSite parentSite = parentSiteURL.getSite();

        // Get URL and site for project path
        WebURL projURL = parentSite.getURL(projectPath);
        WebSite projSite = projURL.getAsSite();

        // Get Project
        Workspace workspace = getWorkspace();
        return workspace.getProjectForSite(projSite);
    }

    /**
     * Adds a dependent project.
     */
    public void addProjectForPath(String aPath)
    {
        // Get project path
        String projPath = aPath;
        if (!projPath.startsWith("/"))
            projPath = '/' + projPath;

        // Get project
        Project proj = getProjectForPath(aPath);
        if (proj == null)
            return;
        addProject(proj);

        // Add to ProjectConfig
        ProjectConfig projConfig = getProjectConfig();
        projConfig.addSrcPath(projPath);
    }

    /**
     * Removes a dependent project.
     */
    public void removeProjectForPath(String projectPath)
    {
        // Get project and remove
        Project proj = getProjectForPath(projectPath);
        removeProject(proj);

        // Remove from config
        ProjectConfig projConfig = getProjectConfig();
        projConfig.removeSrcPath(projectPath);
    }

    /**
     * Returns the projects this project depends on.
     */
    private Project[] getProjectsFromConfig()
    {
        // Create list of projects from ClassPath.ProjectPaths
        ProjectConfig projConfig = getProjectConfig();
        String[] projPaths = projConfig.getProjectPaths();
        List<Project> projs = new ArrayList<>();

        // Iterate over project paths
        for (String projPath : projPaths) {

            // Get Project
            Project proj = getProjectForPath(projPath);

            // Add to list
            Project[] childProjects = proj.getProjects();
            ListUtils.addAllUnique(projs, childProjects);
            ListUtils.addUnique(projs, proj);
        }

        // Return array
        return projs.toArray(new Project[0]);
    }

    /**
     * Returns the ProjectBuilder.
     */
    public ProjectBuilder getBuilder()  { return _projBuilder; }

    /**
     * Returns the resolver.
     */
    public Resolver getResolver()
    {
        Workspace workspace = getWorkspace();
        return workspace.getResolver();
    }

    /**
     * Returns whether file is config file.
     */
    protected boolean isConfigFile(WebFile aFile)  { return false; }

    /**
     * Reads the settings from project settings file(s).
     */
    public void readSettings()  { }

    /**
     * Returns the ProjectFiles that manages project files.
     */
    public ProjectFiles getProjectFiles()  { return _projFiles; }

    /**
     * Returns the source directory.
     */
    public WebFile getSourceDir()  { return _projFiles.getSourceDir(); }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()  { return _projFiles.getBuildDir(); }

    /**
     * Returns a file for given path.
     */
    public WebFile getFile(String aPath)
    {
        return _site.getFileForPath(aPath);
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath, boolean doCreate, boolean isDir)
    {
        return _projFiles.getSourceFile(aPath, doCreate, isDir);
    }

    /**
     * Returns the class for given file.
     */
    public Class<?> getClassForFile(WebFile aFile)
    {
        String className = getClassNameForFile(aFile);
        Resolver resolver = getResolver();
        return resolver.getClassForName(className);
    }

    /**
     * Returns the class name for given class file.
     */
    public String getClassNameForFile(WebFile aFile)
    {
        return _projFiles.getClassNameForFile(aFile);
    }

    /**
     * Watches Project.ProjectConfig for JarPaths change to reset ClassPathInfo.
     */
    private void projConfigDidPropChange(PropChange anEvent)
    {
        String propName = anEvent.getPropName();
        if (propName == ProjectConfig.JarPaths_Prop) {
            Workspace workspace = getWorkspace();
            workspace.clearClassLoader();
        }
    }


    /**
     * Called when file added.
     */
    public void fileAdded(WebFile aFile)
    {
        if (isConfigFile(aFile))
            readSettings();

        // Add build file
        _projBuilder.addBuildFile(aFile, false);
    }

    /**
     * Called when file removed.
     */
    public void fileRemoved(WebFile aFile)
    {
        // Remove build files
        _projBuilder.removeBuildFile(aFile);

        // Remove BuildIssues for file
        Workspace workspace = getWorkspace();
        BuildIssues buildIssues = workspace.getBuildIssues();
        buildIssues.removeIssuesForFile(aFile);
    }

    /**
     * Called when file saved.
     */
    public void fileSaved(WebFile aFile)
    {
        // If File is config file, read file
        if (isConfigFile(aFile))
            readSettings();

        // If plain file, add as BuildFile
        if (!aFile.isDir())
            _projBuilder.addBuildFile(aFile, false);
    }

    /**
     * Deletes the project.
     */
    public void deleteProject(TaskMonitor aTM) throws Exception
    {
        // Start TaskMonitor
        aTM.startTasks(1);
        aTM.beginTask("Deleting files", -1);

        // Clear ClassLoader
        Workspace workspace = getWorkspace();
        workspace.clearClassLoader();

        // Delete SandBox, Site
        WebSite projSite = getSite();
        WebSite projSiteSandbox = projSite.getSandbox();
        projSiteSandbox.deleteSite();
        projSite.deleteSite();

        // Finish TaskMonitor
        aTM.endTask();
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return "Project: " + getSite();
    }

    /**
     * Returns the project for a given site.
     */
    public static Project getProjectForFile(WebFile aFile)
    {
        WebSite site = aFile.getSite();
        return getProjectForSite(site);
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized Project getProjectForSite(WebSite aSite)
    {
        Project proj = (Project) aSite.getProp(Project.class.getSimpleName());
        return proj;
    }
}
