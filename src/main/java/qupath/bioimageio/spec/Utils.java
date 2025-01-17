package qupath.bioimageio.spec;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.bioimageio.spec.axes.Axis;

/**
 * Parse the model spec at <a href="https://github.com/bioimage-io/spec-bioimage-io">https://github.com/bioimage-io/spec-bioimage-io</a> in Java.
 * <p>
 * Currently, this has three dependencies:
 * <ul>
 * <li>snakeyaml</li>
 * <li>Gson</li>
 * <li>slf4j-api</li>
 * </ul>
 * <p>
 * The requirement for Gson might be removed in the future to further simplify reuse.
 * 
 * @author Pete Bankhead
 * 
 * @implNote This was written using v0.4.0 of the bioimage.io model spec, primarily for use in QuPath (but without any 
 *           QuPath-specific dependencies). It might be better generalized in the future.
 */
public class Utils {
	
	private final static Logger logger = LoggerFactory.getLogger(Utils.class);

	private Utils() {
		throw new UnsupportedOperationException("Don't instantiate this class");
	}

    /**
	 * Ensure the input is an unmodifiable list, or empty list if null.
	 * Note that OpenJDK implementation is expected to return its input if already unmodifiable.
	 * @param <T> The type of list objects.
	 * @param list The input list.
	 * @return An unmodifiable list.
	 */
	public static <T> List<T> toUnmodifiableList(List<T> list) {
		return list == null || list.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(list);
	}

	/**
	 * Deserialize a field from a JSON object.
	 * @param <T> The type of the field.
	 * @param context The context used for deserialization.
	 * @param obj The JSON object that contains the field.
	 * @param name The name of the field.
	 * @param typeOfT The type of the field.
	 * @param doStrict if true, fail if the field is missing; otherwise, return null
	 * @return A parsed T object.
	 * @throws IllegalArgumentException if doStrict is true and the field is not found
	 */
	public static <T> T deserializeField(JsonDeserializationContext context, JsonObject obj, String name, Type typeOfT, boolean doStrict) throws IllegalArgumentException {
		if (doStrict && !obj.has(name))
			throw new IllegalArgumentException("Required field " + name + " not found");
		return deserializeField(context, obj, name, typeOfT, null);
	}

	public static <T> T deserializeField(JsonDeserializationContext context, JsonObject obj, String name, Type typeOfT, T defaultValue) {
		if (obj.has(name)) {
			return ensureUnmodifiable(context.deserialize(obj.get(name), typeOfT));
		}
		return ensureUnmodifiable(defaultValue);
	}
	/**
	 * Minor optimization - ensure any lists, maps or sets are unmodifiable at this point,
	 * to avoid generating new unmodifiable wrappers later.
	 * @param <T> The type of the object.
	 * @param input A collection that should be made unmodifiable (copied if not already unmodifiable).
	 * @return An unmodifiable object of class T.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T ensureUnmodifiable(T input) {
		if (input instanceof List)
			return (T)Collections.unmodifiableList((List<?>)input);
		if (input instanceof Map)
			return (T)Collections.unmodifiableMap((Map<?, ?>)input);
		if (input instanceof Set)
			return (T)Collections.unmodifiableSet((Set<?>)input);
		return input;
	}


	/**
	 * Create a shape array for a given axes.
	 * The axes are expected to a string containing only the characters
	 * {@code bitczyx} as defined in the spec.
	 * <p>
	 * The purpose of this is to build shape arrays easily without needing to
	 * explicitly handle different axes and dimension ordering.
	 * <p>
	 * An example:
	 * <pre>
	 * <code>
	 * int[] shape = createShapeArray("byxc", Map.of('x', 256, 'y', 512), 1);
	 * </code>
	 * </pre>
	 * <p>
	 * This should result in an int array with values {@code [1, 512, 256, 1]}.
	 *
	 * @param axes the axes string
	 * @param target map defining the intended length for specified dimensions
	 * @param defaultLength the default length to use for any dimension that are not included in the target map
	 * @return an int array with the same length as the axes string, containing the requested dimensions or default values
	 */
	public static int[] createShapeArray(String axes, Map<Character, Integer> target, int defaultLength) {
		int[] array = new int[axes.length()];
		int i = 0;
		for (var c : axes.toLowerCase().toCharArray()) {
			array[i] = target.getOrDefault(c, defaultLength);
			i++;
		}
		return array;
	}

	/**
	 * Get the old "bcyx" style axis representation of an Axis array.
	 * @param axes The Axis array.
	 * @return A string representing the axis types.
	 */
	public static String getAxesString(Axis[] axes) {
		return Arrays.stream(axes).map(a -> a.getType().toString()).collect(Collectors.joining());
	}


}
