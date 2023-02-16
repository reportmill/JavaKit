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

    // The library paths
    public String[]  _libPaths;

    // The project paths
    public String[]  _projPaths;

    // Constants for ClassPath properties
    public static final String SourcePath_Prop = "SourcePaths";
    public static final String BuildPath_Prop = "BuildPath";
    public static final String LibPaths_Prop = "LibPaths";
    public static final String ProjectPaths_Prop = "ProjectPaths";

    /**
     * Creates a new ClassPathFile for project.
     */
    public ProjectConfig(Project aProj)
    {
        _proj = aProj;

        // Set defaults
        _srcPath = "src";
        _buildPath = "bin";
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
        // If already set, just return
        if (Objects.equals(aPath, _srcPath)) return;

        // Set, firePropChange
        String newPath = aPath != null ? ProjectUtils.getRelativePath(_proj, aPath) : null;
        firePropChange(SourcePath_Prop, _srcPath, _srcPath = newPath);
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
        // If already set, just return
        if (Objects.equals(aPath, _buildPath)) return;

        // Set, firePropChange
        String newPath = aPath != null ? ProjectUtils.getRelativePath(_proj, aPath) : null;
        firePropChange(BuildPath_Prop, _buildPath, _buildPath = newPath);
    }

    /**
     * Returns the library paths.
     */
    public String[] getLibPaths()  { return _libPaths; }

    /**
     * Sets the library paths.
     */
    public void setLibPaths(String[] libPaths)
    {
        // Convert to relative
        String[] relativeLibPaths = ArrayUtils.map(libPaths, path -> ProjectUtils.getRelativePath(_proj, path), String.class);

        // If already set, just return
        if (Arrays.equals(relativeLibPaths, _libPaths)) return;

        // Set, fire prop change
        firePropChange(LibPaths_Prop, _libPaths, _libPaths = relativeLibPaths);
    }

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
        int index = _libPaths.length - 1;
        firePropChange(LibPaths_Prop, null, libPath, index);
    }

    /**
     * Removes a library path.
     */
    public void removeLibPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(_libPaths, aPath);
        if (index < 0)
            return;

        // Remove path
        _libPaths = ArrayUtils.remove(_libPaths, index);

        // Fire prop change
        firePropChange(LibPaths_Prop, aPath, null, index);
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
     * Returns the project paths.
     */
    public String[] getProjectPaths()  { return _projPaths; }

    /**
     * Sets the project paths.
     */
    public void setProjectPaths(String[] projectPaths)
    {
        // Convert to relative
        String[] relativeProjPaths = ArrayUtils.map(projectPaths, path -> ProjectUtils.getRelativePath(_proj, path), String.class);

        // If already set, just return
        if (Arrays.equals(relativeProjPaths, _projPaths)) return;

        // Set, fire prop change
        firePropChange(ProjectPaths_Prop, _projPaths, _projPaths = relativeProjPaths);
    }

    /**
     * Adds a project path.
     */
    public void addProjectPath(String aPath)
    {
        // Add XML for path
        String path = ProjectUtils.getRelativePath(_proj, aPath);

        // Add to array
        _projPaths = ArrayUtils.add(_projPaths, path);

        // Fire property change
        int index = _projPaths.length - 1;
        firePropChange(ProjectPaths_Prop, null, path, index);
    }

    /**
     * Removes a project path.
     */
    public void removeProjectPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(_projPaths, aPath);
        if (index < 0)
            return;

        // Remove from array
        _projPaths = ArrayUtils.remove(_projPaths, index);

        // Fire property change
        firePropChange(ProjectPaths_Prop, aPath, null, index);
    }

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
        StringUtils.appendProp(sb, SourcePath_Prop, getSourcePath());
        StringUtils.appendProp(sb, BuildPath_Prop, getBuildPath());
        StringUtils.appendProp(sb, LibPaths_Prop, Arrays.toString(getLibPaths()));
        StringUtils.appendProp(sb, ProjectPaths_Prop, Arrays.toString(getProjectPaths()));
        return sb.toString();
    }
}