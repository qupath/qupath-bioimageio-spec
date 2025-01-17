package qupath.bioimageio.spec;
import static org.junit.jupiter.api.Assertions.*;
import static qupath.bioimageio.parsing.Parsing.findModelRdf;
import static qupath.bioimageio.parsing.Parsing.isYamlPath;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.bioimageio.parsing.Parsing;

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
			path = Paths.get(TestBioImageIoSpec.class.getResource("/specs").toURI());			
		}
		int testDepth = Integer.parseInt(System.getProperty("depth", "5"));
		
		if (containsModel(path))
			return Collections.singletonList(path);
		else if (Files.isDirectory(path)) {
			return Files.walk(path, testDepth)
			.filter(TestBioImageIoSpec::containsModel)
			.collect(Collectors.toSet());
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
	 * @param path
	 */
	@ParameterizedTest
	@MethodSource("provideYamlPaths")
	void testParseSpec(Path path) {
		try {
			logger.info("Attempting to parse {}", path);
			var model = Parsing.parseModel(path);
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
		assertArrayEquals(new int[]{1, 512, 256, 1}, Utils.createShapeArray("byxc", Map.of('x', 256, 'y', 512), 1));
		assertArrayEquals(new int[]{256, 512, -1}, Utils.createShapeArray("xyc", Map.of('x', 256, 'y', 512), -1));
		assertArrayEquals(new int[]{3, 4, 5, 6}, Utils.createShapeArray("xyct", Map.of('x', 3, 'y', 4, 'c', 5, 't', 6), -1));
	}
}
