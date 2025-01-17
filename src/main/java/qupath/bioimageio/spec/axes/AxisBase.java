package qupath.bioimageio.spec.axes;

abstract class AxisBase implements Axis {
    private final String id;
    private final String description;
    AxisBase(String id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public String getID() {
        return this.id;
    }
}
