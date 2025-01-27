package qupath.bioimageio.spec;

/**
 * Model or repository badge.
 * @param label Badge label to display on hover
 * @param icon path or url to icon
 * @param url the target of the badge if clicked
 */
public record Badge(String label, String icon, String url) {

}

