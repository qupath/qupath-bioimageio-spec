package qupath.bioimageio.spec.tmp.tensor;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import qupath.bioimageio.spec.Utils;
import qupath.bioimageio.spec.tmp.Shape;
import qupath.bioimageio.spec.tmp.axes.Axis;

import java.lang.reflect.Type;
import java.util.ArrayList;
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

    void validate(List<? extends BaseTensor> otherTensors) {
        if (shape != null) {
            shape.validate(otherTensors);
        }
        for (var axis: getAxes()) {
            axis.validate(otherTensors);
        }
    }


}
