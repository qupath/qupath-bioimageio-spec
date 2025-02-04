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

import qupath.bioimageio.spec.tensor.sizes.Size;
import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.List;

/**
 * Axes relating to physical time.
 */
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

    static class TimeAxis extends TimeAxisBase {
        private final Size size;

        TimeAxis(String id, String description, TimeUnit unit, double scale, Size size) {
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

    static class TimeAxisWithHalo extends TimeAxis implements WithHalo {
        private final int halo;

        TimeAxisWithHalo(String id, String description, TimeUnit unit, double scale, Size size, int halo) {
            super(id, description, unit, scale, size);
            this.halo = halo;
        }

        @Override
        public int getHalo() {
            return this.halo;
        }
    }

    /**
     * Possible SI units for time.
     */
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
