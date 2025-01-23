package qupath.bioimageio.spec.tensor;

import com.google.gson.annotations.SerializedName;
import qupath.bioimageio.spec.FileDescr;
import qupath.bioimageio.spec.tensor.axes.Axis;

import java.util.Arrays;
import java.util.List;
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
    private String sampleTensor;
    private String name;
    private String id;
    private Shape shape;

    public TensorDataDescription getDataDescription() {
        return data;
    }

    private TensorDataDescription data;

    @SerializedName("data_range")
    private double[] dataRange;

    public Axis[] getAxes() {
        return axes;
    }

    public String getDataType() {
        return dataType;
    }

    public String getName() {
        return name != null ? name : id;
    }

    public String getId() {
        return id;
    }

    public Shape getShape() {
        return shape != null ? shape : new Shape.SizesShape(Arrays.stream(axes).map(Axis::getSize).collect(Collectors.toList()));
    }

    public double[] getDataRange() {
        // todo: this will be wrong if the axes hold the ranges...
        return dataRange == null ? null : dataRange.clone();
    }

    public void validate(List<? extends BaseTensor> otherTensors) {
        if (shape != null) {
            shape.validate(otherTensors);
        }
        for (var axis: getAxes()) {
            axis.validate(otherTensors);
        }
    }

    public FileDescr getTestTensor() {
        return testTensor;
    }

    public String getSampleTensor() {
        return sampleTensor;
    }
}
