package qupath.bioimageio.spec.tensor;

import qupath.bioimageio.spec.tensor.axes.WithHalo;

import java.util.Arrays;
import java.util.List;

import static qupath.bioimageio.spec.Model.toUnmodifiableList;

/**
 * Model output, including shape, axes, halo, datatype and postprocessing.
 */
public class OutputTensor extends BaseTensor {

    private List<Processing> postprocessing;

    private int[] halo;

    /**
     * Get the post-processing steps for this output tensor.
     * @return The processing steps
     */
    public List<Processing> getPostprocessing() {
        return toUnmodifiableList(postprocessing);
    }

    /**
     * The "halo" that should be cropped from the output tensor to avoid boundary effects.
     * The "halo" is to be cropped from both sides, i.e. `shape_after_crop = shape - 2 * halo`.
     * To document a "halo" that is already cropped by the model `shape.offset` has to be used instead.
     * In 0.5 models, halo is specified on tensor axes, not the tensor itself, but this should hopefully fetch halo from axis objects.
     * @return the halo
     */
    public int[] getHalo() {
        int[] out;
        if (halo == null) {
             out = Arrays.stream(axes).mapToInt(a -> {
                if (a instanceof WithHalo) {
                    return ((WithHalo) a).getHalo();
                }
                return 0;
            }).toArray();
        } else {
            out = halo.clone();
        }
        return out;
    }

    @Override
    public String toString() {
        return "Output tensor [" + getShape() + ", postprocessing steps=" + getPostprocessing().size() + "]";
    }

}
