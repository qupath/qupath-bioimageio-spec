package qupath.bioimageio.spec.tensor;

import com.google.gson.annotations.SerializedName;
import qupath.bioimageio.spec.FileDescr;
import qupath.bioimageio.spec.tensor.axes.Axis;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The basic tensor representation for inputs and outputs.
 */
public abstract class BaseTensor {

    protected Axis[] axes;
    @SerializedName("data_type")
    private String dataType;
    @SerializedName("test_tensor")
    private FileDescr testTensor;
    @SerializedName("sample_tensor")
    private FileDescr sampleTensor;
    private String name;
    private String id;
    private Shape shape;

    public TensorDataDescription getDataDescription() {
        return data;
    }

    private TensorDataDescription data;

    @SerializedName("data_range")
    private double[] dataRange;

    /**
     * Get the axes of the tensor. In the 0.4 spec, this is something like "bcxy"
     * In the 0.5 spec, axes are defined more explicitly.
     * Therefore, this can be a simple or complex object...
     * @return a clone of the axes.
     */
    public Axis[] getAxes() {
        return axes.clone();
    }

    /**
     * Get the data type of the tensor.
     * Inputs can be one of "float32", "uint8", "uint16".
     * Outputs can be one of "float32", "float64", "uint8", "int8", "uint16", "int16", "uint32", "int32", "uint64", "int64", "bool".
     * @return the data type
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Get the tensor name
     * @return the name
     */
    public String getName() {
        return name != null ? name : id;
    }

    /**
     * Get the ID of the tensor. Should be unique, though we do not enforce this on the Java side.
     * @return The tensor.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the shape of the tensor. Implementation in 0.4 spec is for the tensor to hold the shape of all axes as a field.
     * In 0.5, each axis specifies its own size, possibly relative to another tensor.
     * @return The shape.
     */
    public Shape getShape() {
        return shape != null ? shape : new Shape.SizesShape(Arrays.stream(axes).map(Axis::getSize).collect(Collectors.toList()));
    }

    /**
     * Tuple `(minimum, maximum)` specifying the allowed range of the data in this tensor.
     *     If not specified, the full data range that can be expressed in `data_type` is allowed.
     * @return the data range.
     */
    public double[] getDataRange() {
        // todo: unclear where data range has moved to
        return dataRange == null ? null : dataRange.clone();
    }

    /**
     * Validate a tensor by resolving links between tensors and other parameter settings.
     * @param otherTensors other tensors that may be linked to by this tensor.
     */
    public void validate(List<? extends BaseTensor> otherTensors) {
        if (shape != null) {
            shape.validate(otherTensors);
        }
        for (var axis: getAxes()) {
            axis.validate(otherTensors);
        }
    }

    /**
     * An example tensor to use for testing.
     * Using the model with the test input tensors is expected to yield the test output tensors.
     * Each test tensor has to be a ndarray in the
     * <a href="https://numpy.org/doc/stable/reference/generated/numpy.lib.format.html#module-numpy.lib.format">numpy.lib file format</a>,
     * The file extension must be '.npy'.
     * This will always be null in 0.4.x models, but shouldn't be in 0.5
     * @return null
     */
    public Optional<FileDescr> getTestTensor() {
        return Optional.ofNullable(testTensor);
    }

    /**
     * A sample tensor to illustrate a possible input/output for the model,
     * The sample image primarily serves to inform a human user about an example use case
     * and is typically stored as .hdf5, .png or .tiff.
     * It has to be readable by the <a href="https://imageio.readthedocs.io/en/stable/formats/index.html#supported-formats">imageio library</a>
     * (numpy's `.npy` format is not supported).
     * The image dimensionality has to match the number of axes specified in this tensor description.
     * This will always be null in 0.4.x models, but may not be in 0.5
     * @return maybe a sample tensor, maybe nothing!
     */
    public Optional<FileDescr> getSampleTensor() {
        return Optional.ofNullable(sampleTensor);
    }

}
