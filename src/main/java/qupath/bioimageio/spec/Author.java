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

    private String affiliation;
    @SerializedName("github_user")
    private String githubUser;
    private String name;
    private String orcid;

    /**
     * Get the author's affiliation
     * @return the affiliation
     */
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * Get the GitHub username for this author
     * @return the GitHub username
     */
    public String getGitHubUser() {
        return githubUser;
    }

    /**
     * Get the author's name
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * An <a href="https://support.orcid.org/hc/en-us/sections/360001495313-What-is-ORCID">ORCID ID</a>
     * in hyphenated groups of 4 digits, <a href="https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier">valid</a>
     * as per ISO 7064 11,2.
     * @return The ORCID ID.
     */
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

    static class Deserializer implements JsonDeserializer<Author> {

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


