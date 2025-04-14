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

import java.util.List;

import qupath.bioimageio.spec.tensor.BaseTensor;
import qupath.bioimageio.spec.tensor.sizes.ReferencedSize;
import qupath.bioimageio.spec.tensor.sizes.Size;

/**
 * Axes relating to physical space.
 */
public class SpaceAxes {

    /**
     * Possible SI and imperial units for distance in physical space.
     */
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

        SpaceUnit(String unit) {
            this.unit = unit;
        }

        /**
         * Get the SpaceUnit corresponding to a String value.
         * @param unit the unit as a string
         * @return the corresponding unit, or NO_UNIT if not found.
         */
        public static SpaceUnit getUnit(String unit) {
            for (var su: values()) {
                if (su.unit.equalsIgnoreCase(unit)) {
                    return su;
                }
            }
            return NO_UNIT;
        }
    }

    /**
     * An axis that relates to physical space (X, Y, Z)
     */
    public static class SpaceAxis extends AxisBase implements ScaledAxis {
        private final SpaceUnit unit;
        private final double scale;
        private final Size size;
        private final boolean concatenable = false;

        SpaceAxis(String id, String description, String unit, double scale, Size size) {
            this(id, description, SpaceUnit.getUnit(unit), scale, size);
        }

        SpaceAxis(String id, String description, SpaceUnit unit, double scale, Size size) {
            super(id, description);
            this.unit = unit;
            this.scale = scale;
            this.size = size;
        }

        @Override
        public AxisType getType() {
            return AxisType.valueOf(this.getID().toUpperCase());
        }

        @Override
        public double getScale() {
            return this.scale;
        }

        /**
         * Gets the unit for the space dimension
         * @return the unit (hopefully SI, but maybe imperial)
         */
        public SpaceUnit getUnit() {
            return unit;
        }

        @Override
        public void validate(List<? extends BaseTensor> tensors) {
            getSize().validate(tensors);
        }

        @Override
        public Size getSize() {
            return this.size;
        }
    }

    static class SpaceAxisWithHalo extends SpaceAxis implements WithHalo {
        private ReferencedSize size;
        private final int halo;

        SpaceAxisWithHalo(String id, String description, String unit, double scale, Size size, int halo) {
            super(id, description, unit, scale, size);
            this.halo = halo;
        }

        @Override
        public int getHalo() {
            return this.halo;
        }
    }

}
