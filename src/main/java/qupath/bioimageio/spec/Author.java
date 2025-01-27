package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import static qupath.bioimageio.spec.Model.deserializeField;




/**
 * Author or maintainer.
 * @param name Author name
 * @param affiliation Author affiliation
 * @param githubUser Author's GitHub username
 * @param orcid An <a href="https://support.orcid.org/hc/en-us/sections/360001495313-What-is-ORCID">ORCID ID</a> in hyphenated groups of 4 digits,
 *              <a href="https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier">valid</a>
 *              as per ISO 7064 11,2.
 */
public record Author(String name, String affiliation, String githubUser, String orcid) {

    @Override
    public String toString() {
        String base = "Author: " + name;
        if (affiliation != null)
            return base + " (" + affiliation + ")";
        else if (githubUser != null)
            return base + " (" + githubUser + ")";
        return base;
    }

    static class Deserializer implements JsonDeserializer<Author> {

        @Override
        public Author deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            if (json.isJsonObject()) {
                var obj = json.getAsJsonObject();
                return new Author(
                    deserializeField(context, obj, "name", String.class, null),
                    deserializeField(context, obj, "affiliation", String.class, null),
                    deserializeField(context, obj, "github_user", String.class, null),
                    deserializeField(context, obj, "orcid", String.class, null)
                );
            } else if (json.isJsonPrimitive()) {
                return new Author(json.getAsString(), null, null, null);
            }

            return new Author(null, null, null, null);
        }
    }

}
