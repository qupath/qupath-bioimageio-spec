package qupath.bioimageio.spec;

/**
 * A file description (link and hash). Equivalent to FileDescr
 * @param source the file path/URL
 * @param hashSha256 the SHA 256 hash of the file contents
 */
public record FileDescription(String source, String hashSha256) {

    static final FileDescription NULL_FILE = new FileDescription("", "");

}
