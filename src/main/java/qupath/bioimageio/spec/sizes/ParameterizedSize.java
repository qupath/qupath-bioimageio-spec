package qupath.bioimageio.spec.sizes;

import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.List;

/**
 * Describes a range of valid tensor axis sizes as `size = min + n*step`.
 */
class ParameterizedSize implements Size {
    private final int min;
    private final int step;

    ParameterizedSize(int min, int step) {
        this.min = min;
        this.step = step;
    }

    @Override
    public int getSize() {
        return getTargetSize(1);
    }

    @Override
    public int getTargetSize(int target) {
        if (target < 0 || step <= 0)
            return min;
        else
            return min + (int)Math.round((target - min)/(double)step) * step;
    }
    @Override
    public int getStep() {
        return step;
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        assert min >= 0;
        assert step >= 0;
    }
}
