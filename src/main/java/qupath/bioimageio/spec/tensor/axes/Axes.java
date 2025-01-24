package qupath.bioimageio.spec.tensor.axes;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.bioimageio.spec.tensor.sizes.Size;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static qupath.bioimageio.spec.Model.deserializeField;
import static qupath.bioimageio.spec.tensor.sizes.Size.deserializeSize;

/**
 * Utility methods for Axis objects.
 */
public class Axes {
    /**
     * Get the old "bcyx" style axis representation of an Axis array.
     * @param axes The Axis array.
     * @return A string representing the axis types.
     */
    public static String getAxesString(Axis[] axes) {
        return Arrays.stream(axes).map(a -> a.getType().toString()).collect(Collectors.joining());
    }

    /**
     * Get any type adapters/deserializers needed for axis objects.
     * @return a map of class to deserializer for that class.
     */
    public static Map<Class<?>, JsonDeserializer<?>> getDeserializers() {
        Map<Class<?>, JsonDeserializer<?>> map = new HashMap<>();
        map.put(Axis[].class, new AxesDeserializer());
        return map;
    }

    static class AxesDeserializer implements JsonDeserializer<Axis[]> {
        private static final Logger logger = LoggerFactory.getLogger(AxesDeserializer.class);

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
