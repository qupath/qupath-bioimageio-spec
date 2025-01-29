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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.bioimageio.spec.tensor.BaseTensor;

import java.util.List;

import static qupath.bioimageio.spec.Model.deserializeField;


/**
 * An axis size. Can be fixed, data-dependent (unknown), parameterized, or based on another axis.
 */
public interface Size {

    /**
     * Constant to indicate that this element has no meaningful size for some context.
     * For example, a fixed size (int value) has no meaningful step value.
     */
    int NO_SIZE = -1;

    /**
     * Get the default size of this axis. {@link #getTargetSize(int)} may be more useful.
     * @return The size of this axis.
     */
    int getSize();

    /**
     * Get a size as close as possible to a target.
     * @param target The target size.
     * @return The fixed output size, {@link #NO_SIZE} or as close as we can get to the target size.
     */
    int getTargetSize(int target);

    /**
     * Get the step size of this axis size.
     * @return {@link #NO_SIZE} for any axis without a step size, otherwise the size.
     */
    int getStep();

    /**
     * Validate a tensor's size, ensuring that all internal fields are valid and resolving links between tensor objects.
     * @param tensors Tensors that may be referenced by this Size object.
     */
    void validate(List<? extends BaseTensor> tensors);

    /**
     * Deserialize a size object from JSON.
     * @param context The JSON context.
     * @param jsonElement the json element to be parsed.
     * @param scale the numeric scale that may or may not be needed
     * @return A size parsed from the input json element if possible.
     */
    static Size deserializeSize(JsonDeserializationContext context, JsonElement jsonElement, JsonElement scale) {
        Logger logger = LoggerFactory.getLogger(Size.class);
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }
        if (jsonElement.isJsonPrimitive()) {
            return new FixedSize(jsonElement.getAsInt());
        }
        if (jsonElement.isJsonObject()) {
            JsonObject obj = jsonElement.getAsJsonObject();
            if (obj.has("min") && obj.has("step")) {
                return context.deserialize(obj, ParameterizedSize.class);
            }
            if (obj.has("axis_id") && obj.has("tensor_id")) {
                return new ReferencedSize(
                        obj.get("axis_id").getAsString(),
                        obj.get("tensor_id").getAsString(),
                        scale != null ? scale.getAsDouble() : 1.0,
                        deserializeField(context, obj, "offset", Integer.class, 1)
                );
            }
            return new DataDependentSize(
                    deserializeField(context, obj, "min", Integer.class, 1),
                    deserializeField(context, obj, "max", Integer.class, 1));
        }
        logger.error("Unknown JSON element {}", jsonElement);
        throw new JsonParseException("No idea what type of size this is, sorry!");
    }

}
