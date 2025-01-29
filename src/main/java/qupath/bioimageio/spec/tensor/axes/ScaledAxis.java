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

/**
 * Axes that have both size and scale.
 */
public interface ScaledAxis {
    /**
     * Get the size of this axis.
     * @return The size.
     */
    Size getSize();

    /**
     * Get the scale of this axis.
     * @return The scale (might be constant).
     */
    double getScale();
}
