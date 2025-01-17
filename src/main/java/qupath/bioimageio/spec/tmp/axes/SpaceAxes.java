package qupath.bioimageio.spec.tmp.axes;

import qupath.bioimageio.spec.tmp.sizes.ReferencedSize;
import qupath.bioimageio.spec.tmp.sizes.Size;
import qupath.bioimageio.spec.tmp.tensor.BaseTensor;

import java.util.List;

public class SpaceAxes {

    public enum SpaceUnit {
        ATTOMETER("attometer"),
        ANGSTROM("angstrom"),
        CENTIMETER("centimeter"),
        DECIMETER("decimeter"),
        EXAMETER("exameter"),
        FEMTOMETER("femtometer"),
        FOOT("foot"),
        GIGAMETER("gigameter"),
        HECTOMETER("hectometer"),
        INCH("inch"),
        KILOMETER("kilometer"),
        MEGAMETER("megameter"),
        METER("meter"),
        MICROMETER("micrometer"),
        MILE("mile"),
        MILLIMETER("millimeter"),
        NANOMETER("nanometer"),
        PARSEC("parsec"),
        PETAMETER("petameter"),
        PICOMETER("picometer"),
        TERAMETER("terameter"),
        YARD("yard"),
        YOCTOMETER("yoctometer"),
        YOTTAMETER("yottameter"),
        ZEPTOMETER("zeptometer"),
        ZETTAMETER("zettameter"),
        NO_UNIT("");

        private final String unit;

        SpaceUnit(String s) {
            this.unit = s;
        }
    }

    abstract static class SpaceAxisBase extends AxisBase implements ScaledAxis {
        private final SpaceUnit unit;
        private double scale = 1.0;

        SpaceAxisBase(String id, String description, String unit, double scale) {
            this(id, description, SpaceUnit.valueOf(unit.toUpperCase()), scale);
        }

        SpaceAxisBase(String id, String description, SpaceUnit unit, double scale) {
            super(id, description);
            this.unit = unit;
            this.scale = scale;
        }

        @Override
        public AxisType getType() {
            return AxisType.valueOf(this.getID().toUpperCase());
        }

        @Override
        public double getScale() {
            return this.scale;
        }

        public SpaceUnit getUnit() {
            return unit;
        }
    }

    static class SpaceInputAxis extends SpaceAxisBase {
        private final Size size;
        private final boolean concatenable = false;

        SpaceInputAxis(String id, String description, String unit, double scale, Size size) {
            super(id, description, unit.isEmpty() ? "NO_UNIT" : unit, scale);
            this.size = size;
        }

        @Override
        public Size getSize() {
            return this.size;
        }

        @Override
        public void validate(List<? extends BaseTensor> tensors) {
            getSize().validate(tensors);
        }
    }

    static class SpaceOutputAxis extends SpaceAxisBase {
        private final Size size;

        SpaceOutputAxis(String id, String description, String unit, double scale, Size size) {
            super(id, description, unit, scale);
            this.size = size;
        }

        @Override
        public Size getSize() {
            return this.size;
        }

        @Override
        public void validate(List<? extends BaseTensor> tensors) {
            getSize().validate(tensors);
        }
    }

    static class SpaceOutputAxisWithHalo extends SpaceOutputAxis implements WithHalo {
        private ReferencedSize size;
        private int halo = 0;

        SpaceOutputAxisWithHalo(String id, String description, String unit, double scale, Size size, int halo) {
            super(id, description, unit, scale, size);
            this.halo = halo;
        }

        @Override
        public int getHalo() {
            return this.halo;
        }
    }

}
