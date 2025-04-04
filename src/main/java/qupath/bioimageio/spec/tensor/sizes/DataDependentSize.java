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

package qupath.bioimageio.spec.tensor.sizes;


import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.List;

/**
 * A size that is only known after model inference (eg, detecting an unknown number of instances).
 */
class DataDependentSize implements Size {
    private final int min;
    private final int max;

    DataDependentSize(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public int size() {
        return NO_SIZE;
    }

    @Override
    public int getTargetSize(int target) {
        return size();
    }

    @Override
    public int getStep() {
        return NO_SIZE;
    }

    @Override
    public int getMin() {
        return min;
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        assert min > 0;
        assert max >= min;
    }
}
