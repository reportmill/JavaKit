/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import snap.util.FileUtils;
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
        String tempProjPath = FileUtils.getTempFile("TempProj").getAbsolutePath();
        WebURL url = WebURL.getURL(tempProjPath);
        WebSite site = url.getAsSite();
        Project proj = Project.getProjectForSite(site);
        if (proj == null)
            proj = new Project(site);

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
        String fileName = "Untitled." + anExt;
        WebFile tempFile = proj.getSourceFile(fileName, false, false);
        if (tempFile == null)
            return proj.getSourceFile(fileName, true, false);

        // Report project source file for "Untitled-X.ext" where X is first unused file name
        for (int i = 1; i < 1000; i++) {
            String fileName2 = "Untitled-" + i + '.' + anExt;
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
    public static WebURL getProjectSourceURLForURL(WebURL aSourceURL)
    {
        // Check for existing project for SourceURL - if found, just return URL
        WebSite sourceSite = aSourceURL.getSite();
        Project proj = Project.getProjectForSite(sourceSite);
        if (proj != null)
            return aSourceURL;

        // Get parent URL and create new project
        WebURL parentDirURL = aSourceURL.getParent();
        WebSite parentDirSite = parentDirURL.getAsSite();
        Project newProj = new Project(parentDirSite);

        // Clear source dir
        ProjectConfig projectConfig = newProj.getProjectConfig();
        projectConfig.setSourcePath("");

        // Create source file for SourceURL file name
        String fileName = aSourceURL.getPathName();
        WebFile sourceFile = newProj.getSourceFile(fileName, true, false);

        // Return source file URL
        return sourceFile.getURL();
    }
}
