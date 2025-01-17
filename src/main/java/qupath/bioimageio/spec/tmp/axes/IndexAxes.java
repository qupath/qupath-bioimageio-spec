package qupath.bioimageio.spec.tmp.axes;

import qupath.bioimageio.spec.Utils;
import qupath.bioimageio.spec.tmp.sizes.Size;
import qupath.bioimageio.spec.tmp.tensor.BaseTensor;

import java.util.List;

public class IndexAxes {
    abstract static class IndexAxisBase extends AxisBase implements ScaledAxis {
        private final double scale = 1.0;
        private final String unit = null;

        IndexAxisBase(String id, String description) {
            super(id, description);
        }

        @Override
        public AxisType getType() {
            return AxisType.I;
        }

        @Override
        public double getScale() {
            return 1;
        }
    }

    static class IndexInputAxis extends IndexAxisBase {
        private final Size size;
        private final boolean concatenable = false;

        IndexInputAxis(String id, String description, Size size) {
            super(id, description);
            this.size = size;
        }
        @Override
        public Size getSize() {
            return this.size;
        }

        @Override
        public void validate(List<? extends BaseTensor> tensors) {
            size.validate(tensors);
        }
    }

    static class IndexOutputAxis extends IndexAxisBase {
        private Size size;

        IndexOutputAxis(String id, String description) {
            super(id, description);
        }

        @Override
        public Size getSize() {
            return size;
        }

        @Override
        public void validate(List<? extends BaseTensor> tensors) {
            size.validate(tensors);
        }
    }
}
