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
 * A base axis class.
 */
abstract class AxisBase implements Axis {
    private final String id;
    private final String description;
    AxisBase(String id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public String getID() {
        return this.id;
    }

    /**
     * Gets the description of the axis, if present
     * @return the free text description (usually null)
     */
    public String getDescription() {
        return description;
    }
}
