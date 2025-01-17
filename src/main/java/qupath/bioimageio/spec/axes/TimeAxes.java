package qupath.bioimageio.spec.axes;

import qupath.bioimageio.spec.sizes.Size;
import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.List;

public class TimeAxes {

    abstract static class TimeAxisBase extends AxisBase implements ScaledAxis {
        private final String type = "time";
        private final TimeUnit unit;
        private final double scale;

        TimeAxisBase(String id, String description, TimeUnit unit, double scale) {
            super(id, description);
            this.unit = unit;
            this.scale = scale;
        }
        @Override
        public AxisType getType() {
            return AxisType.T;
        }

        @Override
        public double getScale() {
            return scale;
        }

        public TimeUnit getUnit() {
            return unit;
        }
    }

    static class TimeInputAxis extends TimeAxisBase {
        private final Size size;

        TimeInputAxis(String id, String description, TimeUnit unit, double scale, Size size) {
            super(id, description, unit, scale);
            this.size = size;
        }

        @Override
        public Size getSize() {
            return size;
        }

        @Override
        public void validate(List<? extends BaseTensor> tensors) {
            getSize().validate(tensors);
        }
    }

    static class TimeOutputAxis extends TimeAxisBase {
        private final Size size;

        TimeOutputAxis(String id, String description, TimeUnit unit, double scale, Size size) {
            super(id, description, unit, scale);
            this.size = size;
        }

        @Override
        public Size getSize() {
            return size;
        }

        @Override
        public void validate(List<? extends BaseTensor> tensors) {
            getSize().validate(tensors);
        }
    }

    static class TimeOutputAxisWithHalo extends TimeOutputAxis implements WithHalo {
        private final int halo;

        TimeOutputAxisWithHalo(String id, String description, TimeUnit unit, double scale, Size size, int halo) {
            super(id, description, unit, scale, size);
            this.halo = halo;
        }

        @Override
        public int getHalo() {
            return this.halo;
        }
    }

    public enum TimeUnit {
        ATTOSECOND,
        CENTISECOND,
        DAY,
        DECISECOND,
        EXASECOND,
        FEMTOSECOND,
        GIGASECOND,
        HECTOSECOND,
        HOUR,
        KILOSECOND,
        MEGASECOND,
        MICROSECOND,
        MILLISECOND,
        MINUTE,
        NANOSECOND,
        PETASECOND,
        PICOSECOND,
        SECOND,
        TERASECOND,
        YOCTOSECOND,
        YOTTASECOND,
        ZEPTOSECOND,
        ZETTASECOND
    }

}
