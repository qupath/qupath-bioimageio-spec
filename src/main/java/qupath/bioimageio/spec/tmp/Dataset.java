package qupath.bioimageio.spec.tmp;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;

/**
 * Dataset spec. Currently, this provides equivalent information to {@link Resource}.
 */
public class Dataset extends Resource {

    static class DatasetDeserializer implements JsonDeserializer<Dataset> {

        @Override
        public Dataset deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json.isJsonNull())
                return null;
            var obj = json.getAsJsonObject();
            Dataset dataset = new Dataset();
            // Deserialize, but not strictly... i.e. allow nulls because we might just have an ID
            deserializeResourceFields(dataset, obj, context, false);
            return dataset;
        }

    }

}
