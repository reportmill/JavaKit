package javakit.resolver;
import javakit.reflect.Resolver;
import snap.props.PropChange;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * This is a Resolver subclass.
 */
public class Project extends Resolver {

    // The encapsulated data site
    protected WebSite  _site;

    // ProjectConfig
    protected ProjectConfig  _projConfig;

    // ProjectFiles
    protected ProjectFiles  _projFiles;

    // ClassPathInfo
    private ClassPathInfo  _classPathInfo;

    /**
     * Creates a new Project for WebSite.
     */
    protected Project(WebSite aSite)
    {
        // Set site
        setSite(aSite);

        // If site doesn't exist, create root directory, src, bin and default .classpath file
        if (!aSite.getExists()) {
            aSite.getRootDir().save();
            aSite.createFile("/src", true).save();
            aSite.createFile("/bin", true).save();
        }

        // Create/set ProjectFiles
        _projFiles = new ProjectFiles(this);
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
     * Returns whether file is config file.
     */
    protected boolean isConfigFile(WebFile aFile)  { return false; }

    /**
     * Returns the ProjectFiles that manages project files.
     */
    public ProjectFiles getProjectFiles()  { return _projFiles; }

    /**
     * Returns ClassPathInfo.
     */
    public ClassPathInfo getClassPathInfo()
    {
        // If already set, just return
        if (_classPathInfo != null) return _classPathInfo;

        // Create
        ClassPathInfo classPathInfo = new ClassPathInfo(this);

        // Set/return
        return _classPathInfo = classPathInfo;
    }

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
        return _site.getFile(aPath);
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
     * Watches Project.ProjectConfig for JarPaths change to reset ClassPathInfo.
     */
    private void projConfigDidPropChange(PropChange anEvent)
    {
        if (anEvent.getPropertyName() == ProjectConfig.JarPaths_Prop)
            _classPathInfo = null;
    }

    /**
     * Returns the project for a given site.
     */
    public static Project get(WebFile aFile)
    {
        WebSite site = aFile.getSite();
        return get(site);
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized Project get(WebSite aSite)
    {
        return get(aSite, false);
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized Project get(WebSite aSite, boolean doCreate)
    {
        Project proj = (Project) aSite.getProp(Project.class.getSimpleName());
        if (proj == null && doCreate)
            proj = new Project(aSite);

        // Return
        return proj;
    }
}
