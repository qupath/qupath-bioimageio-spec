package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.bioimageio.spec.axes.Axis;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import static qupath.bioimageio.spec.Utils.deserializeField;

/**
 * Base class for pre- and post-processing operations.
 * See the model spec for details.
 */
public class Processing {

    /**
     * Requested mode for pre- or post-processing.
     */
    public enum ProcessingMode {FIXED, PER_DATASET, PER_SAMPLE}

    private final String name;
    Map<String, Object> kwargs;

    Processing(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<String, ?> getKwargs() {
        return kwargs == null ? Collections.emptyMap() : Collections.unmodifiableMap(kwargs);
    }

    /**
     * A binarize operation.
     */
    public static class Binarize extends Processing {

        Binarize() {
            super("binarize");
        }

        double threshold = Double.NaN;

        public double getThreshold() {
            return threshold;
        }

    }

    /**
     * A clipping operation.
     */
    public static class Clip extends Processing {

        double min;
        double max;

        Clip() {
            super("clip");
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

    }

    /**
     * A linear scaling operation.
     */
    public static class ScaleLinear extends Processing {

        double[] gain;
        double[] offset;
        Axis[] axes;

        ScaleLinear() {
            super("scale_linear");
        }

        public double[] getGain() {
            return gain == null ? null : gain.clone();
        }

        public double[] getOffset() {
            return offset == null ? null : offset.clone();
        }

        public Axis[] getAxes() {
            return axes;
        }

    }

    /**
     * A sigmoid transofmration.
     */
    public static class Sigmoid extends Processing {

        Sigmoid() {
            super("sigmoid");
        }

    }

    protected abstract static class ProcessingWithMode extends Processing {

        ProcessingMode mode = ProcessingMode.PER_SAMPLE;
        Axis[] axes;
        double eps = 1e-6;

        ProcessingWithMode(String name) {
            super(name);
        }

        public double getEps() {
            return eps;
        }

        public ProcessingMode getMode() {
            return mode;
        }

        public Axis[] getAxes() {
            return axes;
        }

    }

    /**
     * A scaling operation to the mean and variance of a reference tensor.
     */
    public static class ScaleMeanVariance extends ProcessingWithMode {

        String referenceTensor;

        ScaleMeanVariance() {
            super("scale_mean_variance");
        }

        public String getReferenceTensor() {
            return referenceTensor;
        }

    }

    /**
     * A scaling operation to a reference tensor.
     */
    public static class ScaleRange extends ProcessingWithMode {

        String referenceTensor; // TODO: Figure out whether to use this somehow

        double minPercentile = 0.0;
        double maxPercentile = 100.0;

        ScaleRange() {
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

    /**
     * A scaling operation to zero mean and unit variqnce.
     */
    public static class ZeroMeanUnitVariance extends ProcessingWithMode {

        double[] mean;
        double[] std;

        ZeroMeanUnitVariance() {
            super("zero_mean_unit_variance");
        }

        public double[] getMean() {
            return mean == null ? null : mean.clone();
        }

        public double[] getStd() {
            return std == null ? null : std.clone();
        }

    }

    public static class ProcessingModeDeserializer implements JsonDeserializer<ProcessingMode> {
        Logger logger = LoggerFactory.getLogger(Processing.class);
        @Override
        public Processing.ProcessingMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var s = json.getAsString();
            for (var mode : Processing.ProcessingMode.values()) {
                if (s.equals(mode.name()))
                    return mode;
            }

            for (var mode : Processing.ProcessingMode.values()) {
                if (s.equalsIgnoreCase(mode.name()))
                    return mode;
            }

            logger.warn("Unknown processing mode: {}", s);
            return null;
        }

    }

    public static class ProcessingDeserializer implements JsonDeserializer<Processing> {

        @Override
        public Processing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var obj = json.getAsJsonObject();
            var name = obj.has("name") ? obj.get("name").getAsString() : obj.get("id").getAsString();
            JsonObject kwargs = deserializeField(context, obj, "kwargs", JsonObject.class, null);;
            switch (name) {
                case "binarize":
                    var binarize = new Processing.Binarize();
                    binarize.threshold = deserializeField(context, kwargs, "threshold", Double.class, true);
                    return binarize;
                case "clip":
                    var clip = new Processing.Clip();
                    clip.min = deserializeField(context, kwargs, "min", Double.class, Double.NEGATIVE_INFINITY);
                    clip.max = deserializeField(context, kwargs, "max", Double.class, Double.POSITIVE_INFINITY);
                    return clip;
                case "scale_linear":
                    var scaleLinear = new Processing.ScaleLinear();
                    scaleLinear.axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
                    scaleLinear.gain = deserializeField(context, kwargs, "gain", double[].class, false);
                    scaleLinear.offset = deserializeField(context, kwargs, "offset", double[].class, false);
                    return scaleLinear;
                case "scale_mean_variance":
                    var scaleMeanVariance = new Processing.ScaleMeanVariance();
                    scaleMeanVariance.mode = deserializeField(context, kwargs, "mode", Processing.ProcessingMode.class, false);
                    scaleMeanVariance.axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
                    scaleMeanVariance.eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
                    scaleMeanVariance.referenceTensor = deserializeField(context, kwargs, "reference_tensor", String.class, null);
                    return scaleMeanVariance;
                case "scale_range":
                    var scaleRange = new Processing.ScaleRange();
                    scaleRange.mode = deserializeField(context, kwargs, "mode", Processing.ProcessingMode.class, false);
                    scaleRange.axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
                    scaleRange.eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
                    scaleRange.referenceTensor = deserializeField(context, obj, "reference_tensor", String.class, null);
                    scaleRange.maxPercentile = deserializeField(context, kwargs, "max_percentile", Double.class, 0.0);
                    scaleRange.minPercentile = deserializeField(context, kwargs, "min_percentile", Double.class, 100.0);
                    return scaleRange;
                case "sigmoid":
                    return new Processing.Sigmoid();
                case "zero_mean_unit_variance":
                    var zeroMeanUnitVariance = new Processing.ZeroMeanUnitVariance();
                    zeroMeanUnitVariance.mode = deserializeField(context, kwargs, "mode", Processing.ProcessingMode.class, false);
                    zeroMeanUnitVariance.axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
                    zeroMeanUnitVariance.eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
                    zeroMeanUnitVariance.mean = deserializeField(context, kwargs, "mean", double[].class, false);
                    zeroMeanUnitVariance.std = deserializeField(context, kwargs, "std", double[].class, false);
                    return zeroMeanUnitVariance;
                default:
                    var processing = new Processing(name);
                    processing.kwargs = kwargs == null ? Collections.emptyMap() : context.deserialize(kwargs, Map.class);
                    return processing;
            }
        }

    }

}
