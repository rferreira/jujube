package org.ophion.jujube.http;

import org.apache.hc.core5.http.ContentType;

@SuppressWarnings("unused")
/**
 * Superset helper class for Content Types {@link ContentType}.
 * Technically this class should be called "MediaTypes", but we keep it as ContentTypes for compatibility with
 * Apache HttpComponents Core.
 */
public class ContentTypes {
  public static final ContentType APPLICATION_JAVASCRIPT = ContentType.create("application/javascript");
  public static final ContentType APPLICATION_XML = ContentType.APPLICATION_XML;
  public static final ContentType TEXT_XML = ContentType.TEXT_XML;
  public static final ContentType TEXT_HML = ContentType.TEXT_HTML;
  public static final ContentType ANY_TYPE = ContentType.create("*/*");
  public static final ContentType ANY_TEXT_TYPE = ContentType.create("text/*");
  public static final ContentType ANY_IMAGE_TYPE = ContentType.create("image/*");
  public static final ContentType ANY_AUDIO_TYPE = ContentType.create("audio/*");
  public static final ContentType ANY_VIDEO_TYPE = ContentType.create("video/*");

  /**
   * Returns true if target is within range as defined by HTTP 1.1.
   * {@see https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1}
   * <p>
   * TODO: support q parameters
   *
   * @param target the content type to target
   * @param range  the content type range
   * @return true if within range
   */
  public static boolean isInRange(ContentType target, ContentType range) {
    if (target.isSameMimeType(range)) {
      return true;
    }

    String[] rangeParts = range.getMimeType().split("/");
    String rangeType = rangeParts.length > 0 ? rangeParts[0] : "";
    String rangeSubType = rangeParts.length > 1 ? rangeParts[1] : "";

    // if type is wildcard allow
    if (rangeType.equals("*")) {
      return true;
    }

    // now comparing individual parts:
    String[] targetParts = target.getMimeType().split("/");
    String targetType = targetParts.length > 0 ? targetParts[0] : "";
    String targetSubType = targetParts.length > 1 ? targetParts[1] : "";

    return rangeType.equals(targetType) && rangeSubType.equals("*");
  }

  public static ContentType parse(String content) {
    return ContentType.parse(content);
  }
}
