package qupath.bioimageio.spec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

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
	 * Parse a model from a file or directory.
	 * This can either be a yaml file, or a directory that contains a yaml file representing the model.
	 * @param file The file containing the YAML, or its parent directory.
	 * @return The parsed model.
	 * @throws IOException if the model cannot be found or parsed
	 */
	public static BioimageIoModel parseModel(File file) throws IOException {
		return parseModel(file.toPath());
	}

	/**
	 * Parse a model from a path.
	 * This can either represent a yaml file, or a directory that contains a yaml file representing the model.
	 * @param path The path to the file containing the YAML, or its parent directory.
	 * @return The parsed model.
	 * @throws IOException if the model cannot be found or parsed
	 */
	public static BioimageIoModel parseModel(Path path) throws IOException {
		
		var pathYaml = findModelRdf(path);
		if (pathYaml == null) {
			throw new IOException("Can't find rdf.yaml from " + path);
		}
		
		try (var stream = Files.newInputStream(pathYaml)) {
			var model = parseModel(stream);
			if (model != null) {
				model.baseURI = pathYaml.getParent().toUri();
				model.uri = pathYaml.toUri();
			}
			return model;
		}
	}
	
	/**
	 * Parse a model from an input stream.
	 * Note that {@link BioimageIoModel#getBaseURI()} will return null in this case, because the base URI 
	 * is unknown.
	 * @param stream A stream of YAML.
	 * @return The parsed model.
	 * @throws IOException if the model cannot be found or parsed
	 */
	public static BioimageIoModel parseModel(InputStream stream) throws IOException {
		
		try {
			// Important to use SafeConstructor to restrict potential classes that might be initiated
			// Note: we can't support -inf or inf (but can support -.inf or .inf)
			Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
			Map<String, ?> map = yaml.load(stream);

			var builder = new GsonBuilder()
					.serializeSpecialFloatingPointValues()
					.setPrettyPrinting()
					.registerTypeAdapter(BioimageIoModel.class, new ModelDeserializer())
					.registerTypeAdapter(BioimageIoResource.class, new ResourceDeserializer())
					.registerTypeAdapter(BioimageIoDataset.class, new DatasetDeserializer())
					.registerTypeAdapter(double[].class, new DoubleArrayDeserializer())
					.registerTypeAdapter(Author.class, new Author.Deserializer())
					.registerTypeAdapter(WeightsEntry.class, new WeightsEntry.Deserializer())
					.registerTypeAdapter(WeightsMap.class, new WeightsMap.Deserializer())
					.registerTypeAdapter(Shape.class, new ShapeDeserializer())
					.registerTypeAdapter(Processing.class, new Processing.ProcessingDeserializer())
					.registerTypeAdapter(Axis[].class, new AxesDeserializer())
					.registerTypeAdapter(Processing.ProcessingMode.class, new Processing.ModeDeserializer())
					.setDateFormat("yyyy-MM-dd'T'HH:mm:ss");


			var gson = builder.create();

			var json = gson.toJson(map);
			
			return gson.fromJson(json, BioimageIoModel.class);
		} catch (Exception e) {
            throw new IOException(e);
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
	
	/**
	 * List of file names that may contain the model.
	 * Names should be checked in order, with preference given to the first that is found.
	 */
	static final List<String> MODEL_NAMES = List.of("model.yaml", "model.yml", "rdf.yaml", "rdf.yml");
	
	static Path findModelRdf(Path path) throws IOException {
		return findRdf(path, MODEL_NAMES);
	}
	
	private static Path findRdf(Path path, Collection<String> names) throws IOException {
		if (isYamlPath(path)) {
			if (names.isEmpty())
				return path;
			else {
				var name = path.getFileName().toString().toLowerCase();
				if (names.contains(name) || name.startsWith("model") || name.startsWith("rdf"))
					return path;
				return null;
			}
		}
		
		
		if (Files.isDirectory(path)) {
			// Check directory
			try (Stream<Path> pathStream = Files.list(path)) {
				List<Path> yamlFiles = pathStream.filter(BioimageIoSpec::isYamlPath).collect(Collectors.toList());
				if (yamlFiles.isEmpty())
					return null;
				if (yamlFiles.size() == 1)
					return yamlFiles.get(0);
				for (var name : MODEL_NAMES) {
					var modelFile = yamlFiles.stream()
							.filter(p -> p.getFileName().toString().equalsIgnoreCase(name))
							.findFirst()
							.orElse(null);
					if (modelFile != null)
						return modelFile;
				}
			}
		} else if (path.toAbsolutePath().toString().toLowerCase().endsWith(".zip")) {
			// Check zip file
			try (var fs = FileSystems.newFileSystem(path, (ClassLoader)null)) {
				for (var dir : fs.getRootDirectories()) {
					for (var name : MODEL_NAMES) {
						var p = dir.resolve(name);
						if (Files.exists(p))
							return p;
					}
				}
			}
		}
		return null;
	}
	
	static boolean isYamlPath(Path path) {
		if (Files.isRegularFile(path)) {
			var name = path.getFileName().toString().toLowerCase();
			return name.endsWith(".yaml") || name.endsWith(".yml");
		}
		return false;
	}

	private static class ResourceDeserializer implements JsonDeserializer<BioimageIoResource> {
		
		@Override
		public BioimageIoResource deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
			if (json.isJsonNull())
				return null;
			var obj = json.getAsJsonObject();
			BioimageIoResource resource = new BioimageIoResource();
			deserializeResourceFields(resource, obj, context, true);
			return resource;
		}
		
	}
	
	private static class DatasetDeserializer implements JsonDeserializer<BioimageIoDataset> {
		
		@Override
		public BioimageIoDataset deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
			if (json.isJsonNull())
				return null;
			var obj = json.getAsJsonObject();
			BioimageIoDataset dataset = new BioimageIoDataset();
			// Deserialize, but not strictly... i.e. allow nulls because we might just have an ID
			deserializeResourceFields(dataset, obj, context, false);
			return dataset;
		}
		
	}
	
	
	private static void deserializeResourceFields(BioimageIoResource resource, JsonObject obj, JsonDeserializationContext context, boolean doStrict) {
		
		resource.formatVersion = deserializeField(context, obj, "format_version", String.class, doStrict);
		resource.authors = deserializeField(context, obj, "authors", parameterizedListType(Author.class), doStrict);
		resource.description = deserializeField(context, obj, "description", String.class, doStrict);
		resource.documentation = deserializeField(context, obj, "documentation", String.class, doStrict);
		
		resource.license = deserializeField(context, obj, "license", String.class, doStrict);
		resource.name = deserializeField(context, obj, "name", String.class, doStrict);
		
		if (obj.has("attachments")) {
			if (obj.get("attachments").isJsonObject())
				resource.attachments = deserializeField(context, obj, "attachments", Map.class, Collections.emptyMap());
			else
				logger.warn("Can't parse attachments (not a dict)");
		}
		resource.badges = deserializeField(context, obj, "badges", parameterizedListType(Badge.class), Collections.emptyList());
		resource.cite = deserializeField(context, obj, "cite", parameterizedListType(CiteEntry.class), Collections.emptyList());

		resource.covers = deserializeField(context, obj, "covers", parameterizedListType(String.class), Collections.emptyList());
		resource.downloadURL = deserializeField(context, obj, "download_url", String.class, null);
		resource.gitRepo = deserializeField(context, obj, "git_repo", String.class, null);			
		resource.icon = deserializeField(context, obj, "icon", String.class, null);
		resource.id = deserializeField(context, obj, "id", String.class, null);
		resource.links = deserializeField(context, obj, "links", parameterizedListType(String.class), Collections.emptyList());
		resource.maintainers = deserializeField(context, obj, "maintainers", parameterizedListType(Author.class), Collections.emptyList());

		resource.rdfSource = deserializeField(context, obj, "rdf_source", String.class, null);

		resource.tags = deserializeField(context, obj, "tags", parameterizedListType(String.class), Collections.emptyList());

		resource.trainingData = deserializeField(context, obj, "training_data", BioimageIoDataset.class, null);

		resource.version = deserializeField(context, obj, "version", String.class, null);

	}

	
	private static void deserializeModelFields(BioimageIoModel model, JsonObject obj, JsonDeserializationContext context, boolean doStrict) {
		model.inputs = deserializeField(context, obj, "inputs", parameterizedListType(InputTensor.class), doStrict);
		List<BaseTensor> tensors = new ArrayList<>(List.copyOf(model.inputs));
		for (var tensor: model.inputs) {
			tensor.validate(tensors);
		}


		if (model.isNewerThan("0.5.0")) {
			model.testInputs = List.of();
			model.testOutputs = List.of();
			model.timestamp = "";
		} else {
			// now part of the tensor spec:
			model.testInputs = deserializeField(context, obj, "test_inputs", parameterizedListType(String.class), doStrict);
			// now part of the tensor spec:
			model.testOutputs = deserializeField(context, obj, "test_outputs", parameterizedListType(String.class), doStrict);
			// removed...?
			model.timestamp = deserializeField(context, obj, "timestamp", String.class, doStrict);
		}

		model.weights = deserializeField(context, obj, "weights", WeightsMap.class, doStrict);

		model.config = deserializeField(context, obj, "config", Map.class, Collections.emptyMap());

		model.outputs = deserializeField(context, obj, "outputs", parameterizedListType(OutputTensor.class), Collections.emptyList());
		tensors.addAll(model.outputs);
		for (var tensor: model.outputs) {
			tensor.validate(tensors);
		}
		model.packagedBy = deserializeField(context, obj, "packaged_by", parameterizedListType(Author.class), Collections.emptyList());

		model.parent = deserializeField(context, obj, "parent", ModelParent.class, null);

		if (obj.has("run_mode")) {
			if (obj.get("run_mode").isJsonObject())
				model.runMode = deserializeField(context, obj, "run_mode", Map.class, Collections.emptyMap());
			else
				logger.warn("Can't parse run_mode (not an object)");
		}

		model.sampleInputs = deserializeField(context, obj, "sample_inputs", parameterizedListType(String.class), Collections.emptyList());
		model.sampleOutputs = deserializeField(context, obj, "sample_outputs", parameterizedListType(String.class), Collections.emptyList());
	}
	
	private static Type parameterizedListType(Type typeOfList) {
		return TypeToken.getParameterized(List.class, typeOfList).getType();
	}
	
	private static class ModelDeserializer implements JsonDeserializer<BioimageIoModel> {
		
		@Override
		public BioimageIoModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			
			if (json.isJsonNull())
				return null;
			
			var model = new BioimageIoModel();
			
			var obj = json.getAsJsonObject();
			
			deserializeResourceFields(model, obj, context, true);
			deserializeModelFields(model, obj, context, true);

			return model;
		}
	}
	
	private static class ShapeDeserializer implements JsonDeserializer<Shape> {

		@Override
		public Shape deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			
			if (json.isJsonNull())
				return null;
			
			if (json.isJsonArray()) {
				var shape = new Shape();
				shape.shape = context.deserialize(json, int[].class);
				return shape;
			}
			var obj = json.getAsJsonObject();
			if (obj.has("min") && obj.has("step")) {
				return context.deserialize(obj, Shape.ParameterizedInputShape.class);
			}
			if (obj.has("offset") && obj.has("scale")) {
				return context.deserialize(obj, Shape.ImplicitOutputShape.class);
			}
			throw new JsonParseException("Can't deserialize unknown shape: " + json);
		}
		
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
	private static <T> T deserializeField(JsonDeserializationContext context, JsonObject obj, String name, Type typeOfT, boolean doStrict) throws IllegalArgumentException {
		if (doStrict && !obj.has(name))
			throw new IllegalArgumentException("Required field " + name + " not found");
		return deserializeField(context, obj, name, typeOfT, null);
	}

	private static <T> T deserializeField(JsonDeserializationContext context, JsonObject obj, String name, Type typeOfT, T defaultValue) {
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
	 * General resource, based upon the RDF.
	 * <p>
	 * For machine learning models, you probably want {@link BioimageIoModel}.
	 */
	public static class BioimageIoResource {
				
		private String formatVersion;
		private List<Author> authors;
		private String description;
		private String documentation;

		private String name;
		
		private List<String> tags;
		
		@SerializedName("training_data")
		private BioimageIoDataset trainingData;

		private String version;
		
		private Map<String,?> attachments;
		private List<Badge> badges;
		private List<CiteEntry> cite;
		
		private List<String> covers;

		@SerializedName("download_url")
		private String downloadURL;
		private String gitRepo;
		private String icon;
		private String id;
		private String license;

		private List<String> links;
		private List<Author> maintainers;
		
		private String source;
		
		@SerializedName("rdf_source")
		private String rdfSource;

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
		
		private static WeightsEntry fromKey(String name) {
			for (var v : values()) {
				if (v.key.equals(name) || v.alternatives.contains(name))
					return v;
			}
			return null;
		}
		
		
		private static class Deserializer implements JsonDeserializer<WeightsEntry> {

			@Override
			public WeightsEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				return WeightsEntry.fromKey(json.toString());
			}
			
		}
		
	}
	
	private static class WeightsMap {
		
		private Map<WeightsEntry, ModelWeights> map;
		
		public Map<String, ModelWeights> withStringKeys() {
			return map == null ? Collections.emptyMap() : map.entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue()));
		}

		private static class Deserializer implements JsonDeserializer<WeightsMap> {

			@Override
			public WeightsMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				
				var weights = new WeightsMap();
				if (json.isJsonObject()) {
					var obj = json.getAsJsonObject();
					var map = new LinkedHashMap<WeightsEntry, ModelWeights>();
					for (var key : json.getAsJsonObject().keySet()) {
						var we = WeightsEntry.fromKey(key);
						if (we == null) {
							logger.warn("Unsupported weights: {}", key);
							continue;
						}
						map.put(we, context.deserialize(obj.get(key), ModelWeights.class));
					}
					weights.map = Collections.unmodifiableMap(map);
				}
				return weights;
			}
			
		}
		
	}
	
	
	/**
	 * Resource based on the main model spec.
	 * This extends {@link BioimageIoResource} to provide more information 
	 * relevant for to machine learning models.
	 */
	public static class BioimageIoModel extends BioimageIoResource {
		
		private URI baseURI;
		private URI uri;
				
		private List<InputTensor> inputs;
				
		@SerializedName("test_inputs")
		private List<String> testInputs;
		@SerializedName("test_outputs")
		private List<String> testOutputs;
		
		// Should be in ISO 8601 format - but preserve string as it is found
		private String timestamp;
		
		private WeightsMap weights;

		private Map<?, ?> config;
		
		private List<OutputTensor> outputs;
		
		@SerializedName("packaged_by")
		private List<Author> packagedBy;
		
		private ModelParent parent;
				
		// TODO: Handle run_mode properly
		@SerializedName("run_mode")
		private Map<?, ?> runMode;
		
		@SerializedName("sample_inputs")
		private List<String> sampleInputs;
		@SerializedName("sample_outputs")
		private List<String> sampleOutputs;

		
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
		
		private String affiliation;
		@SerializedName("github_user")
		private String githubUser;
		private String name;
		private String orcid;
		
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
		
		
		private static class Deserializer implements JsonDeserializer<Author> {

			@Override
			public Author deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				
				if (json.isJsonNull())
					return null;
				
				var author = new Author();
				if (json.isJsonObject()) {
					var obj = json.getAsJsonObject();
					author.name = deserializeField(context, obj, "name", String.class, null);
					author.affiliation = deserializeField(context, obj, "affiliation", String.class, null);
					author.githubUser = deserializeField(context, obj, "github_user", String.class, null);
					author.orcid = deserializeField(context, obj, "orcid", String.class, null);
				} else if (json.isJsonPrimitive()) {
					author.name = json.getAsString();
				}
				return author;
			}
			
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

	public abstract static class BaseTensor {

		private Axis[] axes;
		@SerializedName("data_type")
		private String dataType;
		private String name;
		String id;
		private Shape shape;

		@SerializedName("data_range")
		private double[] dataRange;

		public Axis[] getAxes() {
			return axes;
		}

		public String getDataType() {
			return dataType;
		}

		public String getName() {
			return name;
		}

		public Shape getShape() {
			return shape != null ? shape : new Shape.SizesShape(Arrays.stream(axes).map(Axis::getSize).collect(Collectors.toList()));
		}

		public double[] getDataRange() {
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
		
		public int[] getShapeStep() {
			return shape == null ? new int[0] : new int[shape.length];
		}
		
		public double[] getScale() {
			if (shape == null)
				return new double[0];
			var scale = new double[shape.length];
			Arrays.fill(scale, 1.0);
			return scale;
		}

		public double[] getOffset() {
			return shape == null ? new double[0] : new double[shape.length];
		}
		
		@Override
		public String toString() {
			if (shape == null)
				return "Shape (unknown)";
			return "Shape (" + Arrays.toString(shape) + ")";
		}

		private void validate(List<? extends BaseTensor> otherTensors) {
		}

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

			SizesShape(Size... sizes) {
				this(Arrays.asList(sizes));
			}

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
	 * Get the old "bcyx" style axis representation of an Axis array.
	 * @param axes The Axis array.
	 * @return A string representing the axis types.
	 */
	public static String getAxesString(Axis[] axes) {
		return Arrays.stream(axes).map(a -> a.getType().toString()).collect(Collectors.joining());
	}
	
	/**
	 * Base class for pre- and post-processing operations.
	 * See the model spec for details.
	 */
	public static class Processing {

		/**
		 * Requested mode for pre- or post-processing.
		 */
		public enum ProcessingMode { FIXED, PER_DATASET, PER_SAMPLE }

		private final String name;
		private Map<String, Object> kwargs;

		Processing(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Map<String, ?> getKwargs() {
			return kwargs == null ? Collections.emptyMap() : Collections.unmodifiableMap(kwargs);
		}


		public static class Binarize extends Processing {

			Binarize() {
				super("binarize");
			}

			private double threshold = Double.NaN;

			public double getThreshold() {
				return threshold;
			}

		}

		public static class Clip extends Processing {

			private double min;
			private double max;

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

		public static class ScaleLinear extends Processing {

			private double[] gain;
			private double[] offset;
			private Axis[] axes;

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

		public static class Sigmoid extends Processing {

			Sigmoid() {
				super("sigmoid");
			}

		}

		protected abstract static class ProcessingWithMode extends Processing {

			private ProcessingMode mode = ProcessingMode.PER_SAMPLE;
			private Axis[] axes;
			private double eps = 1e-6;

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


		public static class ScaleMeanVariance extends ProcessingWithMode {

			private String referenceTensor;

			ScaleMeanVariance() {
				super("scale_mean_variance");
			}

			public String getReferenceTensor() {
				return referenceTensor;
			}

		}


		public static class ScaleRange extends ProcessingWithMode {

			private String referenceTensor; // TODO: Figure out whether to use this somehow

			private double minPercentile = 0.0;
			private double maxPercentile = 100.0;

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


		public static class ZeroMeanUnitVariance extends ProcessingWithMode {

			private double[] mean;
			private double[] std;

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

		private static class ProcessingDeserializer implements JsonDeserializer<Processing> {

			@Override
			public Processing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				
				if (json.isJsonNull())
					return null;
				
				var obj = json.getAsJsonObject();
				var name = obj.has("name") ? obj.get("name").getAsString() : obj.get("id").getAsString();
				var kwargs = obj.has("kwargs") ? obj.get("kwargs").getAsJsonObject() : null;
				switch (name) {
				case "binarize":
					var binarize = new Binarize();
					binarize.threshold = deserializeField(context, kwargs, "threshold", Double.class, true);
					return binarize;
				case "clip":
					var clip = new Clip();
					clip.min = deserializeField(context, kwargs, "min", Double.class, Double.NEGATIVE_INFINITY);
					clip.max = deserializeField(context, kwargs, "max", Double.class, Double.POSITIVE_INFINITY);
					return clip;
				case "scale_linear":
					var scaleLinear = new ScaleLinear();
					scaleLinear.axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
					scaleLinear.gain = deserializeField(context, kwargs, "gain", double[].class, false);
					scaleLinear.offset = deserializeField(context, kwargs, "offset", double[].class, false);
					return scaleLinear;
				case "scale_mean_variance":
					var scaleMeanVariance = new ScaleMeanVariance();
					((ProcessingWithMode)scaleMeanVariance).mode = deserializeField(context, kwargs, "mode", ProcessingMode.class, false);
					((ProcessingWithMode)scaleMeanVariance).axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
					((ProcessingWithMode)scaleMeanVariance).eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
					scaleMeanVariance.referenceTensor = deserializeField(context, kwargs, "reference_tensor", String.class, null);
					return scaleMeanVariance;
				case "scale_range":
					var scaleRange = new ScaleRange();
					((ProcessingWithMode)scaleRange).mode = deserializeField(context, kwargs, "mode", ProcessingMode.class, false);
					((ProcessingWithMode)scaleRange).axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
					((ProcessingWithMode)scaleRange).eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
					scaleRange.referenceTensor = deserializeField(context, obj, "reference_tensor", String.class, null);
					scaleRange.maxPercentile = deserializeField(context, kwargs, "max_percentile", Double.class, 0.0);
					scaleRange.minPercentile = deserializeField(context, kwargs, "min_percentile", Double.class, 100.0);
					return scaleRange;
				case "sigmoid":
					return new Sigmoid();
				case "zero_mean_unit_variance":
					var zeroMeanUnitVariance = new ZeroMeanUnitVariance();
					((ProcessingWithMode)zeroMeanUnitVariance).mode = deserializeField(context, kwargs, "mode", ProcessingMode.class, false);
					((ProcessingWithMode)zeroMeanUnitVariance).axes = deserializeField(context, kwargs, "axes", Axis[].class, false);
					((ProcessingWithMode)zeroMeanUnitVariance).eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
					zeroMeanUnitVariance.mean = deserializeField(context, kwargs, "mean", double[].class, false);
					zeroMeanUnitVariance.std = deserializeField(context, kwargs, "std", double[].class, false);
					return zeroMeanUnitVariance;
				default:
					var processing = new Processing(name);
					processing.kwargs = kwargs == null ? Collections.emptyMap() : context.deserialize(kwargs, Map.class);
					return processing;
				}
			}
			
		}
		
		
		private static class ModeDeserializer implements JsonDeserializer<ProcessingMode> {

			@Override
			public ProcessingMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				
				if (json.isJsonNull())
					return null;
				
				var s = json.getAsString();
				for (var mode : ProcessingMode.values()) {
					if (s.equals(mode.name()))
						return mode;
				}

				for (var mode : ProcessingMode.values()) {
					if (s.equalsIgnoreCase(mode.name()))
						return mode;
				}
				
				logger.warn("Unknown processing mode: {}", s);
				return null;
			}
			
		}
		
	}

	private static Size deserializeSize(JsonDeserializationContext context, JsonElement jsonElement, JsonElement scale) {
		if (jsonElement == null || jsonElement.isJsonNull()) {
			return null;
		}
		if (jsonElement.isJsonPrimitive()) {
			return new FixedSize(jsonElement.getAsInt());
		}
		if (jsonElement.isJsonObject()) {
			JsonObject obj = jsonElement.getAsJsonObject();
			if (obj.has("min") && obj.has("step")) {
				return context.deserialize(obj, ParameterizedSize.class);
			}
			if (obj.has("min") && obj.has("max")) {
				return context.deserialize(obj, DataDependentSize.class);
			}
			if (obj.has("axis_id") && obj.has("tensor_id")) {
				return new ReferencedSize(obj.get("axis_id").getAsString(), obj.get("tensor_id").getAsString(), scale, obj.get("offset"));
			}
		}
		throw new JsonParseException("No idea what type of size this is, sorry!");
	}

	public static class AxesDeserializer implements JsonDeserializer<Axis[]> {
		@Override
		public Axis[] deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
			if (jsonElement.isJsonPrimitive()) {
				var s = jsonElement.getAsString();
				Axis[] axes = new Axis[s.length()];
				for (int i = 0; i < axes.length; i++) {
					axes[i] = new CharAxis(s.charAt(i));
				}
				return axes;
			}
			if (jsonElement.isJsonArray()) {
				var arr = jsonElement.getAsJsonArray();
				Axis[] axes = new Axis[arr.size()];
				for (int i = 0; i < axes.length; i++) {
					var curr = arr.get(i);
					if (curr.isJsonPrimitive()) {
						axes[i] = new CharAxis(curr.getAsString().charAt(0));
						continue;
					}

					var oj = curr.getAsJsonObject();
					var id = oj.get("id");
					var desc = oj.get("description");
					Size size = deserializeSize(jsonDeserializationContext, oj.get("size"), oj.get("scale"));
					switch (oj.get("type").getAsString()) {
						case "time":
							axes[i] = new TimeInputAxis(
									id, desc,
									TimeUnit.valueOf(oj.get("unit").getAsString().toUpperCase()),
									oj.get("scale").getAsDouble(),
									size
							);
							break;
						case "channel":
							var namesJSON = oj.get("channel_names").getAsJsonArray();
							List<String> names = new LinkedList<>();
							for (JsonElement n : namesJSON) {
								names.add(n.getAsString());
							}
							axes[i] = new ChannelAxis(
									id, desc,
									names
							);
							break;
						case "index":
							axes[i] = new IndexInputAxis(id, desc, size);
							break;
						case "space":
							axes[i] = new SpaceInputAxis(
									id, desc,
									oj.get("unit").getAsString().toUpperCase(),
									oj.get("scale").getAsDouble(),
									size
							);
							break;
						case "batch":
							axes[i] = new BatchAxis(id, desc, oj.get("size"));
							break;
						default:
							logger.error("Unknown object {}", oj);
							axes[i] = null;
					}
				}
				return axes;
			}
			logger.error("Unknown JSON element {}", jsonElement);
			return null;
		}
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
		void validate(List<? extends BaseTensor> tensors);
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
		public void validate(List<? extends BaseTensor> tensors) {
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

		AxisBase(JsonElement id, JsonElement description) {
			this(id == null ? "": id.getAsString().toLowerCase(), description == null ? "": description.getAsString());
		}

		@Override
		public String getID() {
			return this.id;
		}
	}

	static class BatchAxis extends AxisBase {
		private final int size;
		private final boolean concatenable = true;
		BatchAxis(JsonElement id, JsonElement description, JsonElement size) {
			super(id, description);
			int s = 1;
			if (size != null) {
				s = size.getAsInt();
			}
			this.size = s;
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
		public void validate(List<? extends BaseTensor> tensors) {
			// fixed size doesn't need validation
        }
	}
	
	static class ChannelAxis extends AxisBase implements ScaledAxis {
		private final List<String> channel_names;

		ChannelAxis(JsonElement id, JsonElement description, List<String> channel_names) {
			super(id, description);
			this.channel_names = List.copyOf(channel_names);
		}

		@Override
		public Size getSize() {
			return new FixedSize(channel_names.size());
		}

		@Override
		public void validate(List<? extends BaseTensor> tensors) {
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

		IndexAxisBase(JsonElement id, JsonElement description) {
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

		IndexInputAxis(JsonElement id, JsonElement description, Size size) {
			super(id, description);
			this.size = size;
		}
		@Override
		public Size getSize() {
			return this.size;
		}

		@Override
		public void validate(List<? extends BaseTensor> tensors) {
			size.validate(tensors);
		}
	}

	static class IndexOutputAxis extends IndexAxisBase {
		private Size size;

		IndexOutputAxis(JsonElement id, JsonElement description) {
			super(id, description);
		}

		@Override
		public void validate(List<? extends BaseTensor> tensors) {
			size.validate(tensors);
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

		void validate(List<? extends BaseTensor> tensors);
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
		public void validate(List<? extends BaseTensor> tensors) {
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
		public void validate(List<? extends BaseTensor> tensors) {
			assert min > 0;
			assert max > min;
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

		ReferencedSize(String refID, String tensorID, JsonElement scale, JsonElement offset) {
			this(refID, tensorID, (scale != null) ? scale.getAsDouble() : 1.0, (offset != null) ? offset.getAsInt() : 0);
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
		public void validate(List<? extends BaseTensor> tensors) {
			var tensor = tensors.stream().filter(t -> t.id.equals(tensorID)).findFirst().orElse(null);
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
		public void validate(List<? extends BaseTensor> tensors) {
			// can't validate ints
		}
	}

	abstract static class TimeAxisBase extends AxisBase implements ScaledAxis {
		private final String type = "time";
		private final TimeUnit unit;
		private final double scale;

		TimeAxisBase(JsonElement id, JsonElement description, TimeUnit unit, double scale) {
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

	}

	public static class TimeInputAxis extends TimeAxisBase {
		private final Size size;

		TimeInputAxis(JsonElement id, JsonElement description, TimeUnit unit, double scale, Size size) {
			super(id, description, unit, scale);
			this.size = size;
		}

		@Override
		public Size getSize() {
			return size;
		}

		@Override
		public void validate(List<? extends BaseTensor> tensors) {
			getSize().validate(tensors);
		}
	}

	public static class TimeOutputAxis extends TimeAxisBase {
		private final Size size;

		TimeOutputAxis(JsonElement id, JsonElement description, TimeUnit unit, double scale, Size size) {
			super(id, description, unit, scale);
			this.size = size;
		}

		@Override
		public Size getSize() {
			return size;
		}
		@Override
		public void validate(List<? extends BaseTensor> tensors) {
			getSize().validate(tensors);
		}
	}

	public static class TimeOutputAxisWithHalo extends TimeOutputAxis implements WithHalo {
		private final int halo;

		TimeOutputAxisWithHalo(JsonElement id, JsonElement description, TimeUnit unit, double scale, Size size, int halo) {
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
		ATTOMETER,
		ANGSTROM,
		CENTIMETER,
		DECIMETER,
		EXAMETER,
		FEMTOMETER,
		FOOT,
		GIGAMETER,
		HECTOMETER,
		INCH,
		KILOMETER,
		MEGAMETER,
		METER,
		MICROMETER,
		MILE,
		MILLIMETER,
		NANOMETER,
		PARSEC,
		PETAMETER,
		PICOMETER,
		TERAMETER,
		YARD,
		YOCTOMETER,
		YOTTAMETER,
		ZEPTOMETER,
		ZETTAMETER
	}

	public enum BioImageIoVersion {
		VERSION_0_4("0.4"),
		VERSION_0_5("0.5");
		private final String version;

		BioImageIoVersion(String s) {
			this.version = s;
		}
	}

	public abstract static class SpaceAxisBase extends AxisBase implements ScaledAxis {
		private final SpaceUnit unit;
		private double scale = 1.0;

		SpaceAxisBase(JsonElement id, JsonElement description, String unit, double scale) {
			this(id, description, SpaceUnit.valueOf(unit.toUpperCase()), scale);
		}

		SpaceAxisBase(JsonElement id, JsonElement description, SpaceUnit unit, double scale) {
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
	}

	public static class SpaceInputAxis extends SpaceAxisBase {
		private final Size size;
		private final boolean concatenable = false;

		SpaceInputAxis(JsonElement id, JsonElement description, String unit, double scale, Size size) {
			super(id, description, unit, scale);
			this.size = size;
		}

		@Override
		public Size getSize() {
			return this.size;
		}
		@Override
		public void validate(List<? extends BaseTensor> tensors) {
			getSize().validate(tensors);
		}
	}

	public static class SpaceOutputAxis extends SpaceAxisBase {
		private final Size size;

		SpaceOutputAxis(JsonElement id, JsonElement description, String unit, double scale, Size size) {
			super(id, description, unit, scale);
			this.size = size;
		}

		@Override
		public Size getSize() {
			return this.size;
		}
		@Override
		public void validate(List<? extends BaseTensor> tensors) {
			getSize().validate(tensors);
		}
	}

	static class SpaceOutputAxisWithHalo extends SpaceOutputAxis implements WithHalo {
		private ReferencedSize size;
		private int halo = 0;

		SpaceOutputAxisWithHalo(JsonElement id, JsonElement description, String unit, double scale, Size size, int halo) {
			super(id, description, unit, scale, size);
			this.halo = halo;
		}
		public int getHalo() {
			return this.halo;
		}
	}

	/**
	 * An axis with a halo.
	 * The halo should be cropped from the output tensor to avoid boundary effects.
	 * It is to be cropped from both sides, i.e. `size_after_crop = size - 2 * halo`.
	 * To document a halo that is already cropped by the model use `size.offset` instead.
	 */
	interface WithHalo {
		/**
		 * Get the size of the halo for this axis.
		 * @return The size of the halo in pixels.
		 */
		int getHalo();
	}

	/**
	 * Deal with the awkwardness of -inf and inf instead of .inf and .inf.
	 * This otherwise caused several failures for data range.
	 */
	private static class DoubleArrayDeserializer implements JsonDeserializer<double[]> {

		@Override
		public double[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {

			if (json.isJsonNull())
				return null;
			if (json.isJsonArray()) {
				List<Double> values = new ArrayList<>();
				for (var jsonVal : json.getAsJsonArray()) {
					if (jsonVal.isJsonNull()) {
						logger.warn("Found null when expecting a double - will replace with NaN");
						values.add(Double.NaN);
						continue;
					}
					var jsonPrimitive = jsonVal.getAsJsonPrimitive();
					if (jsonPrimitive.isNumber()) {
						values.add(jsonPrimitive.getAsDouble());
					} else {
						var s = jsonPrimitive.getAsString();
						if ("inf".equalsIgnoreCase(s))
							values.add(Double.POSITIVE_INFINITY);
						else if ("-inf".equalsIgnoreCase(s))
							values.add(Double.NEGATIVE_INFINITY);
						else
							values.add(Double.parseDouble(s));
					}
				}
				return values.stream().mapToDouble(d -> d).toArray();
			} else
				throw new JsonParseException("Can't parse data range from " + json);
		}
	}
}
