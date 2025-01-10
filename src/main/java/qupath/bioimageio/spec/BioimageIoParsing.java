package qupath.bioimageio.spec;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BioimageIoParsing {
    /**
     * Parse a model from a file or directory.
     * This can either be a yaml file, or a directory that contains a yaml file representing the model.
     * @param file The file containing the YAML, or its parent directory.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static BioimageIoSpec.BioimageIoModel parseModel(File file) throws IOException {
        return parseModel(file.toPath());
    }

    /**
     * Parse a model from a path.
     * This can either represent a yaml file, or a directory that contains a yaml file representing the model.
     * @param path The path to the file containing the YAML, or its parent directory.
     * @return The parsed model.
     * @throws IOException if the model cannot be found or parsed
     */
    public static BioimageIoSpec.BioimageIoModel parseModel(Path path) throws IOException {

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
    public static BioimageIoSpec.BioimageIoModel parseModel(InputStream stream) throws IOException {

        try {
            // Important to use SafeConstructor to restrict potential classes that might be initiated
            // Note: we can't support -inf or inf (but can support -.inf or .inf)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, ?> map = yaml.load(stream);

            var builder = new GsonBuilder()
                    .serializeSpecialFloatingPointValues()
                    .setPrettyPrinting()
                    .registerTypeAdapter(BioimageIoSpec.BioimageIoModel.class, new Deserializers.ModelDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.BioimageIoResource.class, new Deserializers.ResourceDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.BioimageIoDataset.class, new Deserializers.DatasetDeserializer())
                    .registerTypeAdapter(double[].class, new Deserializers.DoubleArrayDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.Author.class, new Deserializers.AuthorDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.WeightsEntry.class, new Deserializers.WeightsEntryDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.WeightsMap.class, new Deserializers.WeightsMapDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.Shape.class, new Deserializers.ShapeDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.Processing.class, new Deserializers.ProcessingDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.Axis[].class, new Deserializers.AxesDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.Processing.ProcessingMode.class, new Deserializers.ProcessingModeDeserializer())
                    .registerTypeAdapter(BioimageIoSpec.TensorDataDescription.class, new Deserializers.TensorDataDescriptionDeserializer())
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss");



            var gson = builder.create();

            var json = gson.toJson(map);

            return gson.fromJson(json, BioimageIoSpec.BioimageIoModel.class);
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
                List<Path> yamlFiles = pathStream.filter(BioimageIoParsing::isYamlPath).collect(Collectors.toList());
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



    static Type parameterizedListType(Type typeOfList) {
        return TypeToken.getParameterized(List.class, typeOfList).getType();
    }

}
