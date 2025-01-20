package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static qupath.bioimageio.spec.Model.deserializeField;
import static qupath.bioimageio.spec.Model.parameterizedListType;
import static qupath.bioimageio.spec.Model.toUnmodifiableList;

/**
 * General resource, based upon the RDF.
 * <p>
 * For machine learning models, you probably want {@link Model}.
 */
public class Resource {
    private static final Logger logger = LoggerFactory.getLogger(Resource.class);

    String formatVersion;
    List<Author> authors;
    String description;
    String documentation;

    String name;

    List<String> tags;

    @SerializedName("training_data")
    Dataset trainingData;

    String version;

    Map<String,?> attachments;
    List<Badge> badges;
    List<CiteEntry> cite;

    List<String> covers;

    @SerializedName("download_url")
    String downloadURL;
    String gitRepo;
    String icon;
    String id;
    String license;

    List<String> links;
    List<Author> maintainers;

    private String source;

    @SerializedName("rdf_source")
    String rdfSource;

    public String getVersion() {
        return version;
    }

    public List<Badge> getBadges() {
        return toUnmodifiableList(badges);
    }

    public List<CiteEntry> getCite() {
        return toUnmodifiableList(cite);
    }

    public String getDocumentation() {
        return documentation;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getGitRepo() {
        return gitRepo;
    }

    public String getDescription() {
        return description;
    }

    public String getFormatVersion() {
        return formatVersion;
    }

    public String getLicense() {
        return license;
    }

    public String getSource() {
        return source;
    }

    public String getID() {
        return id;
    }

    public List<Author> getAuthors() {
        return toUnmodifiableList(authors);
    }

    public List<Author> getMaintainers() {
        return toUnmodifiableList(maintainers);
    }

    public List<String> getLinks() {
        return toUnmodifiableList(links);
    }

    public List<String> getTags() {
        return toUnmodifiableList(tags);
    }

    public List<String> getCovers() {
        return toUnmodifiableList(covers);
    }

    public Map<String, ?> getAttachments() {
        return attachments == null ? Collections.emptyMap() : Collections.unmodifiableMap(attachments);
    }

    boolean isNewerThan(ModuleDescriptor.Version version) {
        return ModuleDescriptor.Version.parse(this.getFormatVersion()).compareTo(version) > 0;
    }

    boolean isNewerThan(String version) {
        return isNewerThan(ModuleDescriptor.Version.parse(version));
    }


    static class Deserializer implements JsonDeserializer<Resource> {

        @Override
        public Resource deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json.isJsonNull())
                return null;
            var obj = json.getAsJsonObject();
            Resource resource = new Resource();
            deserializeResourceFields(resource, obj, context, true);
            return resource;
        }

    }

    static void deserializeResourceFields(Resource resource, JsonObject obj, JsonDeserializationContext context, boolean doStrict) {

        resource.formatVersion = deserializeField(context, obj, "format_version", String.class, doStrict);
        resource.authors = deserializeField(context, obj, "authors", parameterizedListType(Author.class), doStrict);
        resource.description = deserializeField(context, obj, "description", String.class, doStrict);
        resource.documentation = deserializeField(context, obj, "documentation", String.class, doStrict);

        resource.license = deserializeField(context, obj, "license", String.class, doStrict);
        resource.name = deserializeField(context, obj, "name", String.class, doStrict);

        if (obj.has("attachments")) {
            if (obj.get("attachments").isJsonObject())
                resource.attachments = deserializeField(context, obj, "attachments", Map.class, Collections.emptyMap());
            else
                logger.warn("Can't parse attachments (not a dict)");
        }
        resource.badges = deserializeField(context, obj, "badges", parameterizedListType(Badge.class), Collections.emptyList());
        resource.cite = deserializeField(context, obj, "cite", parameterizedListType(CiteEntry.class), Collections.emptyList());

        resource.covers = deserializeField(context, obj, "covers", parameterizedListType(String.class), Collections.emptyList());
        resource.downloadURL = deserializeField(context, obj, "download_url", String.class, null);
        resource.gitRepo = deserializeField(context, obj, "git_repo", String.class, null);
        resource.icon = deserializeField(context, obj, "icon", String.class, null);
        resource.id = deserializeField(context, obj, "id", String.class, null);
        resource.links = deserializeField(context, obj, "links", parameterizedListType(String.class), Collections.emptyList());
        resource.maintainers = deserializeField(context, obj, "maintainers", parameterizedListType(Author.class), Collections.emptyList());

        resource.rdfSource = deserializeField(context, obj, "rdf_source", String.class, null);

        resource.tags = deserializeField(context, obj, "tags", parameterizedListType(String.class), Collections.emptyList());

        resource.trainingData = deserializeField(context, obj, "training_data", Dataset.class, null);

        resource.version = deserializeField(context, obj, "version", String.class, null);
    }

}
