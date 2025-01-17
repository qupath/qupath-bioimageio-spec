package qupath.bioimageio.spec.tmp.axes;

/**
 * The type of axis. Batch (b), index (i), channel (c), x, y, z, time (t).
 */
public enum AxisType {
    B("b"),
    I("i"),
    C("c"),
    X("x"),
    Y("y"),
    Z("z"),
    T("t");
    private final String type;

    AxisType(String type) {
        this.type = type;
    }

    static AxisType fromString(String s) {
        return AxisType.valueOf(s.toUpperCase());
    }

    @Override
    public String toString() {
        return type;
    }
}
