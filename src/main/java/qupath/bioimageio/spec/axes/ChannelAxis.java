package qupath.bioimageio.spec.axes;

import qupath.bioimageio.spec.sizes.FixedSize;
import qupath.bioimageio.spec.sizes.Size;
import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.List;

class ChannelAxis extends AxisBase implements ScaledAxis {
    private final List<String> channel_names;

    ChannelAxis(String id, String description, List<String> channel_names) {
        super(id, description);
        this.channel_names = List.copyOf(channel_names);
    }

    @Override
    public Size getSize() {
        return new FixedSize(channel_names.size());
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        // fixed size based on list of channels
    }

    @Override
    public AxisType getType() {
        return AxisType.C;
    }

    @Override
    public double getScale() {
        return 1;
    }
}

