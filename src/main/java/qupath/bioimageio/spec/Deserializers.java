package qupath.bioimageio.spec;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static qupath.bioimageio.spec.BioImageIoParsing.parameterizedListType;

class Deserializers {
    private Deserializers() {
        throw new UnsupportedOperationException("Do not instantiate this class");
    }

    private final static Logger logger = LoggerFactory.getLogger(Deserializers.class);

    // todo: these should arguably be in a "Deserialization" class or in
    private static void deserializeResourceFields(BioimageIoSpec.BioimageIoResource resource, JsonObject obj, JsonDeserializationContext context, boolean doStrict) {

        resource.formatVersion = deserializeField(context, obj, "format_version", String.class, doStrict);
        resource.authors = deserializeField(context, obj, "authors", parameterizedListType(BioimageIoSpec.Author.class), doStrict);
        resource.description = deserializeField(context, obj, "description", String.class, doStrict);
        resource.documentation = deserializeField(context, obj, "documentation", String.class, doStrict);

        resource.license = deserializeField(context, obj, "license", String.class, doStrict);
        resource.name = deserializeField(context, obj, "name", String.class, doStrict);

        if (obj.has("attachments")) {
            if (obj.get("attachments").isJsonObject())
                resource.attachments = deserializeField(context, obj, "attachments", Map.class, Collections.emptyMap());
            else
                logger.warn("Can't parse attachments (not a dict)");
        }
        resource.badges = deserializeField(context, obj, "badges", parameterizedListType(BioimageIoSpec.Badge.class), Collections.emptyList());
        resource.cite = deserializeField(context, obj, "cite", parameterizedListType(BioimageIoSpec.CiteEntry.class), Collections.emptyList());

        resource.covers = deserializeField(context, obj, "covers", parameterizedListType(String.class), Collections.emptyList());
        resource.downloadURL = deserializeField(context, obj, "download_url", String.class, null);
        resource.gitRepo = deserializeField(context, obj, "git_repo", String.class, null);
        resource.icon = deserializeField(context, obj, "icon", String.class, null);
        resource.id = deserializeField(context, obj, "id", String.class, null);
        resource.links = deserializeField(context, obj, "links", parameterizedListType(String.class), Collections.emptyList());
        resource.maintainers = deserializeField(context, obj, "maintainers", parameterizedListType(BioimageIoSpec.Author.class), Collections.emptyList());

        resource.rdfSource = deserializeField(context, obj, "rdf_source", String.class, null);

        resource.tags = deserializeField(context, obj, "tags", parameterizedListType(String.class), Collections.emptyList());

        resource.trainingData = deserializeField(context, obj, "training_data", BioimageIoSpec.BioimageIoDataset.class, null);

        resource.version = deserializeField(context, obj, "version", String.class, null);
    }



    private static void deserializeModelFields(BioimageIoSpec.BioimageIoModel model, JsonObject obj, JsonDeserializationContext context, boolean doStrict) {
        model.inputs = deserializeField(context, obj, "inputs", parameterizedListType(BioimageIoSpec.InputTensor.class), doStrict);
        List<BioimageIoSpec.BaseTensor> tensors = new ArrayList<>(List.copyOf(model.inputs));
        for (var tensor: model.inputs) {
            tensor.validate(tensors);
        }


        if (model.isNewerThan("0.5.0")) {
            model.testInputs = List.of();
            model.testOutputs = List.of();
            model.timestamp = "";
        } else {
            // now part of the tensor spec:
            model.testInputs = deserializeField(context, obj, "test_inputs", parameterizedListType(String.class), doStrict);
            // now part of the tensor spec:
            model.testOutputs = deserializeField(context, obj, "test_outputs", parameterizedListType(String.class), doStrict);
            // removed...?
            model.timestamp = deserializeField(context, obj, "timestamp", String.class, doStrict);
        }

        model.weights = deserializeField(context, obj, "weights", BioimageIoSpec.WeightsMap.class, doStrict);

        model.config = deserializeField(context, obj, "config", Map.class, Collections.emptyMap());

        model.outputs = deserializeField(context, obj, "outputs", parameterizedListType(BioimageIoSpec.OutputTensor.class), Collections.emptyList());
        tensors.addAll(model.outputs);
        for (var tensor: model.outputs) {
            tensor.validate(tensors);
        }
        model.packagedBy = deserializeField(context, obj, "packaged_by", parameterizedListType(BioimageIoSpec.Author.class), Collections.emptyList());

        model.parent = deserializeField(context, obj, "parent", BioimageIoSpec.ModelParent.class, null);

        if (obj.has("run_mode")) {
            if (obj.get("run_mode").isJsonObject())
                model.runMode = deserializeField(context, obj, "run_mode", Map.class, Collections.emptyMap());
            else
                logger.warn("Can't parse run_mode (not an object)");
        }

        model.sampleInputs = deserializeField(context, obj, "sample_inputs", parameterizedListType(String.class), Collections.emptyList());
        model.sampleOutputs = deserializeField(context, obj, "sample_outputs", parameterizedListType(String.class), Collections.emptyList());
    }

    /**
     * Deserialize a field from a JSON object.
     * @param <T> The type of the field.
     * @param context The context used for deserialization.
     * @param obj The JSON object that contains the field.
     * @param name The name of the field.
     * @param typeOfT The type of the field.
     * @param doStrict if true, fail if the field is missing; otherwise, return null
     * @return A parsed T object.
     * @throws IllegalArgumentException if doStrict is true and the field is not found
     */
    private static <T> T deserializeField(JsonDeserializationContext context, JsonObject obj, String name, Type typeOfT, boolean doStrict) throws IllegalArgumentException {
        if (doStrict && !obj.has(name))
            throw new IllegalArgumentException("Required field " + name + " not found");
        return deserializeField(context, obj, name, typeOfT, null);
    }

    private static <T> T deserializeField(JsonDeserializationContext context, JsonObject obj, String name, Type typeOfT, T defaultValue) {
        if (obj.has(name)) {
            return ensureUnmodifiable(context.deserialize(obj.get(name), typeOfT));
        }
        return ensureUnmodifiable(defaultValue);
    }

    /**
     * Minor optimization - ensure any lists, maps or sets are unmodifiable at this point,
     * to avoid generating new unmodifiable wrappers later.
     * @param <T> The type of the object.
     * @param input A collection that should be made unmodifiable (copied if not already unmodifiable).
     * @return An unmodifiable object of class T.
     */
    @SuppressWarnings("unchecked")
    private static <T> T ensureUnmodifiable(T input) {
        if (input instanceof List)
            return (T)Collections.unmodifiableList((List<?>)input);
        if (input instanceof Map)
            return (T)Collections.unmodifiableMap((Map<?, ?>)input);
        if (input instanceof Set)
            return (T)Collections.unmodifiableSet((Set<?>)input);
        return input;
    }

    static class ResourceDeserializer implements JsonDeserializer<BioimageIoSpec.BioimageIoResource> {

        @Override
        public BioimageIoSpec.BioimageIoResource deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json.isJsonNull())
                return null;
            var obj = json.getAsJsonObject();
            BioimageIoSpec.BioimageIoResource resource = new BioimageIoSpec.BioimageIoResource();
            deserializeResourceFields(resource, obj, context, true);
            return resource;
        }

    }

    static class DatasetDeserializer implements JsonDeserializer<BioimageIoSpec.BioimageIoDataset> {

        @Override
        public BioimageIoSpec.BioimageIoDataset deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json.isJsonNull())
                return null;
            var obj = json.getAsJsonObject();
            BioimageIoSpec.BioimageIoDataset dataset = new BioimageIoSpec.BioimageIoDataset();
            // Deserialize, but not strictly... i.e. allow nulls because we might just have an ID
            deserializeResourceFields(dataset, obj, context, false);
            return dataset;
        }

    }


    static class ModelDeserializer implements JsonDeserializer<BioimageIoSpec.BioimageIoModel> {

        @Override
        public BioimageIoSpec.BioimageIoModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var model = new BioimageIoSpec.BioimageIoModel();

            var obj = json.getAsJsonObject();

            deserializeResourceFields(model, obj, context, true);
            deserializeModelFields(model, obj, context, true);

            return model;
        }
    }


    /**
     * Deal with the awkwardness of -inf and inf instead of .inf and .inf.
     * This otherwise caused several failures for data range.
     */
    static class DoubleArrayDeserializer implements JsonDeserializer<double[]> {

        @Override
        public double[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;
            if (json.isJsonArray()) {
                List<Double> values = new ArrayList<>();
                for (var jsonVal : json.getAsJsonArray()) {
                    if (jsonVal.isJsonNull()) {
                        logger.warn("Found null when expecting a double - will replace with NaN");
                        values.add(Double.NaN);
                        continue;
                    }
                    var jsonPrimitive = jsonVal.getAsJsonPrimitive();
                    if (jsonPrimitive.isNumber()) {
                        values.add(jsonPrimitive.getAsDouble());
                    } else {
                        var s = jsonPrimitive.getAsString();
                        if ("inf".equalsIgnoreCase(s))
                            values.add(Double.POSITIVE_INFINITY);
                        else if ("-inf".equalsIgnoreCase(s))
                            values.add(Double.NEGATIVE_INFINITY);
                        else
                            values.add(Double.parseDouble(s));
                    }
                }
                return values.stream().mapToDouble(d -> d).toArray();
            } else
                throw new JsonParseException("Can't parse data range from " + json);
        }
    }


    static class TensorDataDescriptionDeserializer implements JsonDeserializer<BioimageIoSpec.TensorDataDescription> {

        @Override
        public BioimageIoSpec.TensorDataDescription deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (jsonElement.isJsonNull()) {
                return null;
            }
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject.get("values") != null) {
                    JsonArray vals = jsonObject.get("values").getAsJsonArray(); // todo: parse to values...
                    return new BioimageIoSpec.NominalOrOrdinalDataDescription(
                            BioimageIoSpec.NominalOrOrdinalDType.valueOf(jsonObject.get("type").getAsString().toUpperCase()),
                            vals.asList()
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
                return new BioimageIoSpec.IntervalOrRatioDataDescription(
                        BioimageIoSpec.IntervalOrRatioDType.valueOf((t != null ? t.getAsString(): "float32").toUpperCase()),
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

    static class ProcessingModeDeserializer implements JsonDeserializer<BioimageIoSpec.Processing.ProcessingMode> {

        @Override
        public BioimageIoSpec.Processing.ProcessingMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var s = json.getAsString();
            for (var mode : BioimageIoSpec.Processing.ProcessingMode.values()) {
                if (s.equals(mode.name()))
                    return mode;
            }

            for (var mode : BioimageIoSpec.Processing.ProcessingMode.values()) {
                if (s.equalsIgnoreCase(mode.name()))
                    return mode;
            }

            logger.warn("Unknown processing mode: {}", s);
            return null;
        }

    }

    static BioimageIoSpec.Size deserializeSize(JsonDeserializationContext context, JsonElement jsonElement, JsonElement scale) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }
        if (jsonElement.isJsonPrimitive()) {
            return new BioimageIoSpec.FixedSize(jsonElement.getAsInt());
        }
        if (jsonElement.isJsonObject()) {
            JsonObject obj = jsonElement.getAsJsonObject();
            if (obj.has("min") && obj.has("step")) {
                return context.deserialize(obj, BioimageIoSpec.ParameterizedSize.class);
            }
            if (obj.has("axis_id") && obj.has("tensor_id")) {
                return new BioimageIoSpec.ReferencedSize(
                        obj.get("axis_id").getAsString(),
                        obj.get("tensor_id").getAsString(),
                        scale != null ? scale.getAsDouble() : 1.0,
                        deserializeField(context, obj, "offset", Integer.class, 1)
                );
            }
            return new BioimageIoSpec.DataDependentSize(
                    deserializeField(context, obj, "min", Integer.class, 1),
                    deserializeField(context, obj, "max", Integer.class, 1));
        }
        logger.error("Unknown JSON element {}", jsonElement);
        throw new JsonParseException("No idea what type of size this is, sorry!");
    }

    static class Deserializer implements JsonDeserializer<BioimageIoSpec.Shape> {

        @Override
        public BioimageIoSpec.Shape deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            if (json.isJsonArray()) {
                var shape = new BioimageIoSpec.Shape();
                shape.shape = context.deserialize(json, int[].class);
                return shape;
            }
            var obj = json.getAsJsonObject();
            if (obj.has("min") && obj.has("step")) {
                return context.deserialize(obj, BioimageIoSpec.Shape.ParameterizedInputShape.class);
            }
            if (obj.has("offset") && obj.has("scale")) {
                return context.deserialize(obj, BioimageIoSpec.Shape.ImplicitOutputShape.class);
            }
            throw new JsonParseException("Can't deserialize unknown shape: " + json);
        }

    }

    static class WeightsEntryDeserializer implements JsonDeserializer<BioimageIoSpec.WeightsEntry> {

        @Override
        public BioimageIoSpec.WeightsEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return BioimageIoSpec.WeightsEntry.fromKey(json.toString());
        }

    }


    static class WeightsMapDeserializer implements JsonDeserializer<BioimageIoSpec.WeightsMap> {

        @Override
        public BioimageIoSpec.WeightsMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            var weights = new BioimageIoSpec.WeightsMap();
            if (json.isJsonObject()) {
                var obj = json.getAsJsonObject();
                var map = new LinkedHashMap<BioimageIoSpec.WeightsEntry, BioimageIoSpec.ModelWeights>();
                for (var key : json.getAsJsonObject().keySet()) {
                    var we = BioimageIoSpec.WeightsEntry.fromKey(key);
                    if (we == null) {
                        logger.warn("Unsupported weights: {}", key);
                        continue;
                    }
                    map.put(we, context.deserialize(obj.get(key), BioimageIoSpec.ModelWeights.class));
                }
                weights.map = Collections.unmodifiableMap(map);
            }
            return weights;
        }

    }

    static class AuthorDeserializer implements JsonDeserializer<BioimageIoSpec.Author> {

        @Override
        public BioimageIoSpec.Author deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var author = new BioimageIoSpec.Author();
            if (json.isJsonObject()) {
                var obj = json.getAsJsonObject();
                author.name = deserializeField(context, obj, "name", String.class, null);
                author.affiliation = deserializeField(context, obj, "affiliation", String.class, null);
                author.githubUser = deserializeField(context, obj, "github_user", String.class, null);
                author.orcid = deserializeField(context, obj, "orcid", String.class, null);
            } else if (json.isJsonPrimitive()) {
                author.name = json.getAsString();
            }
            return author;
        }

    }

    static class ProcessingDeserializer implements JsonDeserializer<BioimageIoSpec.Processing> {

        @Override
        public BioimageIoSpec.Processing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var obj = json.getAsJsonObject();
            var name = obj.has("name") ? obj.get("name").getAsString() : obj.get("id").getAsString();
            JsonObject kwargs = deserializeField(context, obj, "kwargs", JsonObject.class, null);;
            switch (name) {
                case "binarize":
                    var binarize = new BioimageIoSpec.Processing.Binarize();
                    binarize.threshold = deserializeField(context, kwargs, "threshold", Double.class, true);
                    return binarize;
                case "clip":
                    var clip = new BioimageIoSpec.Processing.Clip();
                    clip.min = deserializeField(context, kwargs, "min", Double.class, Double.NEGATIVE_INFINITY);
                    clip.max = deserializeField(context, kwargs, "max", Double.class, Double.POSITIVE_INFINITY);
                    return clip;
                case "scale_linear":
                    var scaleLinear = new BioimageIoSpec.Processing.ScaleLinear();
                    scaleLinear.axes = deserializeField(context, kwargs, "axes", BioimageIoSpec.Axis[].class, false);
                    scaleLinear.gain = deserializeField(context, kwargs, "gain", double[].class, false);
                    scaleLinear.offset = deserializeField(context, kwargs, "offset", double[].class, false);
                    return scaleLinear;
                case "scale_mean_variance":
                    var scaleMeanVariance = new BioimageIoSpec.Processing.ScaleMeanVariance();
                    ((BioimageIoSpec.Processing.ProcessingWithMode)scaleMeanVariance).mode = deserializeField(context, kwargs, "mode", BioimageIoSpec.Processing.ProcessingMode.class, false);
                    ((BioimageIoSpec.Processing.ProcessingWithMode)scaleMeanVariance).axes = deserializeField(context, kwargs, "axes", BioimageIoSpec.Axis[].class, false);
                    ((BioimageIoSpec.Processing.ProcessingWithMode)scaleMeanVariance).eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
                    scaleMeanVariance.referenceTensor = deserializeField(context, kwargs, "reference_tensor", String.class, null);
                    return scaleMeanVariance;
                case "scale_range":
                    var scaleRange = new BioimageIoSpec.Processing.ScaleRange();
                    ((BioimageIoSpec.Processing.ProcessingWithMode)scaleRange).mode = deserializeField(context, kwargs, "mode", BioimageIoSpec.Processing.ProcessingMode.class, false);
                    ((BioimageIoSpec.Processing.ProcessingWithMode)scaleRange).axes = deserializeField(context, kwargs, "axes", BioimageIoSpec.Axis[].class, false);
                    ((BioimageIoSpec.Processing.ProcessingWithMode)scaleRange).eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
                    scaleRange.referenceTensor = deserializeField(context, obj, "reference_tensor", String.class, null);
                    scaleRange.maxPercentile = deserializeField(context, kwargs, "max_percentile", Double.class, 0.0);
                    scaleRange.minPercentile = deserializeField(context, kwargs, "min_percentile", Double.class, 100.0);
                    return scaleRange;
                case "sigmoid":
                    return new BioimageIoSpec.Processing.Sigmoid();
                case "zero_mean_unit_variance":
                    var zeroMeanUnitVariance = new BioimageIoSpec.Processing.ZeroMeanUnitVariance();
                    ((BioimageIoSpec.Processing.ProcessingWithMode)zeroMeanUnitVariance).mode = deserializeField(context, kwargs, "mode", BioimageIoSpec.Processing.ProcessingMode.class, false);
                    ((BioimageIoSpec.Processing.ProcessingWithMode)zeroMeanUnitVariance).axes = deserializeField(context, kwargs, "axes", BioimageIoSpec.Axis[].class, false);
                    ((BioimageIoSpec.Processing.ProcessingWithMode)zeroMeanUnitVariance).eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
                    zeroMeanUnitVariance.mean = deserializeField(context, kwargs, "mean", double[].class, false);
                    zeroMeanUnitVariance.std = deserializeField(context, kwargs, "std", double[].class, false);
                    return zeroMeanUnitVariance;
                default:
                    var processing = new BioimageIoSpec.Processing(name);
                    processing.kwargs = kwargs == null ? Collections.emptyMap() : context.deserialize(kwargs, Map.class);
                    return processing;
            }
        }

    }

    static class AxesDeserializer implements JsonDeserializer<BioimageIoSpec.Axis[]> {

        @Override
        public BioimageIoSpec.Axis[] deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()) {
                var s = jsonElement.getAsString();
                BioimageIoSpec.Axis[] axes = new BioimageIoSpec.Axis[s.length()];
                for (int i = 0; i < axes.length; i++) {
                    axes[i] = new BioimageIoSpec.CharAxis(s.charAt(i));
                }
                return axes;
            }
            // todo: input or output???
            if (jsonElement.isJsonArray()) {
                var arr = jsonElement.getAsJsonArray();
                BioimageIoSpec.Axis[] axes = new BioimageIoSpec.Axis[arr.size()];
                for (int i = 0; i < axes.length; i++) {
                    var curr = arr.get(i);
                    if (curr.isJsonPrimitive()) {
                        axes[i] = new BioimageIoSpec.CharAxis(curr.getAsString().charAt(0));
                        continue;
                    }

                    var oj = curr.getAsJsonObject();
                    var id = deserializeField(context, oj, "id", String.class, "");
                    var desc = deserializeField(context, oj, "description", String.class, "");
                    BioimageIoSpec.Size size = deserializeSize(context, oj.get("size"), oj.get("scale"));
                    switch (oj.get("type").getAsString()) {
                        case "time":
                            axes[i] = new BioimageIoSpec.TimeInputAxis(
                                    id, desc,
                                    BioimageIoSpec.TimeUnit.valueOf(oj.get("unit").getAsString().toUpperCase()),
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
                            axes[i] = new BioimageIoSpec.ChannelAxis(
                                    id, desc,
                                    names
                            );
                            break;
                        case "index":
                            axes[i] = new BioimageIoSpec.IndexInputAxis(id, desc, size);
                            break;
                        case "space":
                            axes[i] = new BioimageIoSpec.SpaceInputAxis(
                                    id, desc,
                                    deserializeField(context, oj, "unit", String.class, ""),
                                    deserializeField(context, oj, "scale", Double.class, 1.0),
                                    size
                            );
                            break;
                        case "batch":
                            axes[i] = new BioimageIoSpec.BatchAxis(id, desc, deserializeField(context, oj, "size", Integer.class, 1));
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


    static class ShapeDeserializer implements JsonDeserializer<BioimageIoSpec.Shape> {

        @Override
        public BioimageIoSpec.Shape deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            if (json.isJsonArray()) {
                var shape = new BioimageIoSpec.Shape();
                shape.shape = context.deserialize(json, int[].class);
                return shape;
            }
            var obj = json.getAsJsonObject();
            if (obj.has("min") && obj.has("step")) {
                return context.deserialize(obj, BioimageIoSpec.Shape.ParameterizedInputShape.class);
            }
            if (obj.has("offset") && obj.has("scale")) {
                return context.deserialize(obj, BioimageIoSpec.Shape.ImplicitOutputShape.class);
            }
            throw new JsonParseException("Can't deserialize unknown shape: " + json);
        }

    }
}
