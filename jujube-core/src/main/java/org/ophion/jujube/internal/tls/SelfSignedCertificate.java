package org.ophion.jujube.internal.tls;
/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import org.ophion.jujube.internal.util.Loggers;
import org.slf4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * Generates a temporary self-signed certificate for testing purposes.
 * <p>
 * <strong>NOTE:</strong>
 * Never use the certificate and private key generated by this class in production.
 * It is purely for testing purposes, and thus it is very insecure.
 * It even uses an insecure pseudo-random generator for faster generation internally.
 * </p><p>
 * An X.509 certificate file and a RSA private key file are generated in a system's temporary directory using
 * {@link File#createTempFile(String, String)}, and they are deleted when the JVM exits using
 * {@link File#deleteOnExit()}.
 * </p><p>
 * At first, this method tries to use OpenJDK's X.509 implementation (the {@code sun.security.x509} package).
 * If it fails, it tries to use <a href="http://www.bouncycastle.org/">Bouncy Castle</a> as a fallback.
 * </p>
 */
public final class SelfSignedCertificate {

  private static final Logger logger = Loggers.build();
  /**
   * Current time minus 1 year, just in case software clock goes back due to time synchronization
   */
  private static final Date DEFAULT_NOT_BEFORE = Date.from(Instant.now().plus(360, ChronoUnit.DAYS));
  /**
   * The maximum possible value in X.509 specification: 9999-12-31 23:59:59
   */
  private static final Date DEFAULT_NOT_AFTER = new Date(253402300799000L);
  /**
   * FIPS 140-2 encryption requires the key length to be 2048 bits or greater.
   * Let's use that as a sane default but allow the default to be set dynamically
   * for those that need more stringent security requirements.
   */
  private static final int DEFAULT_KEY_LENGTH_BITS = 2048;
  private static SecureRandom secureRandom = new SecureRandom();

  private final File certificate;
  private final File privateKey;
  private final X509Certificate cert;
  private final PrivateKey key;

  /**
   * Creates a new instance.
   */
  public SelfSignedCertificate() throws CertificateException {
    this(DEFAULT_NOT_BEFORE, DEFAULT_NOT_AFTER);
  }

  /**
   * Creates a new instance.
   *
   * @param notBefore Certificate is not valid before this time
   * @param notAfter  Certificate is not valid after this time
   */
  public SelfSignedCertificate(Date notBefore, Date notAfter) throws CertificateException {
    this("example.com", notBefore, notAfter);
  }

  /**
   * Creates a new instance.
   *
   * @param fqdn a fully qualified domain name
   */
  public SelfSignedCertificate(String fqdn) throws CertificateException {
    this(fqdn, DEFAULT_NOT_BEFORE, DEFAULT_NOT_AFTER);
  }

  /**
   * Creates a new instance.
   *
   * @param fqdn      a fully qualified domain name
   * @param notBefore Certificate is not valid before this time
   * @param notAfter  Certificate is not valid after this time
   */
  public SelfSignedCertificate(String fqdn, Date notBefore, Date notAfter) throws CertificateException {
    // Bypass entropy collection by using insecure random generator.
    // We just want to generate it without any delay because it's for testing purposes only.
    this(fqdn, secureRandom, DEFAULT_KEY_LENGTH_BITS, notBefore, notAfter);
  }

  /**
   * Creates a new instance.
   *
   * @param fqdn   a fully qualified domain name
   * @param random the {@link SecureRandom} to use
   * @param bits   the number of bits of the generated private key
   */
  public SelfSignedCertificate(String fqdn, SecureRandom random, int bits) throws CertificateException {
    this(fqdn, random, bits, DEFAULT_NOT_BEFORE, DEFAULT_NOT_AFTER);
  }

  /**
   * Creates a new instance.
   *
   * @param fqdn      a fully qualified domain name
   * @param random    the {@link SecureRandom} to use
   * @param bits      the number of bits of the generated private key
   * @param notBefore Certificate is not valid before this time
   * @param notAfter  Certificate is not valid after this time
   */
  public SelfSignedCertificate(String fqdn, SecureRandom random, int bits, Date notBefore, Date notAfter)
    throws CertificateException {
    // Generate an RSA key pair.
    final KeyPair keypair;
    try {
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(bits, random);
      keypair = keyGen.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      // Should not reach here because every Java implementation must have RSA key pair generator.
      throw new Error(e);
    }

    String[] paths;
    try {
      paths = BouncyCastleSelfSignedCertGenerator.generate(fqdn, keypair, random, notBefore, notAfter);
    } catch (Throwable t) {
      logger.debug("Failed to generate a self-signed X.509 certificate using sun.security.x509:", t);
      throw new CertificateException(
        "No provider succeeded to generate a self-signed certificate. " +
          "See debug log for the root cause.", t);
    }

    certificate = new File(paths[0]);
    privateKey = new File(paths[1]);
    key = keypair.getPrivate();
    FileInputStream certificateInput = null;
    try {
      certificateInput = new FileInputStream(certificate);
      cert = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(certificateInput);
    } catch (Exception e) {
      throw new CertificateEncodingException(e);
    } finally {
      if (certificateInput != null) {
        try {
          certificateInput.close();
        } catch (IOException e) {
          if (logger.isWarnEnabled()) {
            logger.warn("Failed to close a file: " + certificate, e);
          }
        }
      }
    }
  }

  static String[] newSelfSignedCertificate(
    String fqdn, PrivateKey key, X509Certificate cert) throws IOException, CertificateEncodingException {
    // Encode the private key into a file.
    ByteBuffer wrappedBuf = ByteBuffer.wrap(key.getEncoded());
    ByteBuffer encodedBuf;
    final String keyText;
    encodedBuf = Base64.getEncoder().encode(wrappedBuf);
    keyText = "-----BEGIN PRIVATE KEY-----\n" +
      new String(encodedBuf.array(), StandardCharsets.US_ASCII) +
      "\n-----END PRIVATE KEY-----\n";

    File keyFile = File.createTempFile("keyutil_" + fqdn + '_', ".key");
    keyFile.deleteOnExit();

    OutputStream keyOut = new FileOutputStream(keyFile);
    try {
      keyOut.write(keyText.getBytes(StandardCharsets.US_ASCII));
      keyOut.close();
      keyOut = null;
    } finally {
      if (keyOut != null) {
        safeClose(keyFile, keyOut);
        safeDelete(keyFile);
      }
    }

    wrappedBuf = ByteBuffer.wrap(cert.getEncoded());
    final String certText;
    encodedBuf = Base64.getEncoder().encode(wrappedBuf);
    // Encode the certificate into a CRT file.
    certText = "-----BEGIN CERTIFICATE-----\n" +
      new String(encodedBuf.array(), StandardCharsets.US_ASCII) +
      "\n-----END CERTIFICATE-----\n";

    File certFile = File.createTempFile("keyutil_" + fqdn + '_', ".crt");
    certFile.deleteOnExit();

    OutputStream certOut = new FileOutputStream(certFile);
    try {
      certOut.write(certText.getBytes(StandardCharsets.US_ASCII));
      certOut.close();
      certOut = null;
    } finally {
      if (certOut != null) {
        safeClose(certFile, certOut);
        safeDelete(certFile);
        safeDelete(keyFile);
      }
    }

    return new String[]{certFile.getPath(), keyFile.getPath()};
  }

  private static void safeDelete(File certFile) {
    if (!certFile.delete()) {
      if (logger.isWarnEnabled()) {
        logger.warn("Failed to delete a file: " + certFile);
      }
    }
  }

  private static void safeClose(File keyFile, OutputStream keyOut) {
    try {
      keyOut.close();
    } catch (IOException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Failed to close a file: " + keyFile, e);
      }
    }
  }

  /**
   * Returns the generated X.509 certificate file in PEM format.
   */
  public File certificate() {
    return certificate;
  }

  /**
   * Returns the generated RSA private key file in PEM format.
   */
  public File privateKey() {
    return privateKey;
  }

  /**
   * Returns the generated X.509 certificate.
   */
  public X509Certificate cert() {
    return cert;
  }

  /**
   * Returns the generated RSA private key.
   */
  public PrivateKey key() {
    return key;
  }

  /**
   * Deletes the generated X.509 certificate file and RSA private key file.
   */
  public void delete() {
    safeDelete(certificate);
    safeDelete(privateKey);
  }
}