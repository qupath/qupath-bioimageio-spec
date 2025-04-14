# QuPath BioImage Model Zoo Spec

Support for parsing the [BioImage Model Zoo spec](https://github.com/bioimage-io/spec-bioimage-io) in Java.

With thanks to @bioimage-io for all their work in creating this spec and the model zoo.

## Purpose

This was written mostly to help support the BioImage Model Zoo in [QuPath](https://qupath.github.io).

It's separated out from QuPath's code in case it could be useful to anyone else.

## Dependencies

The spec has 3 dependencies:

* SnakeYAML
* Gson
* slf4j-api

Gson might be removed as a dependency in the future.

## Compatibility

The code here should be compatible with Java 17.

It was written for the model spec v0.4.x, and updated to handle v0.5.x models, but attempts to also handle most yaml files written with the model spec v0.3.x.

## Building

The jar can be built with:
```
gradlew build
```

There are a few sample model spec files for testing.
To test more, you can pass a base directory and the test code will search recursively (up to 5 levels deep) to find model yaml files to read.
```
gradlew test -Ptest.models=path/to/base/directory
```

> Note: The testing isn't very extensive. Currently, it just checks the models can be parsed without exceptions.
