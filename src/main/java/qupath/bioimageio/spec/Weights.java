package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static qupath.bioimageio.spec.Model.toUnmodifiableList;


public class Weights {
    /**
     * Model weights information for a specific format.
     */
    public static class ModelWeights {

        private String source;
        private Map<String, ?> attachments;
        private List<Author> authors;
        private String parent;
        private String sha256;

        public String getSource() {
            return source;
        }

        public Map<String, ?> getAttachments() {
            return attachments == null ? Collections.emptyMap() : Collections.unmodifiableMap(attachments);
        }

        public String getParent() {
            return parent;
        }

        public String getSha256() {
            return sha256;
        }

        public List<Author> getAuthors() {
            return toUnmodifiableList(authors);
        }

        @Override
        public String toString() {
            return "Weights: " + source;
        }

    }

    /**
     * Enum representing supported model weights.
     */
    public enum WeightsEntry {

        KERAS_HDF5("keras_hdf5"),
        PYTORCH_STATE_DICT("pytorch_state_dict"),
        TENSORFLOW_JS("tensorflow_js"),
        TENSORFLOW_SAVED_MODEL_BUNDLE("tensorflow_saved_model_bundle"),
        ONNX("onnx"),
        TORCHSCRIPT("torchscript", Set.of("pytorch_script"));

        private final String key;
        // Support older key names as alternatives
        private final Set<String> alternatives;

        WeightsEntry(String key) {
            this(key, Collections.emptySet());
        }

        WeightsEntry(String key, Set<String> alternatives) {
            this.key = key;
            this.alternatives = Collections.unmodifiableSet(alternatives);
        }

        @Override
        public String toString() {
            return key;
        }

        static WeightsEntry fromKey(String name) {
            for (var v : values()) {
                if (v.key.equals(name) || v.alternatives.contains(name))
                    return v;
            }
            return null;
        }
    }

    public static class WeightsMap {

        Map<WeightsEntry, ModelWeights> map;

        public Map<String, ModelWeights> withStringKeys() {
            return map == null ? Collections.emptyMap() : map.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue()));
        }

        Map<WeightsEntry, ModelWeights> getMap() {
            return map;
        }
    }

    static class WeightsEntryDeserializer implements JsonDeserializer<WeightsEntry> {

        @Override
        public WeightsEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return WeightsEntry.fromKey(json.toString());
        }

    }


    static class WeightsMapDeserializer implements JsonDeserializer<WeightsMap> {
        private static final Logger logger = LoggerFactory.getLogger(Weights.class);

        @Override
        public WeightsMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            var weights = new WeightsMap();
            if (json.isJsonObject()) {
                var obj = json.getAsJsonObject();
                var map = new LinkedHashMap<WeightsEntry, ModelWeights>();
                for (var key : json.getAsJsonObject().keySet()) {
                    var we = WeightsEntry.fromKey(key);
                    if (we == null) {
                        logger.warn("Unsupported weights: {}", key);
                        continue;
                    }
                    map.put(we, context.deserialize(obj.get(key), ModelWeights.class));
                }
                weights.map = Collections.unmodifiableMap(map);
            }
            return weights;
        }

    }

}
