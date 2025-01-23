package qupath.bioimageio.spec;

public class FileDescr {
    private final String source;
    private final String hashSHA256;

    FileDescr(String source, String hashSHA256) {
        this.source = source;
        this.hashSHA256 = hashSHA256;
    }

    public String getSource() {
        return source;
    }

    public String getHashSHA256() {
        return hashSHA256;
    }
}
