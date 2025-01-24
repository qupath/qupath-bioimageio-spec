package qupath.bioimageio.spec;

/**
 * Model parent. Currently, this provides only an ID and URI. Corresponds to LinkedModel in the 0.4 spec.
 * @author petebankhead
 */
public class ModelParent {

    private String id;
    private String sha256;
    private String uri;

    /**
     * bioimage.io-wide unique resource identifier assigned by bioimage.io; version **un**specific.
     * @return the ID
     */
    public String getID() {
        return id;
    }

    /**
     * Get the SHA256 of... something? Unclear what.
     * @return a SHA256 of unknown provenance.
     */
    public String getSha256() {
        return sha256;
    }

    /**
     * A URI, hopefully to the base path of the model.
     * @return A URI pointing to the model.
     */
    public String getURI() {
        return uri;
    }

    @Override
    public String toString() {
        return "Parent: " + id;
    }

}
