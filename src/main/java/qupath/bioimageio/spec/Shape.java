package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import qupath.bioimageio.spec.sizes.Size;
import qupath.bioimageio.spec.tensor.BaseTensor;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Shape of input or output tensor.
 */
public class Shape {

    protected int[] shape;

    /**
     * Get the shape, if this is defined explicitly.
     * Usually {@link #getTargetShape(int...)} is more useful.
     * @return The shape in pixels.
     */
    public int[] getShape() {
        return shape == null ? new int[0] : shape.clone();
    }

    /**
     * Get the minimum shape; useful for parameterized shapes.
     * @return An int array of minimum shape sizes.
     */
    public int[] getShapeMin() {
        return getShape();
    }

    /**
     * Get a compatible shape given the specified target.
     * <p>
     * For an explicit shape (without scale/offset/step etc.) the target
     * does not influence the result.
     * @param target The shape (in pixel width/height/etc) in pixels that we are requesting.
     * @return As close to the shape as the Shape object allows if a parameterized shape, or the fixed shape if fixed.
     */
    public int[] getTargetShape(int... target) {
        return getShape();
    }

    /**
     * Get the number of elements in the shape array.
     * @return The number of elements in the shape array.
     */
    public int getLength() {
        return shape == null ? 0 : shape.length;
    }

    /**
     * Get the shape step (increment), useful for parameterized shapes.
     * @return An int array of shape steps.
     * */
    public int[] getShapeStep() {
        return shape == null ? new int[0] : new int[shape.length];
    }

    /**
     * Get the shape scale, useful for implicit shapes.
     * @return A double array of scales.
     */
    public double[] getScale() {
        if (shape == null)
            return new double[0];
        var scale = new double[shape.length];
        Arrays.fill(scale, 1.0);
        return scale;
    }

    /**
     * Get the shape scale, useful for implicit shapes.
     * @return A double array of offsets.
     */
    public double[] getOffset() {
        return shape == null ? new double[0] : new double[shape.length];
    }

    @Override
    public String toString() {
        if (shape == null)
            return "Shape (unknown)";
        return "Shape (" + Arrays.toString(shape) + ")";
    }

    public void validate(List<? extends BaseTensor> otherTensors) {}

    /**
     * A shape that is determined based on a minimum and a step size.
     */
    public static class ParameterizedInputShape extends Shape {

        private int[] min;
        private int[] step;

        @Override
        public int[] getShapeMin() {
            return min == null ? super.getShapeMin() : min.clone();
        }

        @Override
        public int[] getShapeStep() {
            return min == null ? super.getShapeStep() : step.clone();
        }

        @Override
        public int[] getTargetShape(int... target) {
            if (min == null || min.length == 0)
                return super.getTargetShape(target);
            if (min.length != target.length) {
                throw new IllegalArgumentException("Target shape does not match! Should be length " + min.length + " but found length " + target.length);
            }
            int n = target.length;
            int[] output = new int[n];
            for (int i = 0; i < n; i++) {
                if (target[i] < 0 || step == null || step[i] <= 0)
                    output[i] = min[i];
                else
                    output[i] = min[i] + (int)Math.round((target[i] - min[i])/(double)step[i]) * step[i];
            }
            return output;
        }

        @Override
        public String toString() {
            if (shape != null)
                return "Input Shape (" + Arrays.toString(shape) + ")";
            if (min != null) {
                if (step != null)
                    return "Input Shape (min=" + Arrays.toString(min) + ", step=" + Arrays.toString(step) + ")";
                return "Input Shape (min=" + Arrays.toString(min) + ")";
            }
            return "Input shape (unknown)";
        }

    }

    /**
     * A shape that is determined based on the shape of another tensor.
     */
    public static class ImplicitOutputShape extends Shape {

        private String referenceTensor;
        private double[] offset;
        private double[] scale;

        public String getReferenceTensor() {
            return referenceTensor;
        }

        @Override
        public double[] getScale() {
            if (scale == null)
                return super.getScale();
            return scale.clone();
        }

        @Override
        public double[] getOffset() {
            if (offset == null)
                return super.getOffset();
            return offset.clone();
        }

        /**
         * Get the output shape for the given input.
         * This uses either the explicit shape, or scale and offset.
         */
        @Override
        public int[] getTargetShape(int... target) {
            if (offset == null || offset.length == 0 || scale == null || scale.length == 0)
                return super.getTargetShape(target);
            int n = target.length;
            int[] output = new int[n];
            for (int i = 0; i < n; i++) {
                output[i] = (int)Math.round(target[i] * scale[i] + offset[i] * 2);
            }
            return output;
        }

        @Override
        public String toString() {
            if (shape != null)
                return "Output Shape (" + Arrays.toString(shape) + ")";
            if (scale != null) {
                if (offset != null)
                    return "Output Shape (scale=" + Arrays.toString(scale) + ", offset=" + Arrays.toString(offset) + ")";
                return "Output Shape (scale=" + Arrays.toString(scale) + ")";
            }
            return "Output shape (unknown)";
        }

    }

    public static class SizesShape extends Shape {
        private final List<Size> sizes;

        public SizesShape(List<Size> sizes) {
            this.sizes = sizes;
            this.shape = this.sizes.stream().mapToInt(Size::getSize).toArray();
        }

        @Override
        public int[] getShape() {
            return sizes.stream().mapToInt(Size::getSize).toArray();
        }

        @Override
        public int[] getTargetShape(int[] target) {
            assert target.length == sizes.size();
            return IntStream.range(0, target.length)
                    .map(i -> sizes.get(i).getTargetSize(target[i]))
                    .toArray();
        }

        @Override
        public int[] getShapeStep() {
            return sizes.stream().mapToInt(Size::getStep).toArray();
        }
    }

    public static class Deserializer implements JsonDeserializer<Shape> {

        @Override
        public Shape deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            if (json.isJsonArray()) {
                var shape = new Shape();
                shape.shape = context.deserialize(json, int[].class);
                return shape;
            }
            var obj = json.getAsJsonObject();
            if (obj.has("min") && obj.has("step")) {
                return context.deserialize(obj, Shape.ParameterizedInputShape.class);
            }
            if (obj.has("offset") && obj.has("scale")) {
                return context.deserialize(obj, Shape.ImplicitOutputShape.class);
            }
            throw new JsonParseException("Can't deserialize unknown shape: " + json);
        }

    }

}


