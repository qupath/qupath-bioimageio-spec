package qupath.bioimageio.spec.axes;

import qupath.bioimageio.spec.sizes.Size;
import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.List;

/**
 * Simple class to hold the old "bcyx" format from 0.4
 */
class CharAxis implements Axis {
    private final char axis;

    CharAxis(char c) {
        this.axis = c;
    }

    @Override
    public AxisType getType() {
        return AxisType.valueOf(String.valueOf(axis).toUpperCase());
    }

    @Override
    public Size getSize() {
        throw new UnsupportedOperationException("Cannot get Size of legacy/char axes");
    }

    @Override
    public String getID() {
        return "";
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        // can't validate char axes, these are validated at tensor level
    }
}
