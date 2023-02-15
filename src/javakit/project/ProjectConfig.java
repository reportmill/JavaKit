package javakit.project;
import snap.props.PropObject;
import snap.util.ArrayUtils;
import snap.util.StringUtils;
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
     * Sets the source path.
     */
    public void setSourcePath(String aPath)
    {
        // Update ivar
        if (Objects.equals(aPath, _srcPath)) return;

        // Set, firePropChange
        String newPath = aPath != null ? ProjectUtils.getRelativePath(_proj, aPath) : null;
        firePropChange(SrcPaths_Prop, _srcPath, _srcPath = newPath);
    }

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

        // Set, firePropChange
        String newPath = aPath != null ? ProjectUtils.getRelativePath(_proj, aPath) : null;
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
        String path = ProjectUtils.getRelativePath(_proj, aPath);

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
        // Get relative path if inside project
        String libPath = ProjectUtils.getRelativePath(_proj, aPath);

        // If already set, just return
        if (ArrayUtils.contains(_libPaths, libPath))
            return;

        // Add path
        _libPaths = ArrayUtils.add(_libPaths, libPath);

        // Fire prop change
        firePropChange(JarPaths_Prop, null, libPath);
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
     * Adds a class path for jar containing given class.
     */
    public void addLibPathForClass(Class<?> aClass)
    {
        String classPath = ProjectUtils.getClassPathForClass(aClass);
        if (classPath != null)
            addLibPath(classPath);
        else System.out.println("ProjectConfig.addLibPathForClass: Couldn't find path for class: " + aClass.getName());
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
        return ProjectUtils.getAbsolutePath(_proj, buildPath, true);
    }

    /**
     * Returns the library paths as absolute paths.
     */
    public String[] getLibPathsAbsolute()
    {
        String[] libPaths = getLibPaths();
        String[] absPaths = ArrayUtils.map(libPaths, path -> ProjectUtils.getAbsolutePath(_proj, path, true), String.class);
        return absPaths;
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