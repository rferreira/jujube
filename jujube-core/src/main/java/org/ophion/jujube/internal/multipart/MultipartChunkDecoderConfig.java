package org.ophion.jujube.internal.multipart;

import org.ophion.jujube.util.DataSize;

public class MultipartChunkDecoderConfig {
  private DataSize bufferSize;
  private DataSize headerSizeLimit;
  private DataSize bodySizeLimit;

  public MultipartChunkDecoderConfig() {
    bodySizeLimit = DataSize.mebibytes(100);
    headerSizeLimit = DataSize.kibibytes(100);
    bufferSize = DataSize.kibibytes(8);
  }

  public DataSize getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(DataSize bufferSize) {
    this.bufferSize = bufferSize;
  }

  public DataSize getHeaderSizeLimit() {
    return headerSizeLimit;
  }

  public void setHeaderSizeLimit(DataSize headerSizeLimit) {
    this.headerSizeLimit = headerSizeLimit;
  }

  public DataSize getBodySizeLimit() {
    return bodySizeLimit;
  }

  public void setBodySizeLimit(DataSize bodySizeLimit) {
    this.bodySizeLimit = bodySizeLimit;
  }
}
