package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.bioimageio.spec.tensor.BaseTensor;
import qupath.bioimageio.spec.tensor.InputTensor;
import qupath.bioimageio.spec.tensor.OutputTensor;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static qupath.bioimageio.parsing.Parsing.parameterizedListType;
import static qupath.bioimageio.spec.Utils.deserializeField;
import static qupath.bioimageio.spec.Utils.toUnmodifiableList;

/**
 * Resource based on the main model spec.
 * This extends {@link BioimageIoResource} to provide more information
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
     * Alternative to {@link #getWeights(WeightsEntry)} using a string key.
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
        return toUnmodifiableList(testInputs);
    }

    public List<String> getTestOutputs() {
        return toUnmodifiableList(testOutputs);
    }

    public List<String> getSampleInputs() {
        return toUnmodifiableList(sampleInputs);
    }

    public List<String> getSampleOutputs() {
        return toUnmodifiableList(sampleOutputs);
    }



    public static class Deserializer implements JsonDeserializer<Model> {

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



}
