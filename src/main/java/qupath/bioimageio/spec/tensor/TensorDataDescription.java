package qupath.bioimageio.spec.tensor;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface TensorDataDescription {

    /**
     * A description of the possible discrete data values in a tensor.
     */
    class NominalOrOrdinalDataDescription implements TensorDataDescription {
        private final NominalOrOrdinalDType type;
        private final List<?> values;

        /**
         * Get the possible values for the described tensor to which numeric dtype values relate to.
         * @return A list, where elements can be int, float, bool, String.
         */
        public List<?> getValues() {
            return values;
        }

        /**
         * The data type of the underlying tensor.
         * @return An enum describing the type.
         */
        public NominalOrOrdinalDType getType() {
            return type;
        }

        NominalOrOrdinalDataDescription(NominalOrOrdinalDType type, List<?> values) {
            this.type = type;
            this.values = values;
        }
    }


    /**
     * A description of the possible ratio data values in a tensor.
     */
    class IntervalOrRatioDataDescription implements TensorDataDescription {
        private final IntervalOrRatioDType type;
        private final List<Optional<Float>> range;
        private final String unit; // todo: SI unit or "abitrary unit": validate somehow
        private final float scale;
        private final float offset;

        IntervalOrRatioDataDescription(IntervalOrRatioDType type, List<Optional<Float>> range, String unit, float scale, float offset) {
            this.type = type;
            this.range = range;
            this.unit = unit;
            this.scale = scale;
            this.offset = offset;
        }

        IntervalOrRatioDataDescription(IntervalOrRatioDType type, List<Optional<Float>> range, String unit, float scale) {
            this(type, range, unit, scale, 0);
        }

        IntervalOrRatioDataDescription(IntervalOrRatioDType type, List<Optional<Float>> range, String unit) {
            this(type, range, unit, 1, 0);
        }

        IntervalOrRatioDataDescription(IntervalOrRatioDType type, List<Optional<Float>> range) {
            this(type, range, "aritrary unit", 1, 0);
        }

        IntervalOrRatioDataDescription(IntervalOrRatioDType type) {
            this(type, List.of(Optional.empty(), Optional.empty()), "aritrary unit", 1, 0);
        }

        public IntervalOrRatioDType getType() {
            return type;
        }

        public List<Optional<Float>> getRange() {
            return range;
        }

        public String getUnit() {
            return unit;
        }

        public float getScale() {
            return scale;
        }

        public float getOffset() {
            return offset;
        }

    }

    enum IntervalOrRatioDType {
        FLOAT32,
        FLOAT64,
        UINT8,
        INT8,
        UINT16,
        INT16,
        UINT32,
        INT32,
        UINT64,
        INT64
    }

    enum NominalOrOrdinalDType {
        FLOAT32,
        FLOAT64,
        UINT8,
        INT8,
        UINT16,
        INT16,
        UINT32,
        INT32,
        UINT64,
        INT64,
        BOOL
    }


    class Deserializer implements JsonDeserializer<TensorDataDescription> {

        @Override
        public TensorDataDescription deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            Logger logger = LoggerFactory.getLogger(TensorDataDescription.class);
            if (jsonElement.isJsonNull()) {
                return null;
            }
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject.get("values") != null) {
                    JsonArray vals = jsonObject.get("values").getAsJsonArray();
                    List<Object> values = new ArrayList<>();
                    for (var val : vals) {
                        if (val instanceof JsonPrimitive) {
                            var prim = val.getAsJsonPrimitive();
                            if (prim.isNumber()) {
                                values.add(prim.getAsNumber());
                            }
                            if (prim.isBoolean()) {
                                values.add(prim.getAsBoolean());
                            }
                            if (prim.isString()) {
                                values.add(prim.getAsString());
                            }
                        }
                    }
                    return new TensorDataDescription.NominalOrOrdinalDataDescription(
                            TensorDataDescription.NominalOrOrdinalDType.valueOf(jsonObject.get("type").getAsString().toUpperCase()),
                            values
                    );
                }
                var t = jsonObject.get("type");
                var r = jsonObject.get("range");
                List<Optional<Float>> range;
                if (r == null) {
                    range = List.of(Optional.empty(), Optional.empty());
                } else {
                    range = r.getAsJsonArray().asList().stream()
                            .map(JsonElement::getAsFloat)
                            .map(Optional::of)
                            .collect(Collectors.toList());
                }
                JsonElement unit = jsonObject.get("unit");
                JsonElement scale = jsonObject.get("scale");
                JsonElement offset = jsonObject.get("offset");
                return new TensorDataDescription.IntervalOrRatioDataDescription(
                        TensorDataDescription.IntervalOrRatioDType.valueOf((t != null ? t.getAsString(): "float32").toUpperCase()),
                        range,
                        unit != null ? unit.getAsString() : "abitrary unit",
                        scale != null ? scale.getAsFloat() :  1.0f,
                        offset != null ? offset.getAsFloat() :  1.0f
                );
            }
            logger.warn("Unknown data description! Returning null");
            return null;
        }
    }

}
