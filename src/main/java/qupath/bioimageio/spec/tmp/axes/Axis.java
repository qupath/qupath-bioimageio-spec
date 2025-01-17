package qupath.bioimageio.spec.tmp.axes;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.bioimageio.spec.Utils;
import qupath.bioimageio.spec.Deserializers;
import qupath.bioimageio.spec.tmp.sizes.Size;
import qupath.bioimageio.spec.tmp.tensor.BaseTensor;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import static qupath.bioimageio.spec.Deserializers.deserializeField;
import static qupath.bioimageio.spec.Deserializers.deserializeSize;

/**
 * Base axis class for 0.4 and 0.5 axes.
 */
public interface Axis {

    /**
     * Get the type of this axis, see {@link AxisType}.
     *
     * @return The axis type.
     */
    AxisType getType();

    /**
     * Get the size of this axis.
     *
     * @return The size, unless it's a 0.4 axis (these have no size).
     */
    Size getSize();

    /**
     * Get the axis ID.
     *
     * @return The axis ID.
     */
    String getID();

    /**
     * Ensure the parameters of the axis are valid.
     *
     * @param tensors Other tensors in the model, in case they are referenced in this axis.
     */
    void validate(List<? extends BaseTensor> tensors);

    class AxesDeserializer implements JsonDeserializer<Axis[]> {
        Logger logger = LoggerFactory.getLogger(AxesDeserializer.class);

        @Override
        public Axis[] deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()) {
                var s = jsonElement.getAsString();
                Axis[] axes = new Axis[s.length()];
                for (int i = 0; i < axes.length; i++) {
                    axes[i] = new CharAxis(s.charAt(i));
                }
                return axes;
            }
            // todo: input or output???
            if (jsonElement.isJsonArray()) {
                var arr = jsonElement.getAsJsonArray();
                Axis[] axes = new Axis[arr.size()];
                for (int i = 0; i < axes.length; i++) {
                    var curr = arr.get(i);
                    if (curr.isJsonPrimitive()) {
                        axes[i] = new CharAxis(curr.getAsString().charAt(0));
                        continue;
                    }

                    var oj = curr.getAsJsonObject();
                    var id = deserializeField(context, oj, "id", String.class, "");
                    var desc = deserializeField(context, oj, "description", String.class, "");
                    Size size = deserializeSize(context, oj.get("size"), oj.get("scale"));
                    switch (oj.get("type").getAsString()) {
                        case "time":
                            axes[i] = new TimeAxes.TimeInputAxis(
                                    id, desc,
                                    TimeAxes.TimeUnit.valueOf(oj.get("unit").getAsString().toUpperCase()),
                                    oj.get("scale").getAsDouble(),
                                    size
                            );
                            break;
                        case "channel":
                            var namesJSON = oj.get("channel_names").getAsJsonArray();
                            List<String> names = new LinkedList<>();
                            for (JsonElement n : namesJSON) {
                                names.add(n.getAsString());
                            }
                            axes[i] = new ChannelAxis(
                                    id, desc,
                                    names
                            );
                            break;
                        case "index":
                            axes[i] = new IndexAxes.IndexInputAxis(id, desc, size);
                            break;
                        case "space":
                            axes[i] = new SpaceAxes.SpaceInputAxis(
                                    id, desc,
                                    deserializeField(context, oj, "unit", String.class, ""),
                                    deserializeField(context, oj, "scale", Double.class, 1.0),
                                    size
                            );
                            break;
                        case "batch":
                            axes[i] = new BatchAxis(id, desc, deserializeField(context, oj, "size", Integer.class, 1));
                            break;
                        default:
                            logger.error("Unknown object {}", oj);
                            axes[i] = null;
                    }
                }
                return axes;
            }
            logger.error("Unknown JSON element {}", jsonElement);
            return null;
        }
    }

}
