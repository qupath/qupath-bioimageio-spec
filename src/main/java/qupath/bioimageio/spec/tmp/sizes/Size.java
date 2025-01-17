package qupath.bioimageio.spec.tmp.sizes;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.bioimageio.spec.tmp.tensor.BaseTensor;

import java.util.List;

import static qupath.bioimageio.spec.Utils.deserializeField;

/**
 * An axis size. Can be fixed, data-dependent (unknown), parameterized, or based on another axis.
 */
public interface Size {


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

    void validate(List<? extends BaseTensor> tensors);

    public static Size deserializeSize(JsonDeserializationContext context, JsonElement jsonElement, JsonElement scale) {
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
