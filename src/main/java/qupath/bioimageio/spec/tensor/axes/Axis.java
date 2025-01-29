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

import com.google.gson.JsonDeserializer;
import qupath.bioimageio.spec.tensor.sizes.Size;
import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base axis class for 0.4 and 0.5 axes.
 */
public interface Axis {

    /**
     * Get the type of this axis, see {@link AxisType}.
     *
     * @return The axis type.
     */
    AxisType getType();

    /**
     * Get the size of this axis.
     *
     * @return The size, unless it's a 0.4 axis (these have no size).
     */
    Size getSize();

    /**
     * Get the axis ID.
     *
     * @return The axis ID.
     */
    String getID();

    /**
     * Ensure the parameters of the axis are valid, possibly resolving references to other tensors and/or axes.
     *
     * @param tensors Other tensors in the model, in case they are referenced in this axis.
     */
    void validate(List<? extends BaseTensor> tensors);

}
