package org.ophion.jujube.util;

import java.io.Serializable;
import java.util.Collections;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.ophion.jujube.util.DataSizeUnit.*;

/**
 * A data size with SI or IEC prefix, such as "128KB" or "5 Gibibytes".
 * This class models a size in terms of bytes and is immutable and thread-safe.
 *
 * @see DataSizeUnit
 * @since 2.0
 */
public class DataSize implements Comparable<DataSize>, Serializable {
  private static final long serialVersionUID = 8517642678733072800L;

  private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)\\s*(\\S*)");
  private static final SortedMap<String, DataSizeUnit> SUFFIXES;

  static {
    final SortedMap<String, DataSizeUnit> suffixes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    suffixes.put("B", BYTES);
    suffixes.put("byte", BYTES);
    suffixes.put("bytes", BYTES);
    suffixes.put("K", KILOBYTES);
    suffixes.put("KB", KILOBYTES);
    suffixes.put("KiB", KIBIBYTES);
    suffixes.put("kilobyte", KILOBYTES);
    suffixes.put("kibibyte", KIBIBYTES);
    suffixes.put("kilobytes", KILOBYTES);
    suffixes.put("kibibytes", KIBIBYTES);
    suffixes.put("M", MEGABYTES);
    suffixes.put("MB", MEGABYTES);
    suffixes.put("MiB", MEBIBYTES);
    suffixes.put("megabyte", MEGABYTES);
    suffixes.put("mebibyte", MEBIBYTES);
    suffixes.put("megabytes", MEGABYTES);
    suffixes.put("mebibytes", MEBIBYTES);
    suffixes.put("G", GIGABYTES);
    suffixes.put("GB", GIGABYTES);
    suffixes.put("GiB", GIBIBYTES);
    suffixes.put("gigabyte", GIGABYTES);
    suffixes.put("gibibyte", GIBIBYTES);
    suffixes.put("gigabytes", GIGABYTES);
    suffixes.put("gibibytes", GIBIBYTES);
    suffixes.put("T", TERABYTES);
    suffixes.put("TB", TERABYTES);
    suffixes.put("TiB", TEBIBYTES);
    suffixes.put("terabyte", TERABYTES);
    suffixes.put("tebibyte", TEBIBYTES);
    suffixes.put("terabytes", TERABYTES);
    suffixes.put("tebibytes", TEBIBYTES);
    suffixes.put("P", PETABYTES);
    suffixes.put("PB", PETABYTES);
    suffixes.put("PiB", PEBIBYTES);
    suffixes.put("petabyte", PETABYTES);
    suffixes.put("pebibyte", PEBIBYTES);
    suffixes.put("petabytes", PETABYTES);
    suffixes.put("pebibytes", PEBIBYTES);
    SUFFIXES = Collections.unmodifiableSortedMap(suffixes);
  }

  private final long count;
  private final DataSizeUnit unit;

  private DataSize(long count, DataSizeUnit unit) {
    this.count = count;
    this.unit = requireNonNull(unit);
  }

  public static DataSize bytes(long count) {
    return new DataSize(count, BYTES);
  }

  public static DataSize kilobytes(long count) {
    return new DataSize(count, KILOBYTES);
  }

  public static DataSize megabytes(long count) {
    return new DataSize(count, MEGABYTES);
  }

  public static DataSize gigabytes(long count) {
    return new DataSize(count, GIGABYTES);
  }

  public static DataSize terabytes(long count) {
    return new DataSize(count, TERABYTES);
  }

  public static DataSize petabytes(long count) {
    return new DataSize(count, PETABYTES);
  }

  public static DataSize kibibytes(long count) {
    return new DataSize(count, KIBIBYTES);
  }

  public static DataSize mebibytes(long count) {
    return new DataSize(count, MEBIBYTES);
  }

  public static DataSize gibibytes(long count) {
    return new DataSize(count, GIBIBYTES);
  }

  public static DataSize tebibytes(long count) {
    return new DataSize(count, TEBIBYTES);
  }

  public static DataSize pebibytes(long count) {
    return new DataSize(count, PEBIBYTES);
  }

  public static DataSize parse(CharSequence size) {
    return parse(size, BYTES);
  }

  public static DataSize parse(CharSequence size, DataSizeUnit defaultUnit) {
    final Matcher matcher = SIZE_PATTERN.matcher(size);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid size: " + size);
    }

    final long count = Long.parseLong(matcher.group(1));
    final String unit = matcher.group(2);
    final DataSizeUnit dataSizeUnit = (unit == null || unit.equals(" ")) ? defaultUnit : SUFFIXES.get(unit);
    if (dataSizeUnit == null) {
      throw new IllegalArgumentException("Invalid size: " + size + ". Wrong size unit");
    }

    return new DataSize(count, dataSizeUnit);
  }

  public long getQuantity() {
    return count;
  }

  public DataSizeUnit getUnit() {
    return unit;
  }

  public long toBytes() {
    return BYTES.convert(count, unit);
  }

  public long toKilobytes() {
    return KILOBYTES.convert(count, unit);
  }

  public long toMegabytes() {
    return MEGABYTES.convert(count, unit);
  }

  public long toGigabytes() {
    return GIGABYTES.convert(count, unit);
  }

  public long toTerabytes() {
    return TERABYTES.convert(count, unit);
  }

  public long toPetabytes() {
    return PETABYTES.convert(count, unit);
  }

  public long toKibibytes() {
    return KIBIBYTES.convert(count, unit);
  }

  public long toMebibytes() {
    return MEBIBYTES.convert(count, unit);
  }

  public long toGibibytes() {
    return GIBIBYTES.convert(count, unit);
  }

  public long toTebibytes() {
    return TEBIBYTES.convert(count, unit);
  }

  public long toPebibytes() {
    return PEBIBYTES.convert(count, unit);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    final DataSize size = (DataSize) obj;
    return (count == size.count) && (unit == size.unit);
  }

  @Override
  public int hashCode() {
    return (31 * (int) (count ^ (count >>> 32))) + unit.hashCode();
  }

  @Override
  public String toString() {
    String units = unit.toString().toLowerCase(Locale.ENGLISH);
    if (count == 1L) {
      units = units.substring(0, units.length() - 1);
    }
    return Long.toString(count) + ' ' + units;
  }

  @Override
  public int compareTo(DataSize other) {
    if (unit == other.unit) {
      return Long.compare(count, other.count);
    }

    return Long.compare(toBytes(), other.toBytes());
  }
}
