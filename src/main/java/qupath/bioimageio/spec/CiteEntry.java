package qupath.bioimageio.spec;

/**
 * Citation entry.
 * @param text Free text description.
 * @param doi A digital object identifier (DOI) is the prefered citation reference.
 *            See <a href="https://www.doi.org/">doi.org</a> for details. (alternatively specify `url`)
 * @param url URL to cite (preferably specify a `doi` instead)
 */
public record CiteEntry(String text, String doi, String url) {}

