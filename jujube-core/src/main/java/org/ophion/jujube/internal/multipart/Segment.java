package org.ophion.jujube.internal.multipart;

/**
 * Segments/states of a multipart message, conforming to:
 *
 * <code>
 * multipart-body := preamble 1*encapsulation close-delimiter epilogue<br>
 * encapsulation := delimiter body CRLF<br>
 * delimiter := "--" boundary CRLF<br>
 * close-delimiter := "--" boundary "--"<br>
 * preamble := &lt;ignore&gt;<br>
 * epilogue := &lt;ignore&gt;<br>
 * body := header-part CRLF body-part<br>
 * header-part := 1*header CRLF<br>
 * header := header-name ":" header-value<br>
 * header-name := &lt;printable ascii characters except ":"&gt;<br>
 * header-value := &lt;any ascii characters except CR &amp; LF&gt;<br>
 * body-data := &lt;arbitrary data&gt;<br>
 * </code>
 * <p>
 */
enum Segment {
  PREAMBLE, HEADER, BODY, EPILOGUE;
}
