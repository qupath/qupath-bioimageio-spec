package qupath.bioimageio.spec;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import qupath.bioimageio.spec.tensor.BaseTensor;
import qupath.bioimageio.spec.tensor.InputTensor;
import qupath.bioimageio.spec.tensor.OutputTensor;
import qupath.bioimageio.spec.tensor.Tensors;

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

import static qupath.bioimageio.spec.FileDescription.NULL_FILE;


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

    private URI baseURI;
    private URI uri;

    private List<InputTensor> inputs;

    private List<String> testInputs;
    private List<String> testOutputs;

    // Should be in ISO 8601 format - but preserve string as it is found
    private String timestamp;

    private Weights.WeightsMap weights;

    private Map<?, ?> config;

    private List<OutputTensor> outputs;

    private List<Author> packagedBy;

    private ModelParent parent;

    // TODO: Handle run_mode properly
    private Map<?, ?> runMode;

    private List<String> sampleInputs;
    private List<String> sampleOutputs;


    /**
     * Parse a model from a file or directory.
     * This can either be a yaml file, or a directory that contains a yaml file representing the model.
     * @param file The file containing the YAML, or its parent directory.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static Model parse(File file) throws IOException {
        return parse(file.toPath());
    }

    /**
     * Parse a model from a path.
     * This can either represent a yaml file, or a directory that contains a yaml file representing the model.
     * @param path The path to the file containing the YAML, or its parent directory.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static Model parse(Path path) throws IOException {
        var pathYaml = findModelRdf(path);
        if (pathYaml == null) {
            throw new IOException("Can't find rdf.yaml from " + path);
        }

        try (var stream = Files.newInputStream(pathYaml)) {
            var model = parse(stream);
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
    public static Model parse(InputStream stream) throws IOException {
        try {
            // Important to use SafeConstructor to restrict potential classes that might be initiated
            // Note: we can't support -inf or inf (but can support -.inf or .inf)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, ?> map = yaml.load(stream);



            var builder = new GsonBuilder()
                    .serializeSpecialFloatingPointValues()
                    .setPrettyPrinting()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES) // convert snake case to camel case
                    .registerTypeAdapter(Model.class, new Model.Deserializer())
                    .registerTypeAdapter(Resource.class, new Resource.Deserializer())
                    .registerTypeAdapter(Dataset.class, new Dataset.Deserializer())
                    .registerTypeAdapter(Author.class, new Author.Deserializer())
                    .registerTypeAdapter(Weights.WeightsEntry.class, new Weights.WeightsEntryDeserializer())
                    .registerTypeAdapter(Weights.WeightsMap.class, new Weights.WeightsMapDeserializer())
                    .registerTypeAdapter(double[].class, new DoubleArrayDeserializer())
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss");

            var deserializers = Tensors.getDeserializers();
            for (var entry: deserializers.entrySet()) {
                builder.registerTypeAdapter(entry.getKey(), entry.getValue());
            }

            var gson = builder.create();
            var json = gson.toJson(map);

            return gson.fromJson(json, Model.class);
        } catch (Exception e) {
            throw new IOException(e);
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
        return weights.getMap().getOrDefault(key, null);
    }

    /**
     * Alternative to {@link #getWeights()}} using a string key.
     * @param key The query key string.
     * @return The weights value, or null if not found.
     */
    public Weights.ModelWeights getWeights(String key) {
        return getWeights(Weights.WeightsEntry.fromKey(key));
    }

    /**
     * The model from which this model is derived, e.g. by fine-tuning the weights.
     * @return The parent model.
     */
    public ModelParent getParent() {
        return parent;
    }

    /**
     * Timestamp in #<a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a>) format
     * with a few restrictions listed <a href="https://docs.python.org/3/library/datetime.html#datetime.datetime.fromisoformat">in the python docs</a>).
     * @return The timestamp.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * A field for custom configuration that can contain any keys not present in the RDF spec.
     * This means you should not store, for example, a GitHub repo URL in `config` since there is a `git_repo` field.
     * Keys in `config` may be very specific to a tool or consumer software. To avoid conflicting definitions,
     * it is recommended to wrap added configuration into a sub-field named with the specific domain or tool name,
     * for example:
     * ```yaml
     * config:
     *     bioimageio:  # here is the domain name
     *         my_custom_key: 3837283
     *             another_key:
     *                 nested: value
     *         imagej:       # config specific to ImageJ
     *             macro_dir: path/to/macro/file
     *     ```
     * If possible, please use <a href="https://en.wikipedia.org/wiki/Snake_case">snake_case</a> for keys in `config`.
     * You may want to list linked files additionally under `attachments` to include them when packaging a resource.
     * (Packaging a resource means downloading/copying important linked files and creating a ZIP archive that contains
     * an altered rdf.yaml file with local references to the downloaded files.)
     * @return the config
     */
    public Map<?, ?> getConfig() {
        return config;
    }

    /**
     * Get the input tensors
     * @return the list of inputs
     */
    public List<InputTensor> getInputs() {
        return toUnmodifiableList(inputs);
    }

    /**
     * Get the output tensors
     * @return the list of output tensors
     */
    public List<OutputTensor> getOutputs() {
        return toUnmodifiableList(outputs);
    }

    /**
     * Test input tensors compatible with the `inputs` description for a **single test case**.
     * This means if your model has more than one input, you should provide one URL/relative path for each input.
     * Each test input should be a file with an ndarray in
     * h<a href="ttps://numpy.org/doc/stable/reference/generated/numpy.lib.format.html#module-numpy.lib.format.">numpy.lib file format</a>
     * The extension must be '.npy'.
     * @return The test inputs.
     */
    public List<String> getTestInputs() {
        var ti = testInputs;
        if (ti.isEmpty() && isFormatNewerThan("0.5")) {
            ti = inputs.stream()
                    .map(BaseTensor::getTestTensor)
                    .map(ofd -> ofd.orElse(NULL_FILE).source())
                    .collect(Collectors.toList());
        }
        return toUnmodifiableList(ti);
    }

    /**
     * Test output tensors compatible with the `output` description for a **single test case**.
     * This means if your model has more than one output, you should provide one URL/relative path for each output.
     * Each test output should be a file with an ndarray in
     * h<a href="ttps://numpy.org/doc/stable/reference/generated/numpy.lib.format.html#module-numpy.lib.format.">numpy.lib file format</a>
     * The extension must be '.npy'.
     * @return The test outputs.
     */
    public List<String> getTestOutputs() {
        var to = testOutputs;
        if (to.isEmpty() && isFormatNewerThan("0.5")) {
            to = outputs.stream()
                    .map(BaseTensor::getTestTensor)
                    .map(ofd -> ofd.orElse(NULL_FILE).source())
                    .collect(Collectors.toList());
        }
        return toUnmodifiableList(to);
    }

    /**
     * URLs/relative paths to sample outputs corresponding to the `sample_inputs`.
     * @return the sample inputs.
     */
    public List<String> getSampleInputs() {
        var si = sampleInputs;
        if (si.isEmpty() && isFormatNewerThan("0.5")) {
            si = inputs.stream()
                    .map(BaseTensor::getTestTensor)
                    .map(ofd -> ofd.orElse(NULL_FILE).source())
                    .collect(Collectors.toList());
        }
        return toUnmodifiableList(si);
    }

    /**
     * URLs/relative paths to sample outputs corresponding to the `sample_outputs`.
     * @return the sample outputs.
     */
    public List<String> getSampleOutputs() {
        var so = sampleOutputs;
        if (so.isEmpty() && isFormatNewerThan("0.5")) {
            so = outputs.stream()
                    .map(BaseTensor::getTestTensor)
                    .map(ofd -> ofd.orElse(NULL_FILE).source())
                    .collect(Collectors.toList());
        }
        return toUnmodifiableList(so);
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

    /**
     * Deserialize a field from a JSON object.
     * @param <T> The type of the field.
     * @param context The context used for deserialization.
     * @param obj The JSON object that contains the field.
     * @param name The name of the field.
     * @param typeOfT The type of the field.
     * @return A parsed T object.
     * @throws IllegalArgumentException if doStrict is true and the field is not found
     */
    public static <T> T deserializeField(JsonDeserializationContext context, JsonObject obj, String name, Type typeOfT, T defaultValue) {
        if (obj.has(name)) {
            return ensureUnmodifiable(context.deserialize(obj.get(name), typeOfT));
        }
        return ensureUnmodifiable(defaultValue);
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
                List<Path> yamlFiles = pathStream.filter(Model::isYamlPath).toList();
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
            try (var fs = FileSystems.newFileSystem(path, (ClassLoader) null)) {
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


    static boolean isYamlPath(Path path) {
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

        if (model.isFormatNewerThan("0.5.0")) {
            model.testInputs = List.of();
            model.testOutputs = List.of();
            model.timestamp = "";
        } else {
            // now part of the tensor spec:
            model.testInputs = deserializeField(context, obj, "test_inputs", parameterizedListType(String.class), doStrict);
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

}
