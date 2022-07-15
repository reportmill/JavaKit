package javakit.resolver;

import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.props.PropChangeSupport;
import snap.util.ArrayUtils;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.util.XMLElement;
import snap.web.WebFile;
import snap.web.WebSite;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to read/edit the .classpath file.
 */
public class ClassPath {

    // The project
    Project _proj;

    // The site
    WebSite _site;

    // The web file
    WebFile _file;

    // The XML element
    XMLElement _xml;

    // The project source and build paths
    String _srcPath, _bldPath;

    // The project source and build directories
    WebFile _srcDir, _bldDir;

    // The paths
    String _srcPaths[], _libPaths[];

    // The project paths
    String _projPaths[];

    // The PropChangeSupport
    PropChangeSupport _pcs = PropChangeSupport.EMPTY;

    // Constants for ClassPath properties
    public static final String SrcPaths_Prop = "SrcPaths";
    public static final String JarPaths_Prop = "JarPaths";

    /**
     * Creates a new ClassPathFile for project.
     */
    public ClassPath(Project aProj)
    {
        _proj = aProj;
        _site = aProj.getSite();
    }

    /**
     * Returns the file.
     */
    public WebFile getFile()
    {
        return _file != null ? _file : (_file = getFileImpl());
    }

    /**
     * Returns the file.
     */
    protected WebFile getFileImpl()
    {
        WebFile file = _site.getFile(".classpath");
        if (file == null) file = _site.createFile(".classpath", false);
        return file;
    }

    /**
     * Returns the XML for file.
     */
    public XMLElement getXML()
    {
        return _xml != null ? _xml : (_xml = createXML());
    }

    /**
     * Creates the XML for file.
     */
    protected XMLElement createXML()
    {
        WebFile file = getFile();
        if (file != null && file.getExists())
            return XMLElement.readFromXMLSource(file);
        XMLElement xml = new XMLElement("classpath");
        return xml;
    }

    /**
     * Returns the source path.
     */
    public String getSourcePath()
    {
        // If already set, just return
        if (_srcPath != null) return _srcPath;

        // Get source path from src classpathentry
        XMLElement xmls[] = getSourcePathXMLs();
        XMLElement xml = xmls.length > 0 ? xmls[0] : null;
        String path = xml != null ? xml.getAttributeValue("path") : null;

        // If no path and /src exists, use it
        if (path == null && _site.getFile("/src") != null) path = "src";
        return _srcPath = path;
    }

    /**
     * Sets the source path.
     */
    public void setSourcePath(String aPath)
    {
        // Update ivar
        if (SnapUtils.equals(aPath, getSourcePath())) return;
        _srcPath = aPath != null && aPath.length() > 0 ? getRelativePath(aPath) : null;
        _srcDir = null;

        // Update XML
        XMLElement xmls[] = getSourcePathXMLs();
        XMLElement xml = xmls.length > 0 ? xmls[0] : null;
        if (xml == null) {
            xml = new XMLElement("classpathentry");
            xml.add("kind", "src");
            getXML().add(xml);
        }
        if (_srcPath != null) xml.add("path", _srcPath);
        else getXML().removeElement(xml);

        // Save file
        try {
            writeFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the project paths path.
     */
    public String[] getProjectPaths()
    {
        // If already set, just return
        if (_projPaths != null) return _projPaths;

        // Load from ProjectXMLs
        List<String> paths = new ArrayList();
        for (XMLElement xml : getProjectPathXMLs()) paths.add(xml.getAttributeValue("path"));
        return _projPaths = paths.toArray(new String[paths.size()]);
    }

    /**
     * Returns the build path.
     */
    public String getBuildPath()
    {
        // If already set, just return
        if (_bldPath != null) return _bldPath;

        // Get source path from output classpathentry
        XMLElement xml = getBuildPathXML();
        String path = xml != null ? xml.getAttributeValue("path") : null;

        // If path not set, use bin
        if (path == null) path = "bin";
        return _bldPath = path;
    }

    /**
     * Sets the build path.
     */
    public void setBuildPath(String aPath)
    {
        // Update ivar
        if (SnapUtils.equals(aPath, getBuildPath())) return;
        _bldPath = aPath != null ? getRelativePath(aPath) : null;
        _bldDir = null;

        // Update XML
        XMLElement xml = getBuildPathXML();
        if (xml == null) {
            xml = new XMLElement("classpathentry");
            xml.add("kind", "output");
            getXML().add(xml);
        }
        if (_bldPath != null) xml.add("path", _bldPath);
        else getXML().removeElement(xml);

        // Save file
        try {
            writeFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the source root directory.
     */
    public WebFile getSourceDir()
    {
        // If already set, just return
        if (_srcDir != null) return _srcDir;

        // Get from SourcePath and site
        String path = getSourcePath();
        if (path != null && !path.startsWith("/")) path = '/' + path;
        WebFile srcDir = path != null ? _site.getFile(path) : _site.getRootDir();
        if (srcDir == null) srcDir = _site.createFile(path, true);
        return _srcDir = srcDir;
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        // If already set, just return
        if (_bldDir != null) return _bldDir;

        // Get from BuildPath and site
        String path = getBuildPath();
        if (path != null && !path.startsWith("/")) path = '/' + path;
        WebFile bldDir = path != null ? _site.getFile(path) : _site.getRootDir();
        if (bldDir == null) bldDir = _site.createFile(path, true);
        return _bldDir = bldDir;
    }

    /**
     * Returns the source paths.
     */
    public String[] getSrcPaths()
    {
        // If already set, just return
        if (_srcPaths != null) return _srcPaths;

        // Load from lib elements
        List<String> paths = new ArrayList();
        for (XMLElement xml : getSrcXMLs()) {
            String path = xml.getAttributeValue("path");
            paths.add(path);
        }
        return _srcPaths = paths.toArray(new String[paths.size()]);
    }

    /**
     * Adds a source path.
     */
    public void addSrcPath(String aPath)
    {
        // Add XML for path
        String path = getRelativePath(aPath);
        XMLElement xml = new XMLElement("classpathentry");
        xml.add("kind", "src");
        xml.add("path", path);
        getXML().add(xml);

        // Save file
        try {
            writeFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Clear cached paths and Fire property change
        _srcPaths = null;
        _projPaths = null;
        firePropChange(SrcPaths_Prop, null, path);
    }

    /**
     * Removes a source path.
     */
    public void removeSrcPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(getSrcPaths(), aPath);
        _srcPaths = null;
        _projPaths = null;

        // Remove XML
        for (int i = 0, iMax = getXML().getElementCount(); i < iMax; i++) {
            XMLElement xml = getXML().getElement(i);
            if ("src".equals(xml.getAttributeValue("kind")) && aPath.equals(xml.getAttributeValue("path"))) {
                getXML().removeElement(i);
                break;
            }
        }

        // Save file
        try {
            writeFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Fire property change
        firePropChange(SrcPaths_Prop, aPath, null);
    }

    /**
     * Returns the paths.
     */
    public String[] getLibPaths()
    {
        // If already set, just return
        if (_libPaths != null) return _libPaths;

        // Load from lib elements
        List<String> paths = new ArrayList();
        for (XMLElement xml : getLibXMLs()) {
            String path = xml.getAttributeValue("path");
            paths.add(path);
        }
        return _libPaths = paths.toArray(new String[paths.size()]);
    }

    /**
     * Adds a library path.
     */
    public void addLibPath(String aPath)
    {
        // Update paths
        String path = getRelativePath(aPath);
        _libPaths = ArrayUtils.add(_libPaths, path);

        // Add XML for path
        XMLElement xml = new XMLElement("classpathentry");
        xml.add("kind", "lib");
        xml.add("path", path);
        getXML().add(xml);

        // Save file
        try {
            writeFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Fire property change
        firePropChange(JarPaths_Prop, null, path);
    }

    /**
     * Removes a library path.
     */
    public void removeLibPath(String aPath)
    {
        // Update paths
        int index = ArrayUtils.indexOf(_libPaths, aPath);
        if (index >= 0) _libPaths = ArrayUtils.remove(_libPaths, index);

        // Remove XML
        for (int i = 0, iMax = getXML().getElementCount(); i < iMax; i++) {
            XMLElement xml = getXML().getElement(i);
            if ("lib".equals(xml.getAttributeValue("kind")) && aPath.equals(xml.getAttributeValue("path"))) {
                getXML().removeElement(i);
                break;
            }
        }

        // Save file
        try {
            writeFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Fire property change
        firePropChange(JarPaths_Prop, aPath, null);
    }

    /**
     * Returns the source path as absolute path.
     */
    public String getSourcePathAbsolute()
    {
        return getAbsolutePath(getSourcePath());
    }

    /**
     * Returns the build path as absolute path.
     */
    public String getBuildPathAbsolute()
    {
        return addDirChar(getAbsolutePath(getBuildPath()));
    }

    /**
     * Returns the library paths as absolute paths.
     */
    public String[] getLibPathsAbsolute()
    {
        String lpaths[] = getLibPaths(), apaths[] = new String[lpaths.length];
        for (int i = 0; i < lpaths.length; i++) apaths[i] = addDirChar(getAbsolutePath(lpaths[i]));
        return apaths;
    }

    /**
     * Returns an absolute path for given relative path with option to add .
     */
    private String getAbsolutePath(String aPath)
    {
        String path = aPath;
        if (!path.startsWith("/"))
            path = getProjRootDirPath() + path;
        return path;
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
        String path = aPath;
        if (File.separatorChar != '/') path = path.replace(File.separatorChar, '/');
        if (!aPath.startsWith("/")) return path;
        String root = getProjRootDirPath();
        if (path.startsWith(root)) path = path.substring(root.length());
        return path;
    }

    /**
     * Returns the project root path.
     */
    private String getProjRootDirPath()
    {
        String root = _site.getRootDir().getJavaFile().getAbsolutePath();
        if (File.separatorChar != '/') root = root.replace(File.separatorChar, '/');
        if (!root.endsWith("/")) root = root + '/';
        if (!root.startsWith("/")) root = '/' + root;
        return root;
    }

    /**
     * Returns the src classpathentry xmls.
     */
    private XMLElement[] getSrcXMLs()
    {
        List<XMLElement> paths = new ArrayList();
        for (XMLElement x : getXML().getElements()) if ("src".equals(x.getAttributeValue("kind"))) paths.add(x);
        return paths.toArray(new XMLElement[paths.size()]);
    }

    /**
     * Returns the lib classpathentry xmls.
     */
    private XMLElement[] getLibXMLs()
    {
        List<XMLElement> paths = new ArrayList();
        for (XMLElement x : getXML().getElements()) if ("lib".equals(x.getAttributeValue("kind"))) paths.add(x);
        return paths.toArray(new XMLElement[paths.size()]);
    }

    /**
     * Returns the src classpathentry xmls that are in project directory.
     */
    private XMLElement[] getSourcePathXMLs()
    {
        List<XMLElement> paths = new ArrayList();
        for (XMLElement src : getSrcXMLs()) {
            String path = src.getAttributeValue("path");
            if (path != null && !path.startsWith("/")) paths.add(src);
        }
        return paths.toArray(new XMLElement[paths.size()]);
    }

    /**
     * Returns the project classpathentry xmls that are outside project directory.
     */
    private XMLElement[] getProjectPathXMLs()
    {
        List<XMLElement> paths = new ArrayList();
        for (XMLElement src : getSrcXMLs()) {
            String path = src.getAttributeValue("path");
            if (path != null && path.startsWith("/")) paths.add(src);
        }
        return paths.toArray(new XMLElement[paths.size()]);
    }

    /**
     * Returns the output classpathentry xml.
     */
    private XMLElement getBuildPathXML()
    {
        for (XMLElement child : getXML().getElements())
            if ("output".equals(child.getAttributeValue("kind")))
                return child;
        return null;
    }

    /**
     * Reads the class path from .classpath file.
     */
    public void readFile()
    {
        _xml = null;
        _srcPaths = _libPaths = null;
        _srcPath = _bldPath = null;
        _srcDir = _bldDir = null;
        if (_file != null) _file.reload();
        _file = null;
    }

    /**
     * Saves the ClassPath to .classpath file.
     */
    public void writeFile() throws Exception
    {
        getFile().setBytes(getXML().getBytes());
        getFile().save();
    }

    /**
     * Add listener.
     */
    public void addPropChangeListener(PropChangeListener aLsnr)
    {
        if (_pcs == PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
        _pcs.addPropChangeListener(aLsnr);
    }

    /**
     * Remove listener.
     */
    public void removePropChangeListener(PropChangeListener aLsnr)
    {
        _pcs.removePropChangeListener(aLsnr);
    }

    /**
     * Fires a property change for given property name, old value, new value and index.
     */
    protected void firePropChange(String aProp, Object oldVal, Object newVal)
    {
        if (!_pcs.hasListener(aProp)) return;
        PropChange pc = new PropChange(this, aProp, oldVal, newVal);
        _pcs.firePropChange(pc);
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getXML().toString();
    }

    /**
     * Creates the ClassPath file for given project.
     */
    public static void createFile(Project aProj)
    {
        StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<classpath>\n");
        sb.append("\t<classpathentry kind=\"src\" path=\"src\"/>\n");
        sb.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n");
        sb.append("\t<classpathentry kind=\"output\" path=\"bin\"/>\n");
        sb.append("</classpath>\n");
        WebSite site = aProj.getSite();
        WebFile file = site.createFile(".classpath", false);
        file.setText(sb.toString());
        file.save();
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized ClassPath get(Project aProj)
    {
        WebSite site = aProj.getSite();
        ClassPath cpf = (ClassPath) site.getProp("snap.project.ClassPathFile");
        if (cpf == null) site.setProp("snap.project.ClassPathFile", cpf = new ClassPath(aProj));
        return cpf;
    }

}