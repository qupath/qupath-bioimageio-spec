package qupath.bioimageio.spec.axes;

/**
 * An axis with a halo.
 * The halo should be cropped from the output tensor to avoid boundary effects.
 * It is to be cropped from both sides, i.e. `size_after_crop = size - 2 * halo`.
 * To document a halo that is already cropped by the model use `size.offset` instead.
 */
public interface WithHalo {
    /**
     * Get the size of the halo for this axis.
     * @return The size of the halo in pixels.
     */
    int getHalo();
}
