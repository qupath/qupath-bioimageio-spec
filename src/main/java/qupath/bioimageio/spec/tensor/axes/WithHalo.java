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

/**
 * An axis with a halo.
 * The halo should be cropped from the output tensor to avoid boundary effects.
 * It is to be cropped from both sides, i.e. `size_after_crop = size - 2 * halo`.
 * To document a halo that is already cropped by the model use `size.offset` instead.
 */
public interface WithHalo {

    /**
     * Get the size of the halo for this axis.
     * @return The size of the halo in pixels.
     */
    int getHalo();
}
