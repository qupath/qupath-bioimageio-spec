package qupath.bioimageio.parsing;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import qupath.bioimageio.spec.tmp.Author;
import qupath.bioimageio.spec.tmp.Dataset;
import qupath.bioimageio.spec.tmp.Model;
import qupath.bioimageio.spec.tmp.Processing;
import qupath.bioimageio.spec.tmp.Resource;
import qupath.bioimageio.spec.tmp.Shape;
import qupath.bioimageio.spec.tmp.Weights;
import qupath.bioimageio.spec.tmp.axes.Axis;
import qupath.bioimageio.spec.tmp.tensor.TensorDataDescription;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parsing {
    /**
     * Parse a model from a file or directory.
     * This can either be a yaml file, or a directory that contains a yaml file representing the model.
     * @param file The file containing the YAML, or its parent directory.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static Model parseModel(File file) throws IOException {
        return parseModel(file.toPath());
    }

    /**
     * Parse a model from a path.
     * This can either represent a yaml file, or a directory that contains a yaml file representing the model.
     * @param path The path to the file containing the YAML, or its parent directory.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static Model parseModel(Path path) throws IOException {

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
     * Note that {@link BioimageIoSpec.BioimageIoModel#getBaseURI()} will return null in this case, because the base URI
     * is unknown.
     * @param stream A stream of YAML.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static Model parseModel(InputStream stream) throws IOException {

        try {
            // Important to use SafeConstructor to restrict potential classes that might be initiated
            // Note: we can't support -inf or inf (but can support -.inf or .inf)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, ?> map = yaml.load(stream);

            var builder = new GsonBuilder()
                    .serializeSpecialFloatingPointValues()
                    .setPrettyPrinting()
                    .registerTypeAdapter(Model.class, new Model.Deserializer())
                    .registerTypeAdapter(Resource.class, new Deserializers.ResourceDeserializer())
                    .registerTypeAdapter(Dataset.class, new Deserializers.DatasetDeserializer())
                    .registerTypeAdapter(double[].class, new DoubleArrayDeserializer())
                    .registerTypeAdapter(Author.class, new Deserializers.AuthorDeserializer())
                    .registerTypeAdapter(Weights.WeightsEntry.class, new Deserializers.WeightsEntryDeserializer())
                    .registerTypeAdapter(Weights.WeightsMap.class, new Deserializers.WeightsMapDeserializer())
                    .registerTypeAdapter(Shape.class, new Shape.Deserializer())
                    .registerTypeAdapter(Processing.class, new Deserializers.ProcessingDeserializer())
                    .registerTypeAdapter(Axis[].class, new Deserializers.AxesDeserializer())
                    .registerTypeAdapter(Processing.ProcessingMode.class, new Deserializers.ProcessingModeDeserializer())
                    .registerTypeAdapter(TensorDataDescription.class, new Deserializers.TensorDataDescriptionDeserializer())
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss");



            var gson = builder.create();

            var json = gson.toJson(map);

            return gson.fromJson(json, Model.class);
        } catch (Exception e) {
            throw new IOException(e);
        }
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
                List<Path> yamlFiles = pathStream.filter(Parsing::isYamlPath).collect(Collectors.toList());
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



    public static Type parameterizedListType(Type typeOfList) {
        return TypeToken.getParameterized(List.class, typeOfList).getType();
    }
    /**
     * Deal with the awkwardness of -inf and inf instead of .inf and .inf.
     * This otherwise caused several failures for data range.
     */
    static class DoubleArrayDeserializer implements JsonDeserializer<double[]> {
        Logger logger = LoggerFactory.getLogger(DoubleArrayDeserializer.class);

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
