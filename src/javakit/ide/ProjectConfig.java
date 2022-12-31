package javakit.ide;
import snap.props.PropObject;
import snap.util.ArrayUtils;
import snap.util.StringUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class manages project properties.
 */
public class ProjectConfig extends PropObject {

    // The project
    private Project  _proj;

    // The project source path
    public String  _srcPath;

    // The project build path
    public String  _buildPath;

    // The source paths
    public String[]  _srcPaths;

    // The library paths
    public String[]  _libPaths;

    // The project paths
    public String[]  _projPaths;

    // Constants for ClassPath properties
    public static final String BuildPath_Prop = "BuildPath";
    public static final String SrcPaths_Prop = "SrcPaths";
    public static final String JarPaths_Prop = "JarPaths";

    /**
     * Creates a new ClassPathFile for project.
     */
    public ProjectConfig(Project aProj)
    {
        _proj = aProj;

        // Set defaults
        _srcPath = "src";
        _buildPath = "bin";
        _srcPaths = new String[0];
        _libPaths = new String[0];
        _projPaths = new String[0];
    }

    /**
     * Returns the source path.
     */
    public String getSourcePath()  { return _srcPath; }

    /**
     * Returns the build path.
     */
    public String getBuildPath()  { return _buildPath; }

    /**
     * Sets the build path.
     */
    public void setBuildPath(String aPath)
    {
        // Update ivar
        if (Objects.equals(aPath, getBuildPath())) return;

        String newPath = aPath != null ? getRelativePath(aPath) : null;
        firePropChange(BuildPath_Prop, _buildPath, _buildPath = newPath);
    }

    /**
     * Returns the source paths.
     */
    public String[] getSrcPaths()  { return _srcPaths; }

    /**
     * Adds a source path.
     */
    public void addSrcPath(String aPath)
    {
        // Add XML for path
        String path = getRelativePath(aPath);

        _srcPaths = ArrayUtils.add(_srcPaths, path);

        firePropChange(SrcPaths_Prop, null, path);
    }

    /**
     * Removes a source path.
     */
    public void removeSrcPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(_srcPaths, aPath);
        if (index < 0) return;

        _srcPaths = ArrayUtils.remove(_srcPaths, index);

        // Fire property change
        firePropChange(SrcPaths_Prop, aPath, null);
    }

    /**
     * Returns the paths.
     */
    public String[] getLibPaths()  { return _libPaths; }

    /**
     * Adds a library path.
     */
    public void addLibPath(String aPath)
    {
        // Update paths
        String path = getRelativePath(aPath);

        _libPaths = ArrayUtils.add(_libPaths, path);

        firePropChange(JarPaths_Prop, null, path);
    }

    /**
     * Removes a library path.
     */
    public void removeLibPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(_libPaths, aPath);
        if (index < 0) return;

        _libPaths = ArrayUtils.remove(_libPaths, index);

        firePropChange(JarPaths_Prop, aPath, null);
    }

    /**
     * Returns the project paths path.
     */
    public String[] getProjectPaths()  { return _projPaths; }

    /**
     * Returns the build path as absolute path.
     */
    public String getBuildPathAbsolute()
    {
        String buildPath = getBuildPath();
        String buildPathAbs = getAbsolutePath(buildPath);
        return addDirChar(buildPathAbs);
    }

    /**
     * Returns the library paths as absolute paths.
     */
    public String[] getLibPathsAbsolute()
    {
        // Get LibPaths
        String[] libPaths = getLibPaths();

        // Convert to absolute paths
        String[] absPaths = new String[libPaths.length];
        for (int i = 0; i < libPaths.length; i++) {
            String absPath = getAbsolutePath(libPaths[i]);
            absPaths[i] = addDirChar(absPath);
        }

        // Return
        return absPaths;
    }

    /**
     * Returns an absolute path for given relative path with option to add .
     */
    private String getAbsolutePath(String aPath)
    {
        // If path missing root dir path, add it
        String filePath = aPath;
        if (!filePath.startsWith("/"))
            filePath = getProjRootDirPath() + filePath;

        // Return
        return filePath;
    }

    /**
     * Adds a directory char to end of path if needed.
     */
    private String addDirChar(String aPath)
    {
        if (!StringUtils.endsWithIC(aPath, ".jar") && !StringUtils.endsWithIC(aPath, ".zip") && !aPath.endsWith("/"))
            aPath = aPath + '/';
        return aPath;
    }

    /**
     * Returns a relative path for given path.
     */
    private String getRelativePath(String aPath)
    {
        // Get filePath
        String filePath = aPath;

        // Make sure separator is standard '/'
        if (File.separatorChar != '/')
            filePath = filePath.replace(File.separatorChar, '/');

        // If missing root (already relative), just return
        if (!aPath.startsWith("/"))
            return filePath;

        // If filePath starts with root dir path, strip it
        String rootDirPath = getProjRootDirPath();
        if (filePath.startsWith(rootDirPath))
            filePath = filePath.substring(rootDirPath.length());

        // Return
        return filePath;
    }

    /**
     * Returns the project root path.
     */
    private String getProjRootDirPath()
    {
        // Get project root dir path
        WebSite projSite = _proj.getSite();
        WebFile rootDir = projSite.getRootDir();
        String rootDirPath = rootDir.getJavaFile().getAbsolutePath();

        // Make sure separator is standard '/'
        if (File.separatorChar != '/')
            rootDirPath = rootDirPath.replace(File.separatorChar, '/');

        // Make sure path ends with dir char
        if (!rootDirPath.endsWith("/"))
            rootDirPath = rootDirPath + '/';

        // Make sure path start with dir char
        if (!rootDirPath.startsWith("/"))
            rootDirPath = '/' + rootDirPath;

        // Return
        return rootDirPath;
    }

    /**
     * Standard toString implementation.
     */
    @Override
    public String toStringProps()
    {
        StringBuffer sb = new StringBuffer();
        StringUtils.appendProp(sb, BuildPath_Prop, getBuildPath());
        StringUtils.appendProp(sb, SrcPaths_Prop, Arrays.toString(getSrcPaths()));
        StringUtils.appendProp(sb, JarPaths_Prop, Arrays.toString(getLibPaths()));
        return sb.toString();
    }
}