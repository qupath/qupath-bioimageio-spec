package qupath.bioimageio.spec;

/**
 * A file description (link and hash). Equivalent to FileDescr
 */
public class FileDescription {
    private final String source;
    private final String hashSha256;

    FileDescription(String source, String hashSha256) {
        this.source = source;
        this.hashSha256 = hashSha256;
    }

    static final FileDescription NULL_FILE = new FileDescription("", "");

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
