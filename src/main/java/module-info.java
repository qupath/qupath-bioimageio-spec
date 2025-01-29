module io.github.qupath.bioimageio.spec {
	
	requires org.yaml.snakeyaml;
	requires com.google.gson;
	requires org.slf4j;

	exports qupath.bioimageio.spec;
	exports qupath.bioimageio.spec.tensor;
	exports qupath.bioimageio.spec.tensor.axes;
	exports qupath.bioimageio.spec.tensor.sizes;
}
