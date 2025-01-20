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
import com.google.gson.annotations.SerializedName;
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
    @SerializedName("axis_id")
    private final String refID;
    @SerializedName("tensor_id")
    private final String tensorID;
    private final int offset;
    private final double scale;

    private volatile ScaledAxis referenceAxis;

    ReferencedSize(String refID, String tensorID) {
        this(refID, tensorID, 1, 0);
    }

    ReferencedSize(String refID, String tensorID, double scale, int offset) {
        this.refID = refID;
        this.tensorID = tensorID;
        this.scale = scale;
        this.offset = offset;
    }

    @Override
    public int getSize() {
        return (int) (referenceAxis.getSize().getSize() * referenceAxis.getScale() / scale + offset);
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
        var tensor = tensors.stream().filter(t -> t.getId().equals(tensorID)).findFirst().orElse(null);
        if (tensor == null) {
            throw new JsonParseException("Cannot find reference tensor " + tensorID);
        }
        ScaledAxis axis = (ScaledAxis) Arrays.stream(tensor.getAxes()).filter(ax -> ax.getID().equalsIgnoreCase(refID)).findFirst().orElse(null);
        if (axis == null) {
            throw new JsonParseException("Cannot find reference axis " + refID);
        }
        this.referenceAxis = axis;
    }
}
