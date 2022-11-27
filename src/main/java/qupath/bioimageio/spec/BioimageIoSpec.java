package qupath.bioimageio.spec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
	 * @param file
	 * @return
	 * @throws IOException if the model cannot be found or parsed
	 */
	public static BioimageIoModel parseModel(File file) throws IOException {
		return parseModel(file.toPath());
	}

	/**
	 * Parse a model from a path.
	 * This can either represent a yaml file, or a directory that contains a yaml file representing the model.
	 * @param path
	 * @return
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
	 * @param stream
	 * @return
	 * @throws IOException if the model cannot be found or parsed
	 */
	public static BioimageIoModel parseModel(InputStream stream) throws IOException {
		
		try {
			// Important to use SafeConstructor to restrict potential classes that might be initiated
			// Note: we can't support -inf or inf (but can support -.inf or .inf)
			Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
			Map<String, ?> map = yaml.load(stream);	
			
			var gson = new GsonBuilder()
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
					.registerTypeAdapter(Processing.class, new Processing.Deserializer())
					.registerTypeAdapter(Processing.ProcessingMode.class, new Processing.ModeDeserializer())
					.setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
					.create();
			
			var json = gson.toJson(map);
			
			return gson.fromJson(json, BioimageIoModel.class);
		} catch (Exception e) {
			if (e instanceof IOException)
				throw e;
			else
				throw new IOException(e);
		}
	}
	
	
	
	/**
	 * Create a shape array for a given axes.
	 * The axes is expected to a string containing only the characters 
	 * {@code bitczyx} as defined in the spec.
	 * <p>
	 * The purpose of this is to build shape arrays easily without needing to 
	 * explicitly handle different axes and dimension ordering.
	 * <p>
	 * An example:
	 * <pre>
	 * <code>
	 * int[] shape = getTargetShape("byxc", Map.of('x', 256, 'y', 512), 1);
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
	static final List<String> MODEL_NAMES = Collections.unmodifiableList(Arrays.asList("model.yaml", "model.yml", "rdf.yaml", "rdf.yml"));
	
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
			List<Path> yamlFiles = Files.list(path).filter(p -> isYamlPath(p)).collect(Collectors.toList());
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
		} else if (path.toAbsolutePath().toString().toLowerCase().endsWith(".zip")) {
			// Check zip file
			var fs = FileSystems.newFileSystem(path, (ClassLoader)null);
			for (var dir : fs.getRootDirectories()) {
				for (var name : MODEL_NAMES) {
					var p = dir.resolve(name);
					if (Files.exists(p))
						return p;
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
		
		model.testInputs = deserializeField(context, obj, "test_inputs", parameterizedListType(String.class), doStrict);
		model.testOutputs = deserializeField(context, obj, "test_outputs", parameterizedListType(String.class), doStrict);

		model.timestamp = deserializeField(context, obj, "timestamp", String.class, doStrict);
		
		model.weights = deserializeField(context, obj, "weights", WeightsMap.class, doStrict);

		model.config = deserializeField(context, obj, "config", Map.class, Collections.emptyMap());

		model.outputs = deserializeField(context, obj, "outputs", parameterizedListType(OutputTensor.class), Collections.emptyList());

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
	 * Deserialize a field.
	 * @param <T>
	 * @param context
	 * @param obj
	 * @param name
	 * @param typeOfT
	 * @param doStrict if true, fail if the field is missing; otherwise, return null
	 * @return
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
	 * so as to avoid generating new unmodifiable wrappers later.
	 * @param <T>
	 * @param input
	 * @return
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
		
	}
	
	/**
	 * Dataset spec. Currently this provides equivalent information to {@link BioimageIoResource}.
	 */
	public static class BioimageIoDataset extends BioimageIoResource {}
	
	/**
	 * Enum representing supported model weights.
	 */
	public static enum WeightsEntry {
		
		KERAS_HDF5("keras_hdf5"),
		PYTORCH_STATE_DICT("pytorch_state_dict"),
		TENSORFLOW_JS("tensorflow_js"),
		TENSORFLOW_SAVED_MODEL_BUNDLE("tensorflow_saved_model_bundle"),
		ONNX("onnx"),
		TORCHSCRIPT("torchscript", Set.of("pytorch_script"));
		
		private final String key;
		// Support older key names as alternatives
		private final Set<String> alternatives;

		private WeightsEntry(String key) {
			this(key, Collections.emptySet());
		}
		
		private WeightsEntry(String key, Set<String> alternatives) {
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
		 * @return
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
		 * @return
		 */
		public URI getURI() {
			return uri;
		}
		
		/**
		 * Get a map view of the weights. It's generally better to use {@link #getWeights()}.
		 * @return
		 */
		public Map<String, ModelWeights> getWeights() {
			return weights == null ? Collections.emptyMap() : weights.withStringKeys();
		}
		
		/**
		 * Alternative to {@link #getWeights()} that corrects for keys that have been renamed.
		 * @param key
		 * @return
		 */
		public ModelWeights getWeights(WeightsEntry key) {
			if (weights == null || key == null)
				return null;
			return weights.map.getOrDefault(key, null);
		}
		
		/**
		 * Alternative to {@link #getWeights(WeightsEntry)} using a string key.
		 * @param key
		 * @return
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
	 * @param <T>
	 * @param list
	 * @return
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
	 * Model parent. Currently this provides only an ID and URI.
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

	private static class BaseTensor {

		private String axes;
		@SerializedName("data_type")
		private String dataType;
		private String name;
		
		private Shape shape;
		
		@SerializedName("data_range")
		private double[] dataRange;

		public String getAxes() {
			return axes;
		}
		
		public String getDataType() {
			return dataType;
		}
		
		public String getName() {
			return name;
		}
		
		public Shape getShape() {
			return shape;
		}
		
		public double[] getDataRange() {
			return dataRange == null ? null : dataRange.clone();
		}
		
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
						if ("inf".equals(s.toLowerCase()))
							values.add(Double.POSITIVE_INFINITY);
						else if ("-inf".equals(s.toLowerCase()))
							values.add(Double.NEGATIVE_INFINITY);
						else
							values.add(Double.parseDouble(s));
					}
				}
				return values.stream().mapToDouble(d -> d.doubleValue()).toArray();
			} else
				throw new JsonParseException("Can't parse data range from " + json);
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
		 * @return
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
		 * @param target
		 * @return
		 */
		public int[] getTargetShape(int... target) {
			return getShape();
		}
		
		/**
		 * Get the number of elements in the shape array.
		 * @return
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
		public static enum ProcessingMode { FIXED, PER_DATASET, PER_SAMPLE }

		private String name;
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
			private String axes;

			ScaleLinear() {
				super("scale_linear");
			}

			public double[] getGain() {
				return gain == null ? null : gain.clone();
			}

			public double[] getOffset() {
				return offset == null ? null : offset.clone();
			}

			public String getAxes() {
				return axes;
			}

		}

		public static class Sigmoid extends Processing {

			Sigmoid() {
				super("sigmoid");
			}

		}

		protected abstract static class ProcessingWithMode extends Processing {

			private Processing.ProcessingMode mode = Processing.ProcessingMode.PER_SAMPLE;
			private String axes;
			private double eps = 1e-6;

			ProcessingWithMode(String name) {
				super(name);
			}

			public double getEps() {
				return eps;
			}

			public Processing.ProcessingMode getMode() {
				return mode;
			}

			public String getAxes() {
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

		
		
		private static class Deserializer implements JsonDeserializer<Processing> {

			@Override
			public Processing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				
				if (json.isJsonNull())
					return null;
				
				var obj = json.getAsJsonObject();
				var name = obj.get("name").getAsString();
				var kwargs = obj.has("kwargs") ? obj.get("kwargs").getAsJsonObject() : null;
				switch (name) {
				case "binarize":
					var binarize = new Processing.Binarize();
					binarize.threshold = deserializeField(context, kwargs, "threshold", Double.class, true);
					return binarize;
				case "clip":
					var clip = new Processing.Clip();
					clip.min = deserializeField(context, kwargs, "min", Double.class, Double.NEGATIVE_INFINITY);
					clip.max = deserializeField(context, kwargs, "max", Double.class, Double.POSITIVE_INFINITY);
					return clip;
				case "scale_linear":
					var scaleLinear = new Processing.ScaleLinear();
					scaleLinear.axes = deserializeField(context, kwargs, "axes", String.class, false);
					scaleLinear.gain = deserializeField(context, kwargs, "gain", double[].class, false);
					scaleLinear.offset = deserializeField(context, kwargs, "offset", double[].class, false);
					return scaleLinear;
				case "scale_mean_variance":
					var scaleMeanVariance = new Processing.ScaleMeanVariance();
					((ProcessingWithMode)scaleMeanVariance).mode = deserializeField(context, kwargs, "mode", Processing.ProcessingMode.class, false);
					((ProcessingWithMode)scaleMeanVariance).axes = deserializeField(context, kwargs, "axes", String.class, false);
					((ProcessingWithMode)scaleMeanVariance).eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
					scaleMeanVariance.referenceTensor = deserializeField(context, kwargs, "reference_tensor", String.class, null);
					return scaleMeanVariance;
				case "scale_range":
					var scaleRange = new Processing.ScaleRange();
					((ProcessingWithMode)scaleRange).mode = deserializeField(context, kwargs, "mode", Processing.ProcessingMode.class, false);
					((ProcessingWithMode)scaleRange).axes = deserializeField(context, kwargs, "axes", String.class, false);
					((ProcessingWithMode)scaleRange).eps = deserializeField(context, kwargs, "eps", Double.class, 1e-6);
					scaleRange.referenceTensor = deserializeField(context, obj, "reference_tensor", String.class, null);
					scaleRange.maxPercentile = deserializeField(context, kwargs, "max_percentile", Double.class, 0.0);
					scaleRange.minPercentile = deserializeField(context, kwargs, "min_percentile", Double.class, 100.0);
					return scaleRange;
				case "sigmoid":
					return new Sigmoid();
				case "zero_mean_unit_variance":
					var zeroMeanUnitVariance = new Processing.ZeroMeanUnitVariance();
					((ProcessingWithMode)zeroMeanUnitVariance).mode = deserializeField(context, kwargs, "mode", Processing.ProcessingMode.class, false);
					((ProcessingWithMode)zeroMeanUnitVariance).axes = deserializeField(context, kwargs, "axes", String.class, false);
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
		
		
		private static class ModeDeserializer implements JsonDeserializer<Processing.ProcessingMode> {

			@Override
			public Processing.ProcessingMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				
				if (json.isJsonNull())
					return null;
				
				var s = json.getAsString();
				for (var mode : Processing.ProcessingMode.values()) {
					if (s.equals(mode.name()))
						return mode;
				}

				for (var mode : Processing.ProcessingMode.values()) {
					if (s.equalsIgnoreCase(mode.name()))
						return mode;
				}
				
				logger.warn("Unknown processing mode: {}", s);
				return null;
			}
			
		}
		
	}
	
}