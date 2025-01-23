package qupath.bioimageio.spec;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import qupath.bioimageio.spec.tensor.BaseTensor;
import qupath.bioimageio.spec.tensor.InputTensor;
import qupath.bioimageio.spec.tensor.OutputTensor;
import qupath.bioimageio.spec.tensor.Processing;
import qupath.bioimageio.spec.tensor.Shape;
import qupath.bioimageio.spec.tensor.TensorDataDescription;
import qupath.bioimageio.spec.tensor.axes.Axis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Resource based on the main model spec.
 * This extends {@link Resource} to provide more information
 * relevant for to machine learning models.
 */
public class Model extends Resource {
    private static final Logger logger = LoggerFactory.getLogger(Model.class);

    public void setBaseURI(URI baseURI) {
        this.baseURI = baseURI;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    URI baseURI;
    URI uri;

    List<InputTensor> inputs;

    @SerializedName("test_inputs")
    List<String> testInputs;
    @SerializedName("test_outputs")
    List<String> testOutputs;

    // Should be in ISO 8601 format - but preserve string as it is found
    String timestamp;

    Weights.WeightsMap weights;

    Map<?, ?> config;

    List<OutputTensor> outputs;

    @SerializedName("packaged_by")
    List<Author> packagedBy;

    ModelParent parent;

    // TODO: Handle run_mode properly
    @SerializedName("run_mode")
    Map<?, ?> runMode;

    @SerializedName("sample_inputs")
    List<String> sampleInputs;
    @SerializedName("sample_outputs")
    List<String> sampleOutputs;


    /**
     * Parse a model from a file or directory.
     * This can either be a yaml file, or a directory that contains a yaml file representing the model.
     * @param file The file containing the YAML, or its parent directory.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static Model parseModel(File file) throws IOException {
        return parseModel(file.toPath());
    }

    /**
     * Parse a model from a path.
     * This can either represent a yaml file, or a directory that contains a yaml file representing the model.
     * @param path The path to the file containing the YAML, or its parent directory.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static Model parseModel(Path path) throws IOException {

        var pathYaml = findModelRdf(path);
        if (pathYaml == null) {
            throw new IOException("Can't find rdf.yaml from " + path);
        }

        try (var stream = Files.newInputStream(pathYaml)) {
            var model = parseModel(stream);
            if (model != null) {
                model.setBaseURI(pathYaml.getParent().toUri());
                model.setUri(pathYaml.toUri());
            }
            return model;
        }
    }

    /**
     * Parse a model from an input stream.
     * Note that {@link Model#getBaseURI()} will return null in this case, because the base URI
     * is unknown.
     * @param stream A stream of YAML.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static Model parseModel(InputStream stream) throws IOException {

        try {
            // Important to use SafeConstructor to restrict potential classes that might be initiated
            // Note: we can't support -inf or inf (but can support -.inf or .inf)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, ?> map = yaml.load(stream);

            var builder = new GsonBuilder()
                    .serializeSpecialFloatingPointValues()
                    .setPrettyPrinting()
                    .registerTypeAdapter(Model.class, new Model.Deserializer())
                    .registerTypeAdapter(Resource.class, new Resource.Deserializer())
                    .registerTypeAdapter(Dataset.class, new Dataset.Deserializer())
                    .registerTypeAdapter(double[].class, new DoubleArrayDeserializer())
                    .registerTypeAdapter(Author.class, new Author.Deserializer())
                    .registerTypeAdapter(Weights.WeightsEntry.class, new Weights.WeightsEntryDeserializer())
                    .registerTypeAdapter(Weights.WeightsMap.class, new Weights.WeightsMapDeserializer())
                    .registerTypeAdapter(Shape.class, new Shape.Deserializer())
                    .registerTypeAdapter(Processing.class, new Processing.ProcessingDeserializer())
                    .registerTypeAdapter(Processing.ProcessingMode.class, new Processing.ProcessingModeDeserializer())
                    .registerTypeAdapter(Axis[].class, new Axis.AxesDeserializer())
                    .registerTypeAdapter(TensorDataDescription.class, new TensorDataDescription.Deserializer())
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss");



            var gson = builder.create();

            var json = gson.toJson(map);

            return gson.fromJson(json, Model.class);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    /**
     * List of file names that may contain the model.
     * Names should be checked in order, with preference given to the first that is found.
     */
    static final List<String> MODEL_NAMES = List.of("model.yaml", "model.yml", "rdf.yaml", "rdf.yml");

    static Path findModelRdf(Path path) throws IOException {
        return findRdf(path, MODEL_NAMES);
    }

    private static Path findRdf(Path path, Collection<String> names) throws IOException {
        if (isYamlPath(path)) {
            if (names.isEmpty())
                return path;
            else {
                var name = path.getFileName().toString().toLowerCase();
                if (names.contains(name) || name.startsWith("model") || name.startsWith("rdf"))
                    return path;
                return null;
            }
        }


        if (Files.isDirectory(path)) {
            // Check directory
            try (Stream<Path> pathStream = Files.list(path)) {
                List<Path> yamlFiles = pathStream.filter(Model::isYamlPath).collect(Collectors.toList());
                if (yamlFiles.isEmpty())
                    return null;
                if (yamlFiles.size() == 1)
                    return yamlFiles.get(0);
                for (var name : MODEL_NAMES) {
                    var modelFile = yamlFiles.stream()
                            .filter(p -> p.getFileName().toString().equalsIgnoreCase(name))
                            .findFirst()
                            .orElse(null);
                    if (modelFile != null)
                        return modelFile;
                }
            }
        } else if (path.toAbsolutePath().toString().toLowerCase().endsWith(".zip")) {
            // Check zip file
            try (var fs = FileSystems.newFileSystem(path, null)) {
                for (var dir : fs.getRootDirectories()) {
                    for (var name : MODEL_NAMES) {
                        var p = dir.resolve(name);
                        if (Files.exists(p))
                            return p;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isYamlPath(Path path) {
        if (Files.isRegularFile(path)) {
            var name = path.getFileName().toString().toLowerCase();
            return name.endsWith(".yaml") || name.endsWith(".yml");
        }
        return false;
    }

    static Type parameterizedListType(Type typeOfList) {
        return TypeToken.getParameterized(List.class, typeOfList).getType();
    }
    /**
     * Deal with the awkwardness of -inf and inf instead of .inf and .inf.
     * This otherwise caused several failures for data range.
     */
    static class DoubleArrayDeserializer implements JsonDeserializer<double[]> {
        private static final Logger logger = LoggerFactory.getLogger(DoubleArrayDeserializer.class);

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



    /**
     * Get the base URI, providing the location of the model.
     * This is typically a URI representing the downloaded model directory
     * (the parent of {@link #getURI()}).
     * This may also represent a zipped file, if the model is read without unzipping
     * the contents.
     * @return The URI to the model file.
     */
    public URI getBaseURI() {
        if (baseURI != null)
            return baseURI;
        if (uri != null)
            return uri.resolve("..");
        return null;
    }

    /**
     * Get the URI providing the location of the model.
     * This is typically a URI representing the yaml file for the model.
     * @return The URI to the model file.
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Get a map view of the weights. It's generally better to use {@link #getWeights(Weights.WeightsEntry key)}.
     * @return a map view of the weights.
     */
    public Map<String, Weights.ModelWeights> getWeights() {
        return weights == null ? Collections.emptyMap() : weights.withStringKeys();
    }

    /**
     * Alternative to {@link #getWeights()} that corrects for keys that have been renamed.
     * @param key The query key.
     * @return The weights value, or null if not found.
     */
    public Weights.ModelWeights getWeights(Weights.WeightsEntry key) {
        if (weights == null || key == null)
            return null;
        return weights.map.getOrDefault(key, null);
    }

    /**
     * Alternative to {@link #getWeights()}} using a string key.
     * @param key The query key string.
     * @return The weights value, or null if not found.
     */
    public Weights.ModelWeights getWeights(String key) {
        return getWeights(Weights.WeightsEntry.fromKey(key));
    }

    public ModelParent getParent() {
        return parent;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Map<?, ?> getConfig() {
        return config;
    }

    public List<InputTensor> getInputs() {
        return toUnmodifiableList(inputs);
    }

    public List<OutputTensor> getOutputs() {
        return toUnmodifiableList(outputs);
    }

    public List<String> getTestInputs() {
        var ti = testInputs;
        if (ti == null && isNewerThan("0.5")) {
            ti = inputs.stream()
                    .map(BaseTensor::getTestTensor)
                    .map(FileDescr::getSource)
                    .collect(Collectors.toList());
        }
        return toUnmodifiableList(ti);
    }

    public List<String> getTestOutputs() {
        var to = testOutputs;
        if (to == null && isNewerThan("0.5")) {
            to = outputs.stream()
                    .map(BaseTensor::getTestTensor)
                    .map(FileDescr::getSource)
                    .collect(Collectors.toList());
        }
        return toUnmodifiableList(to);
    }

    public List<String> getSampleInputs() {
        var si = sampleInputs;
        if (si == null && isNewerThan("0.5")) {
            si = inputs.stream()
                    .map(BaseTensor::getTestTensor)
                    .map(FileDescr::getSource)
                    .collect(Collectors.toList());
        }
        return toUnmodifiableList(si);
    }

    public List<String> getSampleOutputs() {
        var so = sampleOutputs;
        if (so == null && isNewerThan("0.5")) {
            so = outputs.stream()
                    .map(BaseTensor::getTestTensor)
                    .map(FileDescr::getSource)
                    .collect(Collectors.toList());
        }
        return toUnmodifiableList(so);
    }




    static class Deserializer implements JsonDeserializer<Model> {

        @Override
        public Model deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var model = new Model();

            var obj = json.getAsJsonObject();

            deserializeResourceFields(model, obj, context, true);
            deserializeModelFields(model, obj, context, true);

            return model;
        }
    }


    private static void deserializeModelFields(Model model, JsonObject obj, JsonDeserializationContext context, boolean doStrict) {
        model.inputs = deserializeField(context, obj, "inputs", parameterizedListType(InputTensor.class), doStrict);
        List<BaseTensor> tensors = new ArrayList<>(model.inputs);
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

        model.weights = deserializeField(context, obj, "weights", Weights.WeightsMap.class, doStrict);

        model.config = deserializeField(context, obj, "config", Map.class, Collections.emptyMap());

        model.outputs = deserializeField(context, obj, "outputs", parameterizedListType(OutputTensor.class), Collections.emptyList());
        tensors.addAll(model.outputs);
        for (var tensor: model.outputs) {
            tensor.validate(tensors);
        }
        model.packagedBy = deserializeField(context, obj, "packaged_by", parameterizedListType(Author.class), Collections.emptyList());

        model.parent = deserializeField(context, obj, "parent", ModelParent.class, null);

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
     * Ensure the input is an unmodifiable list, or empty list if null.
     * Note that OpenJDK implementation is expected to return its input if already unmodifiable.
     * @param <T> The type of list objects.
     * @param list The input list.
     * @return An unmodifiable list.
     */
    public static <T> List<T> toUnmodifiableList(List<T> list) {
        return list == null || list.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(list);
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
    public static <T> T deserializeField(JsonDeserializationContext context, JsonObject obj, String name, Type typeOfT, boolean doStrict) throws IllegalArgumentException {
        if (doStrict && !obj.has(name))
            throw new IllegalArgumentException("Required field " + name + " not found");
        return deserializeField(context, obj, name, typeOfT, null);
    }

    public static <T> T deserializeField(JsonDeserializationContext context, JsonObject obj, String name, Type typeOfT, T defaultValue) {
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





}
