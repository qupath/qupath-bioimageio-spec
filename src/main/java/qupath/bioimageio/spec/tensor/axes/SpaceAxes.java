/*
 * Copyright 2025 University of Edinburgh
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package qupath.bioimageio.spec.tensor.axes;

import qupath.bioimageio.spec.tensor.sizes.ReferencedSize;
import qupath.bioimageio.spec.tensor.sizes.Size;
import qupath.bioimageio.spec.tensor.BaseTensor;

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

        /**
         * Get the SpaceUnit corresponding to a String value.
         * @param unit The unit as a string
         * @return The corresponding unit, or null if not found.
         */
        public static SpaceUnit getUnit(String unit) {
            for (var su: SpaceUnit.values()) {
                if (su.unit.equalsIgnoreCase(unit)) {
                    return su;
                }
            }
            return null;
        }
    }

    abstract static class SpaceAxisBase extends AxisBase implements ScaledAxis {
        private final SpaceUnit unit;
        private double scale;

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
        private int halo;

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
