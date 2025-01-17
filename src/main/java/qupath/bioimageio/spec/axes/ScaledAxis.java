package qupath.bioimageio.spec.axes;

import qupath.bioimageio.spec.sizes.Size;

/**
 * Axes that have both size and scale.
 */
public interface ScaledAxis {
    /**
     * Get the size of this axis.
     * @return The size.
     */
    Size getSize();

    /**
     * Get the scale of this axis.
     * @return The scale (might be constant).
     */
    double getScale();
}
