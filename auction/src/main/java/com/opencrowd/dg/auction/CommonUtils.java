package com.opencrowd.dg.auction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.tomcat.util.buf.HexUtils;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.hedera.hashgraph.proto.Transaction;
import com.hedera.hashgraph.proto.TransactionBody;

/**
 * Common utilities.
 *
 */
public class CommonUtils {

  /**
   * Read UTF-8 content from a file given a path on disk.
   *
   * @param filePath the path of the file
   */
  public static String readFileContentUTF8(String filePath) {
    String fileString = null;
    try {
      fileString = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return fileString;
  }


  /**
   * Sleep given seconds.
   */
  public static void napMillis(long timeInMillis) {
    try {
      Thread.sleep(timeInMillis);
    } catch (Exception e) {
    }
  }

  /**
   * Sleep given seconds.
   */
  public static void nap(int timeInSec) {
    try {
      Thread.sleep(timeInSec * 1000);
    } catch (Exception e) {
    }
  }

  public static void nap(double timeInSec) {
    try {
      Thread.sleep((long) timeInSec * 1000);
    } catch (Exception e) {
    }
  }


  /**
   * Write bytes to a file.
   *
   * @param path the file path to write bytes
   * @param data the byte array data
   */
  public static void writeToFile(String path, byte[] data) throws IOException {
    writeToFile(path, data, false);
  }

  /**
   * Write bytes to a file.
   *
   * @param append append to existing file if true
   */
  public static void writeToFile(String path, byte[] data, boolean append) throws IOException {
    File f = new File(path);
    File parent = f.getParentFile();
    if (!parent.exists()) {
      parent.mkdirs();
    }

    FileOutputStream fos = new FileOutputStream(f, append);
    fos.write(data);
    fos.flush();
    fos.close();
  }

  /**
   * Write string to a file using UTF_8 encoding.
   *
   * @param path the file path to write bytes
   * @param data the byte array data
   */
  public static void writeToFileUTF8(String path, String data) throws IOException {
    byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
    writeToFile(path, bytes);
  }

  /**
   * Write string to a file using UTF_8 encoding.
   *
   * @param append append to existing file if true
   */
  public static void writeToFileUTF8(String path, String data, boolean append) throws IOException {
    byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
    writeToFile(path, bytes, append);
  }

  /**
   * Encode bytes as base64.
   *
   * @param bytes to be encoded
   * @return base64 string
   */
  public static String base64encode(byte[] bytes) {
    String rv = null;
    rv = Base64.getEncoder().encodeToString(bytes);
    return rv;
  }

  /**
   * Decode base64 string to bytes.
   *
   * @param base64string to be decoded
   * @return decoded bytes
   */
  public static byte[] base64decode(String base64string) {
    byte[] rv = null;
    rv = Base64.getDecoder().decode(base64string);
    return rv;
  }

  /**
   * Convert long value to bytes.
   *
   * @param x long to be converted
   * @return byte array
   */
  public static byte[] longToBytes(long x) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(x);
    return buffer.array();
  }

  /**
   * Convert int value to bytes.
   *
   * @param x int to be converted
   * @return byte array
   */
  public static byte[] intToBytes(int x) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.putInt(x);
    return buffer.array();
  }

  /**
   * Convert bytes to int value.
   *
   * @param bytes input bytes
   * @return the int value
   */
  public static int bytesToInt(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.put(bytes);
    buffer.flip();// need flip
    return buffer.getInt();
  }

  /**
   * Convert bytes to long value.
   *
   * @param bytes input bytes
   * @return the long value
   */
  public static long bytesToLong(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(bytes);
    buffer.flip();// need flip
    return buffer.getLong();
  }

  /**
   * Serialize a Java object to bytes.
   *
   * @param object the Java object to be serialized
   * @return converted bytes
   */
  public static byte[] convertToBytes(Object object) throws IOException {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(
        bos)) {
      out.writeObject(object);
      return bos.toByteArray();
    }
  }

  /**
   * Deserialize a Java object to bytes.
   *
   * @param bytes to be deserialized
   * @return the Java object constructed
   */
  public static Object convertFromBytes(byte[] bytes) {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(
        bytes); ObjectInput in = new ObjectInputStream(bis)) {
      return in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Copy bytes.
   *
   * @param start from position
   * @param length number of bytes to copy
   * @param bytes source byte array
   */
  public static byte[] copyBytes(int start, int length, byte[] bytes) {
    byte[] rv = new byte[length];
    for (int i = 0; i < length; i++) {
      rv[i] = bytes[start + i];
    }
    return rv;
  }

  /**
   * Concatenate two byte arrays into one.
   *
   * @return the combined byte array.
   */
  public static byte[] mergeByteArray(byte[] a, byte[] b) {
    int al = a.length;
    int bl = b.length;
    byte[] rv = new byte[al + bl];

    for (int i = 0; i < al; i++) {
      rv[i] = a[i];
    }
    for (int i = al; i < (al + bl); i++) {
      rv[i] = b[i];
    }

    return rv;
  }

  /**
   * Returns a short form of a string in the form of display first and last n characters.
   */
  public static String shortForm(String string, int n) {
    int d = 2 * n;
    int len = string.length();
    if (string.length() <= d) {
      return string;
    }
    String rv = "length=" + len + ": content=" + string.substring(0, n) + " ... " + string
        .substring(len - d, len);
    return rv;
  }

  /**
   * Serialize a Java object into Json string.
   *
   * @param obj Java object
   * @return Json string
   */
  public static String serialize2Json(Object obj) {
    Gson gsonObj = new Gson();
    String json = gsonObj.toJson(obj);
    return json;
  }

  /**
   * Deserialize a Json string into a Java object.
   *
   * @param json Java object
   * @return Json string
   */
  @SuppressWarnings({"unchecked"})
  public static List<String> deserializeListOfStringFromJson(String json) {
    Gson gsonObj = new Gson();
    List<String> rv = gsonObj.fromJson(json, ArrayList.class);
    return rv;
  }

  public static byte[] hexToBytes(String data) {
    byte[] rv = HexUtils.fromHexString(data);
    return rv;
  }

  /**
   * Reads the bytes of a small binary file as a resource file.
   */
  public static byte[] readBinaryFileAsResource(String filePath)
      throws IOException, URISyntaxException {
    if (ClassLoader.getSystemResource("") == null) {
      return Files.readAllBytes(Paths.get("", filePath));

    } else {
      URI uri = ClassLoader.getSystemResource("").toURI();
      String rootPath = Paths.get(uri).toString();
      Path path = Paths.get(rootPath, filePath);

      return Files.readAllBytes(path);
    }

  }

  /**
   * Reads a resource file.
   *
   * @param filePath the resource file to be read
   * @param myClass the calling class
   */
  public static <T> byte[] readBinaryFileAsResource(String filePath, Class<T> myClass)
      throws IOException, URISyntaxException {
    Path path = Paths.get(myClass.getClassLoader().getResource(filePath).toURI());
    return Files.readAllBytes(path);
  }

  public static String[] splitLine(String line) {
    String[] elms = line.split(",");

    for (int i = 0; i < elms.length; ++i) {
      elms[i] = elms[i].trim();
    }

    return elms;
  }

  /**
   * Generates a human readable string for grpc transaction.
   *
   * @return generated readable string
   */
  public static String toReadableString(Transaction grpcTransaction) {
    String rv = null;
    try {
      TransactionBody body;
      if (grpcTransaction.hasBody()) {
        body = grpcTransaction.getBody();
      } else {
        body = TransactionBody.parseFrom(grpcTransaction.getBodyBytes());
      }
      rv = "body=" + TextFormat.shortDebugString(body) + "; sigs=" + TextFormat.shortDebugString(
          grpcTransaction.hasSigs() ? grpcTransaction.getSigs() : grpcTransaction.getSigMap());
    } catch (InvalidProtocolBufferException e) {
      // no op
    }
    return rv;
  }

  /**
   * Generates a short human readable string for grpc transaction.
   *
   * @return generated readable string
   */
  public static String toReadableStringShort(
      Transaction grpcTransaction) {
    String rv = null;
    try {
      TransactionBody body;
      if (grpcTransaction.hasBody()) {
        body = grpcTransaction.getBody();
      } else {
        body = TransactionBody.parseFrom(grpcTransaction.getBodyBytes());
      }
      rv = "txID=" + TextFormat.shortDebugString(body.getTransactionID()) + "; memo=" + body
          .getMemo();
    } catch (InvalidProtocolBufferException e) {
      // no op
    }
    return rv;
  }

  public static TransactionBody extractTransactionBody(Transaction transaction)
      throws InvalidProtocolBufferException {
    TransactionBody bodyToReturn;
    if (transaction.hasBody()) {
      bodyToReturn = transaction.getBody();
    } else {
      bodyToReturn = TransactionBody.parseFrom(transaction.getBodyBytes());
    }
    return bodyToReturn;
  }


  /**
   * Escape bytes for printing purpose.
   *
   * @param input bytes to escape
   * @return escaped string
   */
  public static String escapeBytes(final byte[] input) {
    return escapeBytes(
        new ByteSequence() {
          @Override
          public int size() {
            return input.length;
          }

          @Override
          public byte byteAt(int offset) {
            return input[offset];
          }
        });
  }

  private interface ByteSequence {

    int size();

    byte byteAt(int offset);
  }

  /**
   * Escapes bytes in the format used in protocol buffer text format, which is the same as the
   * format used for C string literals. All bytes that are not printable 7-bit ASCII characters are
   * escaped, as well as backslash, single-quote, and double-quote characters. Characters for which
   * no defined short-hand escape sequence is defined will be escaped using 3-digit octal
   * sequences.
   */
  public static String escapeBytes(final ByteSequence input) {
    final StringBuilder builder = new StringBuilder(input.size());
    for (int i = 0; i < input.size(); i++) {
      final byte b = input.byteAt(i);
      switch (b) {
        // Java does not recognize \a or \v, apparently.
        case 0x07:
          builder.append("\\a");
          break;
        case '\b':
          builder.append("\\b");
          break;
        case '\f':
          builder.append("\\f");
          break;
        case '\n':
          builder.append("\\n");
          break;
        case '\r':
          builder.append("\\r");
          break;
        case '\t':
          builder.append("\\t");
          break;
        case 0x0b:
          builder.append("\\v");
          break;
        case '\\':
          builder.append("\\\\");
          break;
        case '\'':
          builder.append("\\\'");
          break;
        case '"':
          builder.append("\\\"");
          break;
        default:
          // Only ASCII characters between 0x20 (space) and 0x7e (tilde) are
          // printable.  Other byte values must be escaped.
          if (b >= 0x20 && b <= 0x7e) {
            builder.append((char) b);
          } else {
            builder.append('\\');
            builder.append((char) ('0' + ((b >>> 6) & 3)));
            builder.append((char) ('0' + ((b >>> 3) & 7)));
            builder.append((char) ('0' + (b & 7)));
          }
          break;
      }
    }
    return builder.toString();
  }}
