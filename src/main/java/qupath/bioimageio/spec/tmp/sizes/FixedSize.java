package qupath.bioimageio.spec.tmp.sizes;

import qupath.bioimageio.spec.Utils;
import qupath.bioimageio.spec.tmp.tensor.BaseTensor;

import java.util.List;

public class FixedSize implements Size {
    private final int size;
    public FixedSize(int size) {
        this.size = size;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getTargetSize(int target) {
        return getSize();
    }

    @Override
    public int getStep() {
        return NO_SIZE;
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        // can't validate ints
    }
}
