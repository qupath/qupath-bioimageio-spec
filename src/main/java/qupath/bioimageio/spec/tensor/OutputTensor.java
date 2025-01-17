package qupath.bioimageio.spec.tensor;

import qupath.bioimageio.spec.Processing;

import java.util.List;

import static qupath.bioimageio.spec.Utils.toUnmodifiableList;

/**
 * Model output, including shape, axes, halo, datatype and postprocessing.
 */
public class OutputTensor extends BaseTensor {

    private List<Processing> postprocessing;

    private int[] halo;

    public List<Processing> getPostprocessing() {
        return toUnmodifiableList(postprocessing);
    }

    // todo: needs to handle halo in axis rather than at tensor level
    public int[] getHalo() {
        return halo == null ? new int[0] : halo.clone();
    }

    @Override
    public String toString() {
        return "Output tensor [" + getShape() + ", postprocessing steps=" + getPostprocessing().size() + "]";
    }


}
