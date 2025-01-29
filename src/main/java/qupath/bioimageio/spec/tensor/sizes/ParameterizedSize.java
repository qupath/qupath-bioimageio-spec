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
 * Describes a range of valid tensor axis sizes as `size = min + n*step`.
 */
class ParameterizedSize implements Size {
    private final int min;
    private final int step;

    ParameterizedSize(int min, int step) {
        this.min = min;
        this.step = step;
    }

    @Override
    public int getSize() {
        return getTargetSize(1);
    }

    @Override
    public int getTargetSize(int target) {
        if (target < 0 || step <= 0)
            return min;
        else
            return min + (int)Math.round((target - min)/(double)step) * step;
    }
    @Override
    public int getStep() {
        return step;
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        assert min >= 0;
        assert step >= 0;
    }
}
