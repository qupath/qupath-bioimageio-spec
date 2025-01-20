package qupath.bioimageio.spec.tensor;

import java.util.List;

import static qupath.bioimageio.spec.Model.toUnmodifiableList;


/**
 * Model input, including shape, axes, datatype and preprocessing.
 */
public class InputTensor extends BaseTensor {
    private List<Processing> preprocessing;

    public List<Processing> getPreprocessing() {
        return toUnmodifiableList(preprocessing);
    }

    @Override
    public String toString() {
        return "Input tensor [" + getShape() + ", processing steps=" + getPreprocessing().size() + "]";
    }
}
