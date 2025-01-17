module io.github.qupath.bioimageio.spec {
	
	requires org.yaml.snakeyaml;
	requires com.google.gson;
	requires org.slf4j;
	requires java.logging;

	exports qupath.bioimageio.spec;
	exports qupath.bioimageio.spec.tmp;
	exports qupath.bioimageio.parsing;

}
