package javakit.resolver;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * This class manages files for a project.
 */
public class ProjectFiles {

    // The Project
    private Project  _proj;

    // The Project config
    private ProjectConfig  _projConfig;

    // The project source directory
    protected WebFile  _srcDir;

    // The project build directory
    protected WebFile  _buildDir;

    /**
     * Constructor.
     */
    public ProjectFiles(Project aProject)
    {
        _proj = aProject;
        _projConfig = aProject.getProjectConfig();
    }

    /**
     * Returns the source root directory.
     */
    public WebFile getSourceDir()
    {
        // If already set, just return
        if (_srcDir != null) return _srcDir;

        // Get SourcePath
        String path = _projConfig.getSourcePath();
        if (path != null && !path.startsWith("/"))
            path = '/' + path;

        // Get dir file from Project.Site
        WebSite projSite = _proj.getSite();
        WebFile srcDir = path != null ? projSite.getFile(path) : projSite.getRootDir();
        if (srcDir == null)
            srcDir = projSite.createFile(path, true);

        // Set/return
        return _srcDir = srcDir;
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        // If already set, just return
        if (_buildDir != null) return _buildDir;

        // Get from BuildPath and site
        String path = _projConfig.getBuildPath();
        if (path != null && !path.startsWith("/"))
            path = '/' + path;

        // Get dir file from Project.Site
        WebSite projSite = _proj.getSite();
        WebFile bldDir = path != null ? projSite.getFile(path) : projSite.getRootDir();
        if (bldDir == null)
            bldDir = projSite.createFile(path, true);

        // Set/return
        return _buildDir = bldDir;
    }

}
