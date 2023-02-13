/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;

/**
 * Utility methods for Project.
 */
public class ProjectUtils {

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
        WebURL url = WebURL.getURL(tempProjPath);
        WebSite site = url.getAsSite();

        // Get project for site - create if missing
        Project proj = Project.getProjectForSite(site);
        if (proj == null)
            proj = new Project(new WorkSpace(), site);

        // Return
        return proj;
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

        // Get parent URL and create new project
        WebURL parentDirURL = aSourceURL.getParent();
        WebSite parentDirSite = parentDirURL.getAsSite();
        Project newProj = new Project(new WorkSpace(), parentDirSite);

        // Clear source dir
        ProjectConfig projectConfig = newProj.getProjectConfig();
        projectConfig.setSourcePath("");

        // Create source file for SourceURL file name
        String fileName = '/' + aSourceURL.getPathName();
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
        System.out.println("ResolverUtils.getClassPathForClass: Unexpected class url: " + url);
        return null;
    }
}
