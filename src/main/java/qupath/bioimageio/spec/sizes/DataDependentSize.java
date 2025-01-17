package qupath.bioimageio.spec.sizes;


import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.List;

/**
 * A size that is only known after model inference (eg, detecting an unknown number of instances).
 */
class DataDependentSize implements Size {
    private int min = 0;
    private int max = Integer.MAX_VALUE;

    DataDependentSize(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public int getSize() {
        return NO_SIZE;
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
        assert min > 0;
        assert max >= min;
    }
}
