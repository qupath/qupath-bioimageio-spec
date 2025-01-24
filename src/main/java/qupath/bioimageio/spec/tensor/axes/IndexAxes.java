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
 * Axes that relate to indexes rather than space/time/channel/batch, for example lists of objects.
 */
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
