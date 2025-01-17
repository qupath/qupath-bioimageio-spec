package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

class Deserializers {
    private Deserializers() {
        throw new UnsupportedOperationException("Do not instantiate this class");
    }

    private final static Logger logger = LoggerFactory.getLogger(Deserializers.class);

}
