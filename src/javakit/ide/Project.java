/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.ide;
import javakit.resolver.Resolver;
import snap.props.PropChange;
import snap.util.FilePathUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This is a Resolver subclass.
 */
public class Project {

    // The encapsulated data site
    protected WebSite  _site;

    // ProjectConfig
    protected ProjectConfig  _projConfig;

    // ProjectFiles
    protected ProjectFiles  _projFiles;

    // The ProjectBuilder
    protected ProjectBuilder  _projBuilder;

    // The resolver
    protected Resolver _resolver;

    // The ClassLoader for compiled class info
    protected ClassLoader  _classLoader;

    // A list of build issues
    private BuildIssues  _buildIssues;

    // The list of Breakpoints
    private Breakpoints  _breakpoints;

    /**
     * Creates a new Project for WebSite.
     */
    public Project(WebSite aSite)
    {
        // Set site
        setSite(aSite);

        // If site doesn't exist, create root directory, src, bin and default .classpath file
        if (!aSite.getExists()) {
            aSite.getRootDir().save();
            aSite.createFile("/src", true).save();
            aSite.createFile("/bin", true).save();
        }

        // Create/set ProjectFiles, ProjectBuilder
        _projFiles = new ProjectFiles(this);
        _projBuilder = new ProjectBuilder(this);
    }

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
     * Returns the ProjectBuilder.
     */
    public ProjectBuilder getProjectBuilder()  { return _projBuilder; }

    /**
     * Builds the project.
     */
    public boolean buildProject(TaskMonitor aTM)
    {
        return _projBuilder.buildProject(aTM);
    }

    /**
     * Interrupts build.
     */
    public void interruptBuild()
    {
        _projBuilder.interruptBuild();
    }

    /**
     * Removes all build files from project.
     */
    public void cleanProject()
    {
        _projBuilder.cleanProject();
    }

    /**
     * Adds a build file.
     */
    public void addBuildFilesAll()
    {
        _projBuilder.addBuildFilesAll();
    }

    /**
     * Returns the resolver.
     */
    public Resolver getResolver()
    {
        // If already set, just return
        if (_resolver != null) return _resolver;

        // Create Resolver
        ClassLoader classLoader = getClassLoader();
        Resolver resolver = Resolver.newResolverForClassLoader(classLoader);
        String[] classPaths = getClassPaths();
        resolver.setClassPaths(classPaths);

        // Set, return
        return _resolver = resolver;
    }

    /**
     * Returns the ClassLoader.
     */
    public ClassLoader getClassLoader()
    {
        // If already set, just return
        if (_classLoader != null) return _classLoader;

        // Create ClassLoader
        ClassLoader classLoader = createClassLoader();

        // Set, return
        return _classLoader = classLoader;
    }

    /**
     * Creates the ClassLoader.
     */
    protected ClassLoader createClassLoader()
    {
        // If RootProject, return RootProject.ClassLoader
        Project rproj = getRootProject();
        if (rproj != this)
            return rproj.createClassLoader();

        // Get all project ClassPath URLs
        String[] classPaths = getClassPaths();
        URL[] urls = FilePathUtils.getURLs(classPaths);

        // Get System ClassLoader
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader().getParent();

        // Create special URLClassLoader subclass so when debugging SnapCode, we can ignore classes loaded by Project
        ClassLoader urlClassLoader = new URLClassLoader(urls, sysClassLoader);

        // Return
        return urlClassLoader;
    }

    /**
     * Returns whether file is config file.
     */
    protected boolean isConfigFile(WebFile aFile)  { return false; }

    /**
     * Returns the ProjectFiles that manages project files.
     */
    public ProjectFiles getProjectFiles()  { return _projFiles; }

    /**
     * Returns the source directory.
     */
    public WebFile getSourceDir()
    {
        return _projFiles.getSourceDir();
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        return _projFiles.getBuildDir();
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getClassPaths()
    {
        return _projFiles.getClassPaths();
    }

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
     * Returns the class name for given class file.
     */
    public String getClassNameForFile(WebFile aFile)
    {
        return _projFiles.getClassNameForFile(aFile);
    }

    /**
     * The breakpoint list property.
     */
    public BuildIssues getBuildIssues()
    {
        if (_buildIssues != null) return _buildIssues;
        return _buildIssues = new BuildIssues(this);
    }

    /**
     * Returns the breakpoints.
     */
    public Breakpoints getBreakpoints()
    {
        if (_breakpoints != null) return _breakpoints;
        return _breakpoints = new Breakpoints(this);
    }

    /**
     * Watches Project.ProjectConfig for JarPaths change to reset ClassPathInfo.
     */
    private void projConfigDidPropChange(PropChange anEvent)
    {
        String propName = anEvent.getPropName();
        if (propName == ProjectConfig.JarPaths_Prop) {
            _resolver = null;
            _classLoader = null;
        }
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

    /**
     * Returns a temp project.
     */
    public static Project getTempProject()
    {
        WebURL url = WebURL.getURL("/tmp/TempProj");
        WebSite site = url.getAsSite();
        Project proj = Project.getProjectForSite(site);
        if (proj == null)
            proj = new Project(site);

        // Return
        return proj;
    }
}
