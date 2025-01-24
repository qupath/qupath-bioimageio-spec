package qupath.bioimageio.spec;

/**
 * A file description (link and hash).
 */
public class FileDescr {
    private final String source;
    private final String hashSha256;

    FileDescr(String source, String hashSha256) {
        this.source = source;
        this.hashSha256 = hashSha256;
    }

    /**
     * Get the file path/URL
     * @return the file path/URL
     */
    public String getSource() {
        return source;
    }

    /**
     * Get the SHA256 hash of the file contents
     * @return the SHA256 hash
     */
    public String getHashSha256() {
        return hashSha256;
    }
}
