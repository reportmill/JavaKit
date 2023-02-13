package javakit.project;
import snap.web.WebSite;

/**
 * This class manages working with a set of one or more projects.
 */
public class Pod {

    // The projects that this pod manages


    /**
     * Constructor.
     */
    public Pod()
    {
        super();
    }

    /**
     * Returns a project for given site.
     */
    public Project getProjectForSite(WebSite aSite)
    {
        Project proj = Project.getProjectForSite(aSite);
        if (proj == null)
            proj = new Project(aSite);
        return proj;
    }
}
