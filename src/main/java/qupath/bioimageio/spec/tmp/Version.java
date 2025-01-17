package qupath.bioimageio.spec.tmp;

public enum Version {
    VERSION_0_4("0.4"),
    VERSION_0_5("0.5");
    private final String version;

    Version(String s) {
        this.version = s;
    }

    public String getVersion() {
        return version;
    }
}
