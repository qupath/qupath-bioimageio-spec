package qupath.bioimageio.spec;

import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

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
public class BioimageIoSpec {
	
	private final static Logger logger = LoggerFactory.getLogger(BioimageIoSpec.class);
	
	/**
	 * General resource, based upon the RDF.
	 * <p>
	 * For machine learning models, you probably want {@link BioimageIoModel}.
	 */
	public static class BioimageIoResource {
				
		String formatVersion;
		List<Author> authors;
		String description;
		String documentation;

		String name;
		
		List<String> tags;
		
		@SerializedName("training_data")
        BioimageIoDataset trainingData;

		String version;
		
		Map<String,?> attachments;
		List<Badge> badges;
		List<CiteEntry> cite;
		
		List<String> covers;

		@SerializedName("download_url")
        String downloadURL;
		String gitRepo;
		String icon;
		String id;
		String license;

		List<String> links;
		List<Author> maintainers;
		
		private String source;
		
		@SerializedName("rdf_source")
        String rdfSource;

		public String getVersion() {
			return version;
		}

		public List<Badge> getBadges() {
			return toUnmodifiableList(badges);
		}

		public List<CiteEntry> getCite() {
			return toUnmodifiableList(cite);
		}

		public String getDocumentation() {
			return documentation;
		}
		
		public String getName() {
			return name;
		}
		
		public String getIcon() {
			return icon;
		}
		
		public String getGitRepo() {
			return gitRepo;
		}
		
		public String getDescription() {
			return description;
		}
		
		public String getFormatVersion() {
			return formatVersion;
		}

		public String getLicense() {
			return license;
		}
		
		public String getSource() {
			return source;
		}
		
		public String getID() {
			return id;
		}
		
		public List<Author> getAuthors() {
			return toUnmodifiableList(authors);
		}
		
		public List<Author> getMaintainers() {
			return toUnmodifiableList(maintainers);
		}
		
		public List<String> getLinks() {
			return toUnmodifiableList(links);
		}
		
		public List<String> getTags() {
			return toUnmodifiableList(tags);
		}
		
		public List<String> getCovers() {
			return toUnmodifiableList(covers);
		}
		
		public Map<String, ?> getAttachments() {
			return attachments == null ? Collections.emptyMap() : Collections.unmodifiableMap(attachments);
		}

		public boolean isNewerThan(ModuleDescriptor.Version version) {
			return ModuleDescriptor.Version.parse(this.getFormatVersion()).compareTo(version) > 0;
		}

		public boolean isNewerThan(String version) {
			return isNewerThan(ModuleDescriptor.Version.parse(version));
		}
	}
	
	/**
	 * Dataset spec. Currently, this provides equivalent information to {@link BioimageIoResource}.
	 */
	public static class BioimageIoDataset extends BioimageIoResource {}
	
	/**
	 * Enum representing supported model weights.
	 */
	public enum WeightsEntry {
		
		KERAS_HDF5("keras_hdf5"),
		PYTORCH_STATE_DICT("pytorch_state_dict"),
		TENSORFLOW_JS("tensorflow_js"),
		TENSORFLOW_SAVED_MODEL_BUNDLE("tensorflow_saved_model_bundle"),
		ONNX("onnx"),
		TORCHSCRIPT("torchscript", Set.of("pytorch_script"));
		
		private final String key;
		// Support older key names as alternatives
		private final Set<String> alternatives;

		WeightsEntry(String key) {
			this(key, Collections.emptySet());
		}
		
		WeightsEntry(String key, Set<String> alternatives) {
			this.key = key;
			this.alternatives = Collections.unmodifiableSet(alternatives);
		}
		
		@Override
		public String toString() {
			return key;
		}
		
		static WeightsEntry fromKey(String name) {
			for (var v : values()) {
				if (v.key.equals(name) || v.alternatives.contains(name))
					return v;
			}
			return null;
		}
	}
	
	static class WeightsMap {
		
		Map<WeightsEntry, ModelWeights> map;
		
		public Map<String, ModelWeights> withStringKeys() {
			return map == null ? Collections.emptyMap() : map.entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue()));
		}

		Map<WeightsEntry, ModelWeights> getMap() {
			return map;
		}
	}
	
	
	/**
	 * Resource based on the main model spec.
	 * This extends {@link BioimageIoResource} to provide more information 
	 * relevant for to machine learning models.
	 */
	public static class BioimageIoModel extends BioimageIoResource {
		
		URI baseURI;
		URI uri;

		List<InputTensor> inputs;
				
		@SerializedName("test_inputs")
        List<String> testInputs;
		@SerializedName("test_outputs")
        List<String> testOutputs;
		
		// Should be in ISO 8601 format - but preserve string as it is found
        String timestamp;
		
		WeightsMap weights;

		Map<?, ?> config;
		
		List<OutputTensor> outputs;
		
		@SerializedName("packaged_by")
        List<Author> packagedBy;
		
		ModelParent parent;
				
		// TODO: Handle run_mode properly
		@SerializedName("run_mode")
        Map<?, ?> runMode;
		
		@SerializedName("sample_inputs")
        List<String> sampleInputs;
		@SerializedName("sample_outputs")
        List<String> sampleOutputs;

		
		/**
		 * Get the base URI, providing the location of the model.
		 * This is typically a URI representing the downloaded model directory 
		 * (the parent of {@link #getURI()}).
		 * This may also represent a zipped file, if the model is read without unzipping 
		 * the contents.
		 * @return The URI to the model file.
		 */
		public URI getBaseURI() {
			if (baseURI != null)
				return baseURI;
			if (uri != null)
				return uri.resolve("..");
			return null;
		}
		
		/**
		 * Get the URI providing the location of the model.
		 * This is typically a URI representing the yaml file for the model.
		 * @return The URI to the model file.
		 */
		public URI getURI() {
			return uri;
		}
		
		/**
		 * Get a map view of the weights. It's generally better to use {@link #getWeights(WeightsEntry key)}.
		 * @return a map view of the weights.
		 */
		public Map<String, ModelWeights> getWeights() {
			return weights == null ? Collections.emptyMap() : weights.withStringKeys();
		}
		
		/**
		 * Alternative to {@link #getWeights()} that corrects for keys that have been renamed.
		 * @param key The query key.
		 * @return The weights value, or null if not found.
		 */
		public ModelWeights getWeights(WeightsEntry key) {
			if (weights == null || key == null)
				return null;
			return weights.map.getOrDefault(key, null);
		}
		
		/**
		 * Alternative to {@link #getWeights(WeightsEntry)} using a string key.
		 * @param key The query key string.
		 * @return The weights value, or null if not found.
		 */
		public ModelWeights getWeights(String key) {
			return getWeights(WeightsEntry.fromKey(key));
		}
		
		public ModelParent getParent() {
			return parent;
		}
		
		public String getTimestamp() {
			return timestamp;
		}
		
		public Map<?, ?> getConfig() {
			return config;
		}
		
		public List<InputTensor> getInputs() {
			return toUnmodifiableList(inputs);
		}

		public List<OutputTensor> getOutputs() {
			return toUnmodifiableList(outputs);
		}
		
		public List<String> getTestInputs() {
			return toUnmodifiableList(testInputs);
		}

		public List<String> getTestOutputs() {
			return toUnmodifiableList(testOutputs);
		}
		
		public List<String> getSampleInputs() {
			return toUnmodifiableList(sampleInputs);
		}

		public List<String> getSampleOutputs() {
			return toUnmodifiableList(sampleOutputs);
		}
		
	}
	
    /**
	 * Ensure the input is an unmodifiable list, or empty list if null.
	 * Note that OpenJDK implementation is expected to return its input if already unmodifiable.
	 * @param <T> The type of list objects.
	 * @param list The input list.
	 * @return An unmodifiable list.
	 */
	private static <T> List<T> toUnmodifiableList(List<T> list) {
		return list == null || list.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(list);
	}
	
	/**
	 * Author or maintainer.
	 */
	public static class Author {
		
		String affiliation;
		@SerializedName("github_user")
        String githubUser;
		String name;
		String orcid;
		
		public String getAffiliation() {
			return affiliation;
		}
		
		public String getGitHubUser() {
			return githubUser;
		}
		
		public String getName() {
			return name;
		}
		
		public String getOrcid() {
			return orcid;
		}
		
		@Override
		public String toString() {
			String base = "Author: " + name;
			if (affiliation != null)
				return base + " (" + affiliation + ")";
			else if (githubUser != null)
				return base + " (" + githubUser + ")";
			return base;
		}
		

	}
	
	/**
	 * Model weights information for a specific format.
	 */
	public static class ModelWeights {
		
		private String source;
		private Map<String, ?> attachments;
		private List<Author> authors;
		private String parent;
		private String sha256;
		
		public String getSource() {
			return source;
		}
		
		public Map<String, ?> getAttachments() {
			return attachments == null ? Collections.emptyMap() : Collections.unmodifiableMap(attachments);
		}
		
		public String getParent() {
			return parent;
		}
		
		public String getSha256() {
			return sha256;
		}
		
		public List<Author> getAuthors() {
			return toUnmodifiableList(authors);
		}
		
		@Override
		public String toString() {
			return "Weights: " + source;
		}
				
	}
	
	/**
	 * Model parent. Currently, this provides only an ID and URI.
	 * @author petebankhead
	 *
	 */
	public static class ModelParent {
		
		private String id;
		private String sha256;
		private String uri;
		
		public String getID() {
			return id;
		}
		
		public String getSha256() {
			return sha256;
		}
		
		public String getURI() {
			return uri;
		}
		
		@Override
		public String toString() {
			return "Parent: " + id;
		}
		
	}

	/**
	 * The basic tensor representation for inputs and outputs.
	 */
	public abstract static class BaseTensor {

		protected Axis[] axes;
		@SerializedName("data_type")
		private String dataType;
		private String name;
		private String id;
		private Shape shape;
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


	interface TensorDataDescription {

	}

	/**
	 * A description of the possible discrete data values in a tensor.
	 */
	public static class NominalOrOrdinalDataDescription implements TensorDataDescription {
		private final NominalOrOrdinalDType type;

		public List<? extends JsonElement> getValues() {
			return values;
		}

		public NominalOrOrdinalDType getType() {
			return type;
		}

		private final List<? extends JsonElement> values; // todo: can be int, float, bool, str

        NominalOrOrdinalDataDescription(NominalOrOrdinalDType type, List<? extends JsonElement> values) {
            this.type = type;
            this.values = values;
        }

    }
	

	/**
	 * A description of the possible ratio data values in a tensor.
	 */
	public static class IntervalOrRatioDataDescription implements TensorDataDescription {
		private final IntervalOrRatioDType type;
		private final List<Optional<Float>> range;
		private final String unit; // todo: SI unit or "abitrary unit": validate somehow
		private final float scale;
		private final float offset;

		IntervalOrRatioDataDescription(IntervalOrRatioDType type, List<Optional<Float>> range, String unit, float scale, float offset) {
            this.type = type;
            this.range = range;
            this.unit = unit;
            this.scale = scale;
            this.offset = offset;
        }

		IntervalOrRatioDataDescription(IntervalOrRatioDType type, List<Optional<Float>> range, String unit, float scale) {
			this(type, range, unit, scale, 0);
		}

		IntervalOrRatioDataDescription(IntervalOrRatioDType type, List<Optional<Float>> range, String unit) {
			this(type, range, unit, 1, 0);
		}

		IntervalOrRatioDataDescription(IntervalOrRatioDType type, List<Optional<Float>> range) {
			this(type, range, "aritrary unit", 1, 0);
		}

		IntervalOrRatioDataDescription(IntervalOrRatioDType type) {
			this(type, List.of(Optional.empty(), Optional.empty()), "aritrary unit", 1, 0);
		}

		public IntervalOrRatioDType getType() {
			return type;
		}

		public List<Optional<Float>> getRange() {
			return range;
		}

		public String getUnit() {
			return unit;
		}

		public float getScale() {
			return scale;
		}

		public float getOffset() {
			return offset;
		}

	}
	
	public enum IntervalOrRatioDType {
		FLOAT32,
		FLOAT64,
		UINT8,
		INT8,
		UINT16,
		INT16,
		UINT32,
		INT32,
		UINT64,
		INT64
	}

	public enum NominalOrOrdinalDType {
		FLOAT32,
		FLOAT64,
		UINT8,
		INT8,
		UINT16,
		INT16,
		UINT32,
		INT32,
		UINT64,
		INT64,
		BOOL
	}



	/**
	 * Model input, including shape, axes, datatype and preprocessing.
	 */
	public static class InputTensor extends BaseTensor {
		private List<Processing> preprocessing;

		public List<Processing> getPreprocessing() {
			return toUnmodifiableList(preprocessing);
		}

		@Override
		public String toString() {
			return "Input tensor [" + getShape() + ", processing steps=" + getPreprocessing().size() + "]";
		}
	}

	/**
	 * Model output, including shape, axes, halo, datatype and postprocessing.
	 */
	public static class OutputTensor extends BaseTensor {
				
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
	
	/**
	 * Shape of input or output tensor.
	 */
	public static class Shape {
		
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

		private void validate(List<? extends BaseTensor> otherTensors) {}

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
		
		static class SizesShape extends Shape {
			private final List<Size> sizes;

			SizesShape(List<Size> sizes) {
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
	}

	
	/**
	 * Model or repository badge.
	 */
	public static class Badge {
		
		private String label;
		private String icon;
		private String url;
		
		public String getLabel() {
			return label;
		}

		public String getIcon() {
			return icon;
		}

		public String getURL() {
			return url;
		}

	}
	
	/**
	 * Citation entry.
	 */
	public static class CiteEntry {
		
		private String text;
		private String doi;
		private String url;
		
		public String getText() {
			return text;
		}
		
		public String getDOI() {
			return doi;
		}
		
		public String getURL() {
			return url;
		}
		
	}


	
	/**
	 * Base class for pre- and post-processing operations.
	 * See the model spec for details.
	 */
	public static class Processing {

		/**
		 * Requested mode for pre- or post-processing.
		 */
		public enum ProcessingMode {FIXED, PER_DATASET, PER_SAMPLE}

		private final String name;
		Map<String, Object> kwargs;

		Processing(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Map<String, ?> getKwargs() {
			return kwargs == null ? Collections.emptyMap() : Collections.unmodifiableMap(kwargs);
		}

		/**
		 * A binarize operation.
		 */
		public static class Binarize extends Processing {

			Binarize() {
				super("binarize");
			}

			double threshold = Double.NaN;

			public double getThreshold() {
				return threshold;
			}

		}

		/**
		 * A clipping operation.
		 */
		public static class Clip extends Processing {

			double min;
			double max;

			Clip() {
				super("clip");
			}

			public double getMin() {
				return min;
			}

			public double getMax() {
				return max;
			}

		}

		/**
		 * A linear scaling operation.
		 */
		public static class ScaleLinear extends Processing {

			double[] gain;
			double[] offset;
			Axis[] axes;

			ScaleLinear() {
				super("scale_linear");
			}

			public double[] getGain() {
				return gain == null ? null : gain.clone();
			}

			public double[] getOffset() {
				return offset == null ? null : offset.clone();
			}

			public Axis[] getAxes() {
				return axes;
			}

		}

		/**
		 * A sigmoid transofmration.
		 */
		public static class Sigmoid extends Processing {

			Sigmoid() {
				super("sigmoid");
			}

		}

		protected abstract static class ProcessingWithMode extends Processing {

			ProcessingMode mode = ProcessingMode.PER_SAMPLE;
			Axis[] axes;
			double eps = 1e-6;

			ProcessingWithMode(String name) {
				super(name);
			}

			public double getEps() {
				return eps;
			}

			public ProcessingMode getMode() {
				return mode;
			}

			public Axis[] getAxes() {
				return axes;
			}

		}

		/**
		 * A scaling operation to the mean and variance of a reference tensor.
		 */
		public static class ScaleMeanVariance extends ProcessingWithMode {

			String referenceTensor;

			ScaleMeanVariance() {
				super("scale_mean_variance");
			}

			public String getReferenceTensor() {
				return referenceTensor;
			}

		}

		/**
		 * A scaling operation to a reference tensor.
		 */
		public static class ScaleRange extends ProcessingWithMode {

			String referenceTensor; // TODO: Figure out whether to use this somehow

			double minPercentile = 0.0;
			double maxPercentile = 100.0;

			ScaleRange() {
				super("scale_range");
			}

			public double getMinPercentile() {
				return minPercentile;
			}

			public double getMaxPercentile() {
				return maxPercentile;
			}

			public String getReferenceTensor() {
				return referenceTensor;
			}


		}

		/**
		 * A scaling operation to zero mean and unit variqnce.
		 */
		public static class ZeroMeanUnitVariance extends ProcessingWithMode {

			double[] mean;
			double[] std;

			ZeroMeanUnitVariance() {
				super("zero_mean_unit_variance");
			}

			public double[] getMean() {
				return mean == null ? null : mean.clone();
			}

			public double[] getStd() {
				return std == null ? null : std.clone();
			}

		}

	}

	/**
	 * Get the old "bcyx" style axis representation of an Axis array.
	 * @param axes The Axis array.
	 * @return A string representing the axis types.
	 */
	public static String getAxesString(Axis[] axes) {
		return Arrays.stream(axes).map(a -> a.getType().toString()).collect(Collectors.joining());
	}

	/**
	 * Base axis class for 0.4 and 0.5 axes.
	 */
	public interface Axis {
		/**
		 * Get the type of this axis, see {@link AxisType}.
		 * @return The axis type.
		 */
		AxisType getType();

		/**
		 * Get the size of this axis.
		 * @return The size, unless it's a 0.4 axis (these have no size).
		 */
		Size getSize();

		/**
		 * Get the axis ID.
		 * @return The axis ID.
		 */
		String getID();

		/**
		 * Ensure the parameters of the axis are valid.
		 * @param tensors Other tensors in the model, in case they are referenced in this axis.
		 */
		void validate(List<? extends BioimageIoSpec.BaseTensor> tensors);
	}

	/**
	 * The type of axis. Batch (b), index (i), channel (c), x, y, z, time (t).
	 */
	public enum AxisType {
		B("b"),
		I("i"),
		C("c"),
		X("x"),
		Y("y"),
		Z("z"),
		T("t");
		private final String type;

		AxisType(String type) {
			this.type = type;
		}

		static AxisType fromString(String s) {
			return AxisType.valueOf(s.toUpperCase());
		}

		@Override
		public String toString() {
			return type;
		}
	}


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

	/**
	 * An axis size. Can be fixed, data-dependent (unknown), parameterized, or based on another axis.
	 */
	public interface Size {
		int NO_SIZE = -1;

		/**
		 * Get the default size of this axis. {@link #getTargetSize(int)} may be more useful.
		 * @return The size of this axis.
		 */
		int getSize();

		/**
		 * Get a size as close as possible to a target.
		 * @param target The target size.
		 * @return The fixed output size, {@link #NO_SIZE} or as close as we can get to the target size.
		 */
		int getTargetSize(int target);

		/**
		 * Get the step size of this axis size.
		 * @return {@link #NO_SIZE} for any axis without a step size, otherwise the size.
		 */
		int getStep();

		void validate(List<? extends BioimageIoSpec.BaseTensor> tensors);
	}

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

	/**
	 * Simple class to hold the old "bcyx" format from 0.4
	 */
	static class CharAxis implements Axis {
		private final char axis;

		CharAxis(char c) {
			this.axis = c;
		}

		@Override
		public AxisType getType() {
			return AxisType.valueOf(String.valueOf(axis).toUpperCase());
		}

		@Override
		public Size getSize() {
			throw new UnsupportedOperationException("Cannot get Size of legacy/char axes");
		}

		@Override
		public String getID() {
			return "";
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			// can't validate char axes, these are validated at tensor level
		}
	}

	abstract static class AxisBase implements Axis {
		private final String id;
		private final String description;
		AxisBase(String id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String getID() {
			return this.id;
		}
	}

	static class BatchAxis extends AxisBase {
		private final int size;
		private final boolean concatenable = true;

		BatchAxis(String id, String description, int size) {
			super(id, description);
			this.size = size;
		}

		@Override
		public AxisType getType() {
			return AxisType.B;
		}

		@Override
		public Size getSize() {
			return new FixedSize(size);
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			// fixed size doesn't need validation
		}
	}

	static class ChannelAxis extends AxisBase implements ScaledAxis {
		private final List<String> channel_names;

		ChannelAxis(String id, String description, List<String> channel_names) {
			super(id, description);
			this.channel_names = List.copyOf(channel_names);
		}

		@Override
		public Size getSize() {
			return new FixedSize(channel_names.size());
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			// fixed size based on list of channels
		}

		@Override
		public AxisType getType() {
			return AxisType.C;
		}

		@Override
		public double getScale() {
			return 1;
		}
	}

	abstract static class IndexAxisBase extends AxisBase implements ScaledAxis {
		private final double scale = 1.0;
		private final String unit = null;

		IndexAxisBase(String id, String description) {
			super(id, description);
		}

		@Override
		public AxisType getType() {
			return AxisType.I;
		}

		@Override
		public Size getSize() {
			return null;
		}

		@Override
		public double getScale() {
			return 1;
		}
	}

	static class IndexInputAxis extends IndexAxisBase {
		private final Size size;
		private final boolean concatenable = false;

		IndexInputAxis(String id, String description, Size size) {
			super(id, description);
			this.size = size;
		}
		@Override
		public Size getSize() {
			return this.size;
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			size.validate(tensors);
		}
	}

	static class IndexOutputAxis extends IndexAxisBase {
		private Size size;

		IndexOutputAxis(String id, String description) {
			super(id, description);
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			size.validate(tensors);
		}
	}


	/**
	 * Describes a range of valid tensor axis sizes as `size = min + n*step`.
	 */
	static class ParameterizedSize implements Size {
		private final int min;
		private final int step;

		ParameterizedSize(int min, int step) {
			this.min = min;
			this.step = step;
		}

		@Override
		public int getSize() {
			return getTargetSize(1);
		}

		@Override
		public int getTargetSize(int target) {
			if (target < 0 || step <= 0)
				return min;
			else
				return min + (int)Math.round((target - min)/(double)step) * step;
		}
		@Override
		public int getStep() {
			return step;
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			assert min >= 0;
			assert step >= 0;
		}
	}

	/**
	 * A size that is only known after model inference (eg, detecting an unknown number of instances).
	 */
	static class DataDependentSize implements Size {
		private int min = 0;
		private int max = Integer.MAX_VALUE;

		DataDependentSize(int min, int max) {
			this.min = min;
			this.max = max;
		}

		@Override
		public int getSize() {
			return NO_SIZE;
		}

		@Override
		public int getTargetSize(int target) {
			return getSize();
		}

		@Override
		public int getStep() {
			return NO_SIZE;
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			assert min > 0;
			assert max >= min;
		}
	}

	/**
	 * A tensor axis size (extent in pixels/frames) defined in relation to a reference axis.
	 * <br>
	 * <code>size = reference.size * reference.scale / axis.scale + offset</code>
	 */
	static class ReferencedSize implements Size {
		private volatile String thisID;
		@SerializedName("axis_id")
		private final String refID;
		@SerializedName("tensor_id")
		private final String tensorID;
		private final int offset;
		private final double scale;

		private volatile ScaledAxis referenceAxis;

		ReferencedSize(String refID, String tensorID) {
			this(refID, tensorID, 1, 0);
		}

		ReferencedSize(String refID, String tensorID, double scale, int offset) {
			this.refID = refID;
			this.tensorID = tensorID;
			this.scale = scale;
			this.offset = offset;
		}

		@Override
		public int getSize() {
			return (int) (referenceAxis.getSize().getSize() * referenceAxis.getScale() / scale + offset);
		}

		@Override
		public int getTargetSize(int target) {
			return getSize();
		}

		@Override
		public int getStep() {
			return NO_SIZE;
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			var tensor = tensors.stream().filter(t -> t.getId().equals(tensorID)).findFirst().orElse(null);
			if (tensor == null) {
				throw new JsonParseException("Cannot find reference tensor " + tensorID);
			}
			ScaledAxis axis = (ScaledAxis) Arrays.stream(tensor.axes).filter(ax -> ax.getID().equalsIgnoreCase(refID)).findFirst().orElse(null);
			if (axis == null) {
				throw new JsonParseException("Cannot find reference axis " + refID);
			}
			this.referenceAxis = axis;
		}
	}

	static class FixedSize implements Size {
		private final int size;
		FixedSize(int size) {
			this.size = size;
		}

		@Override
		public int getSize() {
			return size;
		}

		@Override
		public int getTargetSize(int target) {
			return getSize();
		}

		@Override
		public int getStep() {
			return NO_SIZE;
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			// can't validate ints
		}
	}

	abstract static class TimeAxisBase extends AxisBase implements ScaledAxis {
		private final String type = "time";
		private final TimeUnit unit;
		private final double scale;

		TimeAxisBase(String id, String description, TimeUnit unit, double scale) {
			super(id, description);
			this.unit = unit;
			this.scale = scale;
		}
		@Override
		public AxisType getType() {
			return AxisType.T;
		}

		@Override
		public double getScale() {
			return scale;
		}

		public TimeUnit getUnit() {
			return unit;
		}
	}

	static class TimeInputAxis extends TimeAxisBase {
		private final Size size;

		TimeInputAxis(String id, String description, TimeUnit unit, double scale, Size size) {
			super(id, description, unit, scale);
			this.size = size;
		}

		@Override
		public Size getSize() {
			return size;
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			getSize().validate(tensors);
		}
	}

	static class TimeOutputAxis extends TimeAxisBase {
		private final Size size;

		TimeOutputAxis(String id, String description, TimeUnit unit, double scale, Size size) {
			super(id, description, unit, scale);
			this.size = size;
		}

		@Override
		public Size getSize() {
			return size;
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			getSize().validate(tensors);
		}
	}

	static class TimeOutputAxisWithHalo extends TimeOutputAxis implements WithHalo {
		private final int halo;

		TimeOutputAxisWithHalo(String id, String description, TimeUnit unit, double scale, Size size, int halo) {
			super(id, description, unit, scale, size);
			this.halo = halo;
		}

		@Override
		public int getHalo() {
			return this.halo;
		}
	}

	public enum TimeUnit {
		ATTOSECOND,
		CENTISECOND,
		DAY,
		DECISECOND,
		EXASECOND,
		FEMTOSECOND,
		GIGASECOND,
		HECTOSECOND,
		HOUR,
		KILOSECOND,
		MEGASECOND,
		MICROSECOND,
		MILLISECOND,
		MINUTE,
		NANOSECOND,
		PETASECOND,
		PICOSECOND,
		SECOND,
		TERASECOND,
		YOCTOSECOND,
		YOTTASECOND,
		ZEPTOSECOND,
		ZETTASECOND
	}

	public enum SpaceUnit {
		ATTOMETER("attometer"),
		ANGSTROM("angstrom"),
		CENTIMETER("centimeter"),
		DECIMETER("decimeter"),
		EXAMETER("exameter"),
		FEMTOMETER("femtometer"),
		FOOT("foot"),
		GIGAMETER("gigameter"),
		HECTOMETER("hectometer"),
		INCH("inch"),
		KILOMETER("kilometer"),
		MEGAMETER("megameter"),
		METER("meter"),
		MICROMETER("micrometer"),
		MILE("mile"),
		MILLIMETER("millimeter"),
		NANOMETER("nanometer"),
		PARSEC("parsec"),
		PETAMETER("petameter"),
		PICOMETER("picometer"),
		TERAMETER("terameter"),
		YARD("yard"),
		YOCTOMETER("yoctometer"),
		YOTTAMETER("yottameter"),
		ZEPTOMETER("zeptometer"),
		ZETTAMETER("zettameter"),
		NO_UNIT("");

		private final String unit;

		SpaceUnit(String s) {
			this.unit = s;
		}
	}

	abstract static class SpaceAxisBase extends AxisBase implements ScaledAxis {
		private final SpaceUnit unit;
		private double scale = 1.0;

		SpaceAxisBase(String id, String description, String unit, double scale) {
			this(id, description, SpaceUnit.valueOf(unit.toUpperCase()), scale);
		}

		SpaceAxisBase(String id, String description, SpaceUnit unit, double scale) {
			super(id, description);
			this.unit = unit;
			this.scale = scale;
		}

		@Override
		public AxisType getType() {
			return AxisType.valueOf(this.getID().toUpperCase());
		}

		@Override
		public double getScale() {
			return this.scale;
		}

		public SpaceUnit getUnit() {
			return unit;
		}
	}

	static class SpaceInputAxis extends SpaceAxisBase {
		private final Size size;
		private final boolean concatenable = false;

		SpaceInputAxis(String id, String description, String unit, double scale, Size size) {
			super(id, description, unit.isEmpty() ? "NO_UNIT" : unit, scale);
			this.size = size;
		}

		@Override
		public Size getSize() {
			return this.size;
		}
		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			getSize().validate(tensors);
		}
	}

	static class SpaceOutputAxis extends SpaceAxisBase {
		private final Size size;

		SpaceOutputAxis(String id, String description, String unit, double scale, Size size) {
			super(id, description, unit, scale);
			this.size = size;
		}

		@Override
		public Size getSize() {
			return this.size;
		}

		@Override
		public void validate(List<? extends BioimageIoSpec.BaseTensor> tensors) {
			getSize().validate(tensors);
		}
	}

	static class SpaceOutputAxisWithHalo extends SpaceOutputAxis implements WithHalo {
		private ReferencedSize size;
		private int halo = 0;

		SpaceOutputAxisWithHalo(String id, String description, String unit, double scale, Size size, int halo) {
			super(id, description, unit, scale, size);
			this.halo = halo;
		}

		@Override
		public int getHalo() {
			return this.halo;
		}
	}


	public enum BioImageIoVersion {
		VERSION_0_4("0.4"),
		VERSION_0_5("0.5");
		private final String version;

		BioImageIoVersion(String s) {
			this.version = s;
		}

		public String getVersion() {
			return version;
		}
	}
}
