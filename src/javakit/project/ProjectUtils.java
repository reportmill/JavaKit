/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;

import java.io.File;

/**
 * Utility methods for Project.
 */
public class ProjectUtils {

    /**
     * Returns the project root path.
     */
    public static String getRootDirPath(Project aProj)
    {
        // If Project not local file, return null
        WebSite projSite = aProj.getSite();
        WebURL projURL = projSite.getURL();
        String scheme = projURL.getScheme();
        if (!(scheme.equals("file") || scheme.equals("local")))
            return null;

        // Get project root dir path
        WebFile rootDir = projSite.getRootDir();
        File rootDirFile = rootDir.getJavaFile();
        String rootDirPath = rootDirFile.getAbsolutePath();

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
     * Returns an absolute path for given relative path, with option to add trailing dir char ('/').
     */
    public static String getAbsolutePath(Project aProj, String aPath, boolean isDir)
    {
        // If path missing root dir path, add it
        String absPath = aPath;
        if (!absPath.startsWith("/")) {
            String rootPath = getRootDirPath(aProj);
            if (rootPath != null)
                absPath = rootPath + absPath;
            else System.err.println("ProjectUtils.getAbsolutePath: Can't find project root path");
        }

        // Handle dir (except .jar/.zip)
        if (isDir) {
            boolean exclude = StringUtils.endsWithIC(aPath, ".jar") || StringUtils.endsWithIC(aPath, ".zip") || aPath.endsWith("/");
            if (!exclude)
                absPath += '/';
        }

        // Return
        return absPath;
    }

    /**
     * Returns a relative path for given path.
     */
    public static String getRelativePath(Project aProj, String aPath)
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
        String rootDirPath = getRootDirPath(aProj);
        if (rootDirPath != null && filePath.startsWith(rootDirPath))
            filePath = filePath.substring(rootDirPath.length());

        // Return
        return filePath;
    }

    /**
     * Returns a temp project.
     */
    public static Project getTempProject()
    {
        // Get path to temp dir named TempProj
        String tempProjPath = FileUtils.getTempFile("TempProj").getAbsolutePath();
        if (SnapUtils.isMac)
            tempProjPath = "/tmp/TempProj";

        // Get URL and Site for TempProjPath
        WebURL tempProjURL = WebURL.getURL(tempProjPath);
        WebSite tempProjSite = tempProjURL.getAsSite();

        // Get project for site - create if missing
        Project tempProj = Project.getProjectForSite(tempProjSite);
        if (tempProj == null) {
            Workspace newWorkspace = new Workspace();
            tempProj = newWorkspace.addProjectForSite(tempProjSite);
        }

        // Return
        return tempProj;
    }

    /**
     * Deletes the temp project.
     */
    public static void deleteTempProject()
    {
        // Get path to temp dir named TempProj
        String tempProjPath = FileUtils.getTempFile("TempProj").getAbsolutePath();
        if (SnapUtils.isMac)
            tempProjPath = "/tmp/TempProj";

        // Get URL and Site for TempProjPath
        WebURL tempProjURL = WebURL.getURL(tempProjPath);
        WebFile tempProjFile = tempProjURL.getFile();
        if (tempProjFile != null)
            tempProjFile.delete();
    }

    /**
     * Returns a temp source file for given project and extension. If null project, uses TempProject.
     */
    public static WebFile getTempSourceFile(Project aProj, String anExt)
    {
        // Get project - if given null project, use TempProject
        Project proj = aProj != null ? aProj : getTempProject();

        // Return project source file for "Untitled.ext", if not present
        String fileName = "/Untitled." + anExt;
        WebFile tempFile = proj.getSourceFile(fileName, false, false);
        if (tempFile == null)
            return proj.getSourceFile(fileName, true, false);

        // Report project source file for "Untitled-X.ext" where X is first unused file name
        for (int i = 1; i < 1000; i++) {
            String fileName2 = "/Untitled-" + i + '.' + anExt;
            tempFile = proj.getSourceFile(fileName2, false, false);
            if (tempFile == null)
                return proj.getSourceFile(fileName2, true, false);
        }

        // Should never get here
        throw new RuntimeException("ProjectUtils.getTempSourceFile: What is your deal with temp files?");
    }

    /**
     * Returns a URL to the project source file implied by given URL.
     * If URL is null, gets temp project and creates temp source file.
     * If URL site doesn't have project, creates project for URL parent and returns source file URL.
     */
    public static WebFile getProjectSourceFileForURL(WebURL aSourceURL)
    {
        // Check for existing project for SourceURL - if found, just return URL
        WebSite sourceSite = aSourceURL.getSite();
        Project proj = Project.getProjectForSite(sourceSite);
        if (proj != null) {
            String path = aSourceURL.getPath();
            return proj.getSourceFile(path, true, false);
        }

        // Create new workspace
        Workspace newWorkspace = new Workspace();

        // Get parent URL and create new project
        WebURL parentDirURL = aSourceURL.getParent();
        WebSite parentDirSite = parentDirURL.getAsSite();
        Project newProj = newWorkspace.addProjectForSite(parentDirSite);

        // Clear source dir
        ProjectConfig projectConfig = newProj.getProjectConfig();
        projectConfig.setSourcePath("");

        // Create source file for SourceURL file name
        String fileName = '/' + aSourceURL.getFilename();
        WebFile sourceFile = newProj.getSourceFile(fileName, true, false);

        // Return source file URL
        return sourceFile;
    }

    /**
     * Returns a class path for given class.
     */
    public static String getClassPathForClass(Class<?> aClass)
    {
        // Get URL and Site
        WebURL url = WebURL.getURL(aClass);

        // If URL string has separator, use site
        String urlString = url.getString();
        if (urlString.contains("!/")) {
            WebSite site = url.getSite();
            return site.getPath();
        }

        // Otherwise, trim className from path
        String path = url.getPath();
        int classNameLen = aClass.getName().length() + ".class".length() + 1;
        if (path.length() > classNameLen)
            return path.substring(0, path.length() - classNameLen);

        // Express concern and return null
        System.out.println("ProjectUtils.getClassPathForClass: Unexpected class url: " + url);
        return null;
    }
}
