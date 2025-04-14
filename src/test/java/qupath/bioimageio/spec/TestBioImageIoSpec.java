package qupath.bioimageio.spec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static qupath.bioimageio.spec.Model.findModelRdf;
import static qupath.bioimageio.spec.Model.isYamlPath;
import static qupath.bioimageio.spec.tensor.Shape.createShapeArray;

/**
 * Test parsing the spec.
 */
class TestBioImageIoSpec {
	
	private static final Logger logger = LoggerFactory.getLogger(TestBioImageIoSpec.class);
	
	static Collection<Path> provideYamlPaths() throws IOException, URISyntaxException {
		
		Path path;
		var testPath = System.getProperty("models", null);
		if (testPath != null) {
			path = Paths.get(testPath);
		} else {
			path = Paths.get(Objects.requireNonNull(TestBioImageIoSpec.class.getResource("/specs")).toURI());
		}
		int testDepth = Integer.parseInt(System.getProperty("depth", "5"));
		
		if (containsModel(path))
			return Collections.singletonList(path);
		else if (Files.isDirectory(path)) {
			try (var fstream = Files.walk(path, testDepth)) {
				return fstream
						.filter(p -> containsModel(p) || p.toString().endsWith(".zip"))
						.collect(Collectors.toSet());
			}
		} else {
			logger.error("No yaml files found to test!");
			return Collections.emptyList();
		}
	}
	
	
	private static boolean containsModel(Path path) {
		try {
			if (findModelRdf(path) != null)
				return true;
			// Accept also yaml files starting with model or rdf
			// (but ignore things like environment.yml)
			if (isYamlPath(path)) {
				var name = path.getFileName().toString().toLowerCase();
				return name.startsWith("model") || name.startsWith("rdf");
			}
		} catch (IOException e) {
			logger.error(e.getLocalizedMessage(), e);
		}
		return false;
	}

	/**
	 * Test that the model in a specific path can be parsed.
	 * @param path The path.
	 */
	@ParameterizedTest
	@MethodSource("provideYamlPaths")
	void testParseSpec(Path path) {
		try {
			logger.info("Attempting to parse {}", path);
			var model = Model.parse(path);
			assertNotNull(model);
			if (Files.isDirectory(path))
				assertEquals(path.toUri(), model.getBaseURI());
		} catch (IOException e) {
			fail(e);
		}
	}
	
	
	/**
	 * Test creating shapes for different axes strings.
	 */
	@Test
	void testCreateShape() {
		assertArrayEquals(new int[]{1, 512, 256, 1}, createShapeArray("byxc", Map.of('x', 256, 'y', 512), 1));
		assertArrayEquals(new int[]{256, 512, -1}, createShapeArray("xyc", Map.of('x', 256, 'y', 512), -1));
		assertArrayEquals(new int[]{3, 4, 5, 6}, createShapeArray("xyct", Map.of('x', 3, 'y', 4, 'c', 5, 't', 6), -1));
	}
}
