package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static qupath.bioimageio.spec.BioimageIoSpec.deserializeField;
import static qupath.bioimageio.spec.BioimageIoSpec.parameterizedListType;
import static qupath.bioimageio.spec.BioimageIoSpec.toUnmodifiableList;

public class BioImageIoSpec05 {
    private final static Logger logger = LoggerFactory.getLogger(BioImageIoSpec05.class);

    /**
     * Resource based on the main model spec.
     * This extends {@link BioimageIoSpec.BioimageIoResource} to provide more information
     * relevant for to machine learning models.
     */
    public static class BioimageIoModel05 extends BioimageIoSpec.BioimageIoResource {

        URI baseURI;
        URI uri;

        private List<InputTensor05> inputs;

        // Should be in ISO 8601 format - but preserve string as it is found
        private String timestamp;

        private BioimageIoSpec.WeightsMap weights;

        private Map<?, ?> config;

        private List<OutputTensor05> outputs;

        @SerializedName("packaged_by")
        private List<BioimageIoSpec.Author> packagedBy;

        private BioimageIoSpec.ModelParent parent;

        // TODO: Handle run_mode properly
        @SerializedName("run_mode")
        private Map<?, ?> runMode;

        @SerializedName("sample_inputs")
        private List<String> sampleInputs;
        @SerializedName("sample_outputs")
        private List<String> sampleOutputs;


        /**
         * Get the base URI, providing the location of the model.
         * This is typically a URI representing the downloaded model directory
         * (the parent of {@link #getURI()}).
         * This may also represent a zipped file, if the model is read without unzipping
         * the contents.
         * @return
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
         * @return
         */
        public URI getURI() {
            return uri;
        }

        /**
         * Get a map view of the weights. It's generally better to use {@link #getWeights()}.
         * @return
         */
        public Map<String, BioimageIoSpec.ModelWeights> getWeights() {
            return weights == null ? Collections.emptyMap() : weights.withStringKeys();
        }

        /**
         * Alternative to {@link #getWeights()} that corrects for keys that have been renamed.
         * @param key
         * @return
         */
        public BioimageIoSpec.ModelWeights getWeights(BioimageIoSpec.WeightsEntry key) {
            if (weights == null || key == null)
                return null;
            return weights.getMap().getOrDefault(key, null);
        }

        /**
         * Alternative to {@link #getWeights(BioimageIoSpec.WeightsEntry)} using a string key.
         * @param key
         * @return
         */
        public BioimageIoSpec.ModelWeights getWeights(String key) {
            return getWeights(BioimageIoSpec.WeightsEntry.fromKey(key));
        }

        public BioimageIoSpec.ModelParent getParent() {
            return parent;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public Map<?, ?> getConfig() {
            return config;
        }

        public List<InputTensor05> getInputs() {
            return toUnmodifiableList(inputs);
        }

        public List<OutputTensor05> getOutputs() {
            return toUnmodifiableList(outputs);
        }

        public List<String> getSampleInputs() {
            return toUnmodifiableList(sampleInputs);
        }

        public List<String> getSampleOutputs() {
            return toUnmodifiableList(sampleOutputs);
        }

    }

    public static class AxisBase {
        private String id;
        private String description;
    }

    public static class BatchAxis extends AxisBase {
        private int size;
        private double scale = 1.0;
        private boolean concatenable = true;
        private String unit = null;
    }

    public static class ChannelAxis extends AxisBase {
        private List<String> channel_names;
        public int size() {
            return channel_names.size();
        }
    }

    public static class IndexAxisBase extends AxisBase {
        private double scale = 1.0;
        private String unit = null;
    }

    public static class IndexInputAxis {
        private boolean concatenable;
    }

    public static class IndexOutputAxis {
        private boolean concatenable;
        private Size size;
    }

    public static class Size {
        //     todo: handle ints or SizeReference of DataDependentSize...???
    }

    public static class TimeAxisBase extends AxisBase {
        private final String type = "time";
        private TimeUnit unit;
        private double scale = 1;
    }

    public static class TimeInputAxis extends TimeAxisBase {
        private boolean concatenable = false;
    }

    public static enum TimeUnit {
            ATTOSECOND,
            CENTISECOND,
            DAY,
            DECISECOND,
            EXASECOND,
            FEMTOSECOND,
            GIGASECOND,
            HECTOSECOND,
            HOUR,
            KILOSECOND,
            MEGASECOND,
            MICROSECOND,
            MILLISECOND,
            MINUTE,
            NANOSECOND,
            PETASECOND,
            PICOSECOND,
            SECOND,
            TERASECOND,
            YOCTOSECOND,
            YOTTASECOND,
            ZEPTOSECOND,
            ZETTASECOND;
    }

    public static enum SpaceUnit {
            ATTOMETER,
            ANGSTROM,
            CENTIMETER,
            DECIMETER,
            EXAMETER,
            FEMTOMETER,
            FOOT,
            GIGAMETER,
            HECTOMETER,
            INCH,
            KILOMETER,
            MEGAMETER,
            METER,
            MICROMETER,
            MILE,
            MILLIMETER,
            NANOMETER,
            PARSEC,
            PETAMETER,
            PICOMETER,
            TERAMETER,
            YARD,
            YOCTOMETER,
            YOTTAMETER,
            ZEPTOMETER,
            ZETTAMETER;
    }

    public static class SpaceAxisBase extends AxisBase {
        private String id;
        private SpaceUnit unit;
        private double scale = 1.0;
    }


    private static class BaseTensor05 {

        private AxisBase[] axes;
        @SerializedName("data_type")
        private String dataType;
        private String name;

        private BioimageIoSpec.Shape shape;

        @SerializedName("data_range")
        private double[] dataRange;

        public AxisBase[] getAxes() {
            return axes;
        }

        public String getDataType() {
            return dataType;
        }

        public String getName() {
            return name;
        }

        public BioimageIoSpec.Shape getShape() {
            return shape;
        }

        public double[] getDataRange() {
            return dataRange == null ? null : dataRange.clone();
        }

    }

    /**
     * Model output, including shape, axes, halo, datatype and postprocessing.
     */
    public static class OutputTensor05 extends BaseTensor05 {

        private List<BioimageIoSpec.Processing> postprocessing;

        private int[] halo;

        public List<BioimageIoSpec.Processing> getPostprocessing() {
            return toUnmodifiableList(postprocessing);
        }

        public int[] getHalo() {
            return halo == null ? new int[0] : halo.clone();
        }

        @Override
        public String toString() {
            return "Output tensor [" + getShape() + ", postprocessing steps=" + getPostprocessing().size() + "]";
        }
    }


    /**
     * Model input, including shape, axes, datatype and preprocessing.
     */
    public static class InputTensor05 extends BaseTensor05 {

        private List<BioimageIoSpec.Processing> preprocessing;

        public List<BioimageIoSpec.Processing> getPreprocessing() {
            return toUnmodifiableList(preprocessing);
        }

        @Override
        public String toString() {
            return "Input tensor [" + getShape() + ", processing steps=" + getPreprocessing().size() + "]";
        }

    }


    static class ModelDeserializer05 implements JsonDeserializer<BioimageIoModel05> {

        @Override
        public BioimageIoModel05 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var model = new BioimageIoModel05();

            var obj = json.getAsJsonObject();

            BioimageIoSpec.deserializeResourceFields(model, obj, context, true);
            deserializeModelFields05(model, obj, context, true);

            return model;
        }
    }


    private static void deserializeModelFields05(BioimageIoModel05 model, JsonObject obj, JsonDeserializationContext context, boolean doStrict) {
        model.inputs = deserializeField(context, obj, "inputs", parameterizedListType(InputTensor05.class), doStrict);

        // model.timestamp = deserializeField(context, obj, "timestamp", String.class, doStrict);

        model.weights = deserializeField(context, obj, "weights", BioimageIoSpec.WeightsMap.class, doStrict);

        model.config = deserializeField(context, obj, "config", Map.class, Collections.emptyMap());

        model.outputs = deserializeField(context, obj, "outputs", parameterizedListType(OutputTensor05.class), Collections.emptyList());

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



    public static class ScaleMeanVariance05 extends BioimageIoSpec.Processing.ProcessingWithMode05 {

        String referenceTensor;

        ScaleMeanVariance05() {
            super("scale_mean_variance");
        }

        public String getReferenceTensor() {
            return referenceTensor;
        }

    }


    public static class ScaleRange05 extends BioimageIoSpec.Processing.ProcessingWithMode05 {

        String referenceTensor; // TODO: Figure out whether to use this somehow

        double minPercentile = 0.0;
        double maxPercentile = 100.0;

        ScaleRange05() {
            super("scale_range");
        }

        public double getMinPercentile() {
            return minPercentile;
        }

        public double getMaxPercentile() {
            return maxPercentile;
        }

        public String getReferenceTensor() {
            return referenceTensor;
        }


    }


    public static class ZeroMeanUnitVariance05 extends BioimageIoSpec.Processing.ProcessingWithMode05 {

        double[] mean;
        double[] std;

        ZeroMeanUnitVariance05() {
            super("zero_mean_unit_variance");
        }


        public double[] getMean() {
            return mean == null ? null : mean.clone();
        }

        public double[] getStd() {
            return std == null ? null : std.clone();
        }

    }
}
