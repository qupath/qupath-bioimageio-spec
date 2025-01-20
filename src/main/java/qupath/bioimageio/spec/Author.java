package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

import static qupath.bioimageio.spec.Model.deserializeField;


/**
 * Author or maintainer.
 */
public class Author {

    String affiliation;
    @SerializedName("github_user")
    String githubUser;
    String name;
    String orcid;

    public String getAffiliation() {
        return affiliation;
    }

    public String getGitHubUser() {
        return githubUser;
    }

    public String getName() {
        return name;
    }

    public String getOrcid() {
        return orcid;
    }

    @Override
    public String toString() {
        String base = "Author: " + name;
        if (affiliation != null)
            return base + " (" + affiliation + ")";
        else if (githubUser != null)
            return base + " (" + githubUser + ")";
        return base;
    }

    public static class Deserializer implements JsonDeserializer<Author> {

        @Override
        public Author deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            if (json.isJsonNull())
                return null;

            var author = new Author();
            if (json.isJsonObject()) {
                var obj = json.getAsJsonObject();
                author.name = deserializeField(context, obj, "name", String.class, null);
                author.affiliation = deserializeField(context, obj, "affiliation", String.class, null);
                author.githubUser = deserializeField(context, obj, "github_user", String.class, null);
                author.orcid = deserializeField(context, obj, "orcid", String.class, null);
            } else if (json.isJsonPrimitive()) {
                author.name = json.getAsString();
            }
            return author;
        }

    }

}


