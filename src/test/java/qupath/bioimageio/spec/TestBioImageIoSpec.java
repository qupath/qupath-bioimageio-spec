package qupath.bioimageio.spec;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TestBioImageIoSpec {
	
	static List<Path> provideYamlPaths() throws IOException, URISyntaxException {
		
		Path path;
		var testPath = System.getProperty("models", null);
		if (testPath != null) {
			path = Paths.get(testPath);
		} else {
			path = Paths.get(TestBioImageIoSpec.class.getResource("/specs").toURI());			
		}
		if (isModelYaml(path))
			return Collections.singletonList(path);
		else if (Files.isDirectory(path)) {
			return Files.walk(path, 5)
			.filter(TestBioImageIoSpec::isModelYaml)
			.collect(Collectors.toList());
		} else {
			System.err.println("To yaml files found to test!");
			return Collections.emptyList();
		}
	}
	
	
	private static boolean isModelYaml(Path path) {
		if (!Files.isRegularFile(path))
			return false;
		var name = path.getFileName().toString().toLowerCase();
		if (name.endsWith(".yaml") || name.endsWith(".yml"))
			return name.startsWith("rdf") || name.startsWith("model");
		return false;
	}

	@ParameterizedTest
	@MethodSource("provideYamlPaths")
	void testParseSpec(Path path) {
		try {
			System.out.println("Testing " + path);
			var model = BioimageIoSpec.parseModel(path);
			assertNotNull(model);
		} catch (IOException e) {
			fail(e);
		}
	}
	
	

}
