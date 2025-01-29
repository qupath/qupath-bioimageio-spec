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
 * Simple class to hold the old "bcyx" format from 0.4
 */
class CharAxis implements Axis {
    private final char axis;

    CharAxis(char c) {
        this.axis = c;
    }

    @Override
    public AxisType getType() {
        return AxisType.valueOf(String.valueOf(axis).toUpperCase());
    }

    @Override
    public Size getSize() {
        throw new UnsupportedOperationException("Cannot get Size of legacy/char axes");
    }

    @Override
    public String getID() {
        return "";
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        // can't validate char axes, these are validated at tensor level
    }
}
