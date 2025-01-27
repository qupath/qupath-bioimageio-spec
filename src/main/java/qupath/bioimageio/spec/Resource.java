package qupath.bioimageio.spec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    private String formatVersion;
    private List<Author> authors;
    private String description;
    private String documentation;
    private String name;
    private List<String> tags;

    private Dataset trainingData;

    private String version;

    private Map<String,?> attachments;
    private List<Badge> badges;
    private List<CiteEntry> cite;

    private List<String> covers;

    private String downloadURL;
    private String gitRepo;
    private String icon;
    private String id;
    private String license;

    private List<String> links;
    private List<Author> maintainers;

    private String source;

    private String rdfSource;

    /**
     * Get the version of the resource.
     * @return A semantic version hopefully, or maybe just an int, or worse, something else...
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the badges for the resource.
     * @return The badges
     */
    public List<Badge> getBadges() {
        return toUnmodifiableList(badges);
    }

    /**
     * Get the citations for the resource.
     * @return the cite entries.
     */
    public List<CiteEntry> getCite() {
        return toUnmodifiableList(cite);
    }

    /**
     * URL or relative path to a markdown file with additional documentation.
     * The recommended documentation file name is `README.md`. An `.md` suffix is mandatory.
     * The documentation should include a '[#[#]]# Validation' (sub)section
     * with details on how to quantitatively validate the model on unseen data.
     * @return A URL or path.
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * A human-readable name of this model.
     * It should be no longer than 64 characters and only contain letter, number, underscore, minus or space characters."""
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the relative path to the resource icon
     * @return the path to the icon.
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Get a link to the git repository for this resource
     * @return Hopefully a git:// or http(s):// link, but I guess it could be a file://...
     */
    public String getGitRepo() {
        return gitRepo;
    }

    /**
     * Get the resource description
     * @return A string of unknown length or content.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the version of the bioimage format used for this resource.
     * There were big changes between 0.4.x and 0.5.x that should mostly
     * be handled by this library internally, but some behaviour may be
     * inconsistent.
     * @return The format as a (hopefully SemVer) string.
     */
    public String getFormatVersion() {
        return formatVersion;
    }

    /**
     * A [SPDX license identifier](https://spdx.org/licenses/).
     * We do not support custom license beyond the SPDX license list.
     * @return the license
     */
    public String getLicense() {
        return license;
    }

    /**
     * Get the source file for the resource
     * @return a file path or http(s) link potentially
     */
    public String getSource() {
        return source;
    }

    /**
     * bioimage.io-wide unique resource identifier assigned by bioimage.io; version **un**specific.
     * @return the ID
     */
    public String getID() {
        return id;
    }

    /**
     * The authors are the creators of the model RDF and the primary points of contact.
     * @return The authors.
     */
    public List<Author> getAuthors() {
        return toUnmodifiableList(authors);
    }

    /**
     * Maintainers of this resource. If not specified `authors` are maintainers and at least some of them should specify their `github_user` name
     * @return The maintainers
     */
    public List<Author> getMaintainers() {
        return toUnmodifiableList(maintainers);
    }

    /**
     * IDs of other bioimage.io resources
     * @return links
     */
    public List<String> getLinks() {
        return toUnmodifiableList(links);
    }

    /**
     * Associated tage
     * @return the tags
     */
    public List<String> getTags() {
        return toUnmodifiableList(tags);
    }

    /**
     * Cover images. Please use an image smaller than 500KB and an aspect ratio width to height of 2:1.
     * @return links to image files
     */
    public List<String> getCovers() {
        return toUnmodifiableList(covers);
    }

    /**
     * file and other attachments
     * @return attachments
     */
    public Map<String, ?> getAttachments() {
        return attachments == null ? Collections.emptyMap() : Collections.unmodifiableMap(attachments);
    }

    boolean isFormatNewerThan(ModuleDescriptor.Version version) {
        return ModuleDescriptor.Version.parse(this.getFormatVersion()).compareTo(version) > 0;
    }

    /**
     * Compare a resource's format version to a semantic version
     * @param version The semantic version we're comparing to
     * @return Whether the resource's version is newer than this.
     */
    public boolean isFormatNewerThan(String version) {
        return isFormatNewerThan(ModuleDescriptor.Version.parse(version));
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
