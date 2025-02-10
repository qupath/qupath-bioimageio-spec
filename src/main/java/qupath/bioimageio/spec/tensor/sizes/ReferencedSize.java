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

import com.google.gson.JsonParseException;
import qupath.bioimageio.spec.tensor.axes.ScaledAxis;
import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.Arrays;
import java.util.List;

/**
 * A tensor axis size (extent in pixels/frames) defined in relation to a reference axis.
 * <br>
 * <code>size = reference.size * reference.scale / axis.scale + offset</code>
 */
public class ReferencedSize implements Size {
    private volatile String thisID;
    private final String axisId;
    private final String tensorId;
    private final int offset;
    private final double scale;
    private volatile ScaledAxis referenceAxis;

    ReferencedSize(String axisId, String tensorId) {
        this(axisId, tensorId, 1, 0);
    }

    ReferencedSize(String axisId, String tensorId, double scale, int offset) {
        this.axisId = axisId;
        this.tensorId = tensorId;
        this.scale = scale;
        this.offset = offset;
    }

    @Override
    public int size() {
        return (int) (referenceAxis.getSize().size() * referenceAxis.getScale() / scale + offset);
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
        return size();
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        var tensor = tensors.stream().filter(t -> t.getId().equals(tensorId)).findFirst().orElse(null);
        if (tensor == null) {
            throw new JsonParseException("Cannot find reference tensor " + tensorId);
        }
        ScaledAxis axis = (ScaledAxis) Arrays.stream(tensor.getAxes()).filter(ax -> ax.getID().equalsIgnoreCase(axisId)).findFirst().orElse(null);
        if (axis == null) {
            throw new JsonParseException("Cannot find reference axis " + axisId);
        }
        this.referenceAxis = axis;
    }
}
