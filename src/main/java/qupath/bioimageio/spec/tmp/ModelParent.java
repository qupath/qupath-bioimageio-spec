package qupath.bioimageio.spec.tmp;

/**
 * Model parent. Currently, this provides only an ID and URI.
 * @author petebankhead
 *
 */
public class ModelParent {

    private String id;
    private String sha256;
    private String uri;

    public String getID() {
        return id;
    }

    public String getSha256() {
        return sha256;
    }

    public String getURI() {
        return uri;
    }

    @Override
    public String toString() {
        return "Parent: " + id;
    }

}
