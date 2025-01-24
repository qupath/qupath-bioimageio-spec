package qupath.bioimageio.spec;

/**
 * Citation entry.
 */
public class CiteEntry {

    private String text;
    private String doi;
    private String url;

    /**
     * Free text description.
     * @return The description.
     */
    public String getText() {
        return text;
    }

    /**
     * A digital object identifier (DOI) is the prefered citation reference.
     * See <a href="https://www.doi.org/">doi.org</a> for details. (alternatively specify `url`)
     * @return The DOI.
     */
    public String getDOI() {
        return doi;
    }

    /**
     * URL to cite (preferably specify a `doi` instead)
     * @return The URL if present.
     */
    public String getURL() {
        return url;
    }

}

