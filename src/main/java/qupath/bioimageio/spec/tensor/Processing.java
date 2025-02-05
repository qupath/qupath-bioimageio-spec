/*
 * Copyright 2025 University of Edinburgh
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package qupath.bioimageio.spec.tensor;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.bioimageio.spec.tensor.axes.Axis;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static qupath.bioimageio.spec.Model.deserializeField;

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
    private Map<String, Object> kwargs;

    Processing(String name) {
        this.name = name;
    }


    /**
     * Get the type adapters/deserializers necessary for any Processing objects.
     * @return A map of type to deserializer for that type.
     */
    public static Map<Class<?>, JsonDeserializer<?>> getDeserializers() {
        Map<Class<?>, JsonDeserializer<?>> map = new HashMap<>();
        map.put(Processing.class, new ProcessingDeserializer());
        map.put(ProcessingMode.class, new ProcessingModeDeserializer());
        return map;
    }

    /**
     * Get the name of this operation
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the keyword arguments for the operation
     * @return named arguments of uncertain type
     */
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

        /**
         * Get the threshold for binarization
         * @return the numeric threshold
         */
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

        /**
         * The minimum value
         * @return the minimum
         */
        public double getMin() {
            return min;
        }

        /**
         * The maximum value
         * @return the maximum
         */
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

        /**
         * Get the multiplicative scaling factor
         * @return the scale
         */
        public double[] getGain() {
            return gain == null ? null : gain.clone();
        }

        /**
         * Get the additive offset
         * @return the offset
         */
        public double[] getOffset() {
            return offset == null ? null : offset.clone();
        }

        /**
         * Get the axes this applies to.
         * @return the axes
         */
        public Axis[] getAxes() {
            return axes == null ? null : axes.clone();
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

    /**
     * A processing mode that operates optionally on different sets of data.
     * For example, per_dataset or per_sample.
     */
    protected abstract static class ProcessingWithMode extends Processing {

        ProcessingMode mode = ProcessingMode.PER_SAMPLE;
        Axis[] axes;
        double eps = 1e-6;

        ProcessingWithMode(String name) {
            super(name);
        }

        /**
         * Epsilon for numeric stability:
         * out  = (tensor - mean) / (std + eps) * (ref_std + eps) + ref_mean.
         * @return The epsilon value.
         */
        public double getEps() {
            return eps;
        }

        /**
         * Mode for computing processing. Not all are supported for all operations, but here are some options:
         * |     mode    |             description              |
         * | ----------- | ------------------------------------ |
         * |   fixed     | Fixed values                         |
         * | per_dataset | Compute for the entire dataset       |
         * | per_sample  | Compute for each sample individually |
         */
        public ProcessingMode getMode() {
            return mode;
        }

        /**
         * The subset of axes to scale jointly.
         * For example xy to normalize the two image axes for 2d data jointly.
         * Default: scale all non-batch axes jointly.
         * @return the subset of axes to scale jointly.
         */
        public Axis[] getAxes() {
            return axes.clone();
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

        /**
         * The tensor to match mean and variance to.
         */
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

        /**
         * The minimum percentile.
         * @return the min percentile
         */
        public double getMinPercentile() {
            return minPercentile;
        }
        /**
         * The maximum percentile.
         * @return the max percentile
         */
        public double getMaxPercentile() {
            return maxPercentile;
        }

        /**
         * The reference tensor, which we use for range scaling.
         * @return the reference tensor
         */
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

        /**
         * The mean value(s) to use for `mode: fixed`.
         * For example `[1.1, 2.2, 3.3]` in the case of a 3 channel image with `axes: xy`.
         * @return the mean value(s)
         */
        public double[] getMean() {
            return mean == null ? null : mean.clone();
        }

        /**
         * The standard deviation values to use for `mode: fixed`. Analogous to mean.
         * @return the std values.
         */
        public double[] getStd() {
            return std == null ? null : std.clone();
        }

    }

    static class ProcessingModeDeserializer implements JsonDeserializer<ProcessingMode> {
        private static final Logger logger = LoggerFactory.getLogger(Processing.class);
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

    static class ProcessingDeserializer implements JsonDeserializer<Processing> {

        @Override
        public Processing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var obj = json.getAsJsonObject();
            var name = obj.has("name") ? obj.get("name").getAsString() : obj.get("id").getAsString();
            JsonObject kwargs = deserializeField(context, obj, "kwargs", JsonObject.class, null);
            return switch (name) {
                case "binarize" -> {
                    var binarize = new Binarize();
                    binarize.threshold = deserializeField(context, kwargs, "threshold", Double.class, true);
                    yield binarize;
                }
                case "clip" -> {
                    var clip = new Clip();
                    clip.min = deserializeField(context, kwargs, "min", Double.class, Double.NEGATIVE_INFINITY);
                    clip.max = deserializeField(context, kwargs, "max", Double.class, Double.POSITIVE_INFINITY);
                    yield clip;
                }
                case "scale_linear" -> {
                    var scaleLinear = new ScaleLinear();
                    scaleLinear.axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
                    scaleLinear.gain = deserializeField(context, kwargs, "gain", double[].class, false);
                    scaleLinear.offset = deserializeField(context, kwargs, "offset", double[].class, false);
                    yield scaleLinear;
                }
                case "scale_mean_variance" -> {
                    var scaleMeanVariance = new ScaleMeanVariance();
                    scaleMeanVariance.mode = deserializeField(context, kwargs, "mode", ProcessingMode.class, false);
                    scaleMeanVariance.axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
                    scaleMeanVariance.eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
                    scaleMeanVariance.referenceTensor = deserializeField(context, kwargs, "reference_tensor", String.class, null);
                    yield scaleMeanVariance;
                }
                case "scale_range" -> {
                    var scaleRange = new ScaleRange();
                    scaleRange.mode = deserializeField(context, kwargs, "mode", ProcessingMode.class, false);
                    scaleRange.axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
                    scaleRange.eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
                    scaleRange.referenceTensor = deserializeField(context, obj, "reference_tensor", String.class, null);
                    scaleRange.maxPercentile = deserializeField(context, kwargs, "max_percentile", Double.class, 0.0);
                    scaleRange.minPercentile = deserializeField(context, kwargs, "min_percentile", Double.class, 100.0);
                    yield scaleRange;
                }
                case "sigmoid" -> {
                    yield new Sigmoid();
                }
                case "zero_mean_unit_variance" -> {
                    var zeroMeanUnitVariance = new ZeroMeanUnitVariance();
                    zeroMeanUnitVariance.mode = deserializeField(context, kwargs, "mode", ProcessingMode.class, false);
                    zeroMeanUnitVariance.axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
                    zeroMeanUnitVariance.eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
                    zeroMeanUnitVariance.mean = deserializeField(context, kwargs, "mean", double[].class, false);
                    zeroMeanUnitVariance.std = deserializeField(context, kwargs, "std", double[].class, false);
                    yield zeroMeanUnitVariance;
                }
                default -> {
                    var processing = new Processing(name);
                    processing.kwargs = kwargs == null ? Collections.emptyMap() : context.deserialize(kwargs, Map.class);
                    yield processing;
                }
            };
        }

    }

}
