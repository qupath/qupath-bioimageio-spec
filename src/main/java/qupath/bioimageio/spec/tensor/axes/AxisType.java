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
 * The type of axis. Batch (b), index (i), channel (c), x, y, z, time (t).
 */
public enum AxisType {
    B("b"),
    I("i"),
    C("c"),
    X("x"),
    Y("y"),
    Z("z"),
    T("t");
    private final String type;

    AxisType(String type) {
        this.type = type;
    }

    static AxisType fromString(String s) {
        return AxisType.valueOf(s.toUpperCase());
    }

    @Override
    public String toString() {
        return type;
    }
}
