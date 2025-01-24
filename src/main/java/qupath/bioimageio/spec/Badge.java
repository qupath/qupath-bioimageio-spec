package qupath.bioimageio.spec;

/**
 * Model or repository badge.
 */
public class Badge {

    private String label;
    private String icon;
    private String url;

    /**
     * Badge label to display on hover
     * @return The label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Badge icon
     * @return a path or URL to an icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Target URL
     * @return the target of the badge if clicked
     */
    public String getURL() {
        return url;
    }

}

