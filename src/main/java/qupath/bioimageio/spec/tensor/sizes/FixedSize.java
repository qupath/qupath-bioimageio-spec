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
 * A fixed axis size. Basically an int with extra steps.
 */
public class FixedSize implements Size {
    private final int size;

    /**
     * Create a fixed size instance
     * @param size The fixed size in pixels.
     */
    public FixedSize(int size) {
        this.size = size;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getTargetSize(int target) {
        return getSize();
    }

    @Override
    public int getStep() {
        return NO_SIZE;
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        // can't validate ints
    }
}
