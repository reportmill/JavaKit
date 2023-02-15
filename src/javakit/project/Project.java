/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.resolver.Resolver;
import snap.props.PropChange;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * This class manages all aspects of a project.
 */
public class Project {

    // The Workspace that owns this project
    private Workspace  _workspace;

    // The encapsulated data site
    protected WebSite  _site;

    // ProjectConfig
    protected ProjectConfig  _projConfig;

    // ProjectFiles
    protected ProjectFiles  _projFiles;

    // The ProjectBuilder
    protected ProjectBuilder  _projBuilder;

    // The set of projects this project depends on
    private ProjectSet  _projSet;

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

        // Create ProjectSet
        _projSet = new ProjectSet(this);
        _projSet.getProjects();
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
    public ProjectSet getProjectSet()  { return _projSet; }

    /**
     * Returns an array of projects that this project depends on.
     */
    public Project[] getProjects()  { return _projSet.getProjects(); }

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
