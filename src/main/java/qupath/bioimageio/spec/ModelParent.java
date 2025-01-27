package qupath.bioimageio.spec;

/**
 * Model parent. Currently, this provides only an ID and URI. Corresponds to LinkedModel in the 0.4 spec.
 * @author petebankhead
 * @param id bioimage.io-wide unique resource identifier assigned by bioimage.io; version **un**specific.
 * @param sha256 The SHA256 of something TODO: clarify what
 * @param uri A URI, hopefully to the base path of the model.
 */
public record ModelParent(String id, String sha256, String uri) {

    @Override
    public String toString() {
        return "Parent: " + id;
    }

}
