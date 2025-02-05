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

import qupath.bioimageio.spec.tensor.sizes.FixedSize;
import qupath.bioimageio.spec.tensor.sizes.Size;
import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.List;

/**
 * An axis corresponding to a set of channels.
 */
public class ChannelAxis extends AxisBase implements ScaledAxis {
    private final List<String> channel_names;

    ChannelAxis(String id, String description, List<String> channel_names) {
        super(id, description);
        this.channel_names = List.copyOf(channel_names);
    }

    @Override
    public Size getSize() {
        return new FixedSize(channel_names.size());
    }

    @Override
    public void validate(List<? extends BaseTensor> tensors) {
        // fixed size based on list of channels
    }

    @Override
    public AxisType getType() {
        return AxisType.C;
    }

    @Override
    public double getScale() {
        return 1;
    }
}
