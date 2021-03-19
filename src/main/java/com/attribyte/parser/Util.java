/*
 * Copyright 2018 Attribyte, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.attribyte.parser;

import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Image;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.net.InternetDomainName;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public class Util {

   /**
    * An element with empty content.
    */
   public static final Element EMPTY_ELEMENT = new Element(Tag.valueOf("EMPTY"), "");

   /**
    * Gets a map of the first child vs tag name for all names in the specified set.
    * @param parent The parent element.
    * @param tagNames The set of tag names.
    * @return The element map.
    */
   public static Map<String, Element> map(final Element parent, final Set<String> tagNames) {
      Map<String, Element> kvMap = Maps.newHashMapWithExpectedSize(tagNames.size());
      for(Element element : parent.children()) {
         String tagName = element.tagName();
         if(tagNames.contains(tagName) && !kvMap.containsKey(tagName)) {
            kvMap.put(tagName, element);
         }
      }
      return kvMap;
   }

   /**
    * Gets the text of a child element.
    * @param parent The parent element.
    * @param childName The tag name of the child.
    * @return The text or an empty string if none.
    */
   public static String childText(final Element parent, final String childName) {
      Elements children = parent.getElementsByTag(childName);
      if(children == null || children.isEmpty()) {
         return "";
      } else {
         return children.first().text();
      }
   }

   /**
    * Gets the text for a JSON object property.
    * @param parent The parent node. May be {@code null}.
    * @param propertyName The name of the property.
    * @return The text or an empty string if none.
    */
   public static String childText(final JsonNode parent, final String propertyName) {
      if(parent == null) {
         return "";
      }
      if(parent.hasNonNull(propertyName)) {
         return parent.get(propertyName).asText("");
      } else {
         return "";
      }
   }

   /**
    * Gets first child element.
    * @param parent The parent element.
    * @param childName The tag name of the child.
    * @return The first matching child or {@code null} if none.
    */
   public static Element firstChild(final Element parent, final String childName) {
      Elements children = parent.getElementsByTag(childName);
      return (children != null && !children.isEmpty()) ? children.first() : null;
   }

   /**
    * Finds the first element matching the pattern.
    * @param parent The parent element.
    * @param pattern The pattern.
    * @return The element or {@code null} if not found.
    */
   public static Element firstMatch(final Element parent, final String pattern) {
      Elements match = parent.select(pattern);
      return (match != null && !match.isEmpty()) ? match.get(0) : null;
   }

   /**
    * Converts all children of an HTML element to a string.
    * @param element The element.
    * @return The HTML string.
    */
   public static String childrenToString(final Element element) {
      if(element == null) {
         return "";
      }

      StringBuilder buf = new StringBuilder();
      element.childNodes().forEach(node -> buf.append(node.toString()));
      return buf.toString();
   }

   /**
    * Cleans special characters from a string, including emojii.
    * @param str The string.
    * @return The cleaned string.
    */
   public static String cleanSpecialCharacters(final String str) {
      if(isNullOrEmpty(str)) {
         return str;
      }
      StringBuilder buf = new StringBuilder();
      for(char ch : str.toCharArray()) {
         switch(Character.getType(ch)) {
            case Character.LETTER_NUMBER:
            case Character.OTHER_LETTER:
            case Character.LOWERCASE_LETTER:
            case Character.UPPERCASE_LETTER:
            case Character.SPACE_SEPARATOR:
            case Character.DECIMAL_DIGIT_NUMBER:
            case Character.CONNECTOR_PUNCTUATION:
            case Character.DASH_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
               buf.append(ch);
               break;
            default:
               buf.append(" ");
               break;
         }
      }
      return buf.toString();
   }

   /**
    * Gets the protocol from a link.
    * @param link The link.
    * @return The protocol or {@code null} if not found/invalid link.
    */
   public static String protocol(final String link) {
      if(isNullOrEmpty(link)) {
         return null;
      }

      try {
         return new URL(link).getProtocol();
      } catch(MalformedURLException me) {
         return null;
      }
   }

   /**
    * Gets the host for a link.
    * @param link The link.
    * @return The host or {@code null} if link is an invalid URL.
    */
   public static String host(String link) {

      if(isNullOrEmpty(link)) {
         return null;
      }

      if(!link.contains("://")) {
         link = "http://" + link;
      }

      try {
         return new URL(link).getHost();
      } catch(MalformedURLException mue) {
         return null;
      }
   }

   /**
    * Gets the (top, private) domain for the link.
    * <p>
    *    For example: {@code test.attribyte.com -> attribyte.com, test.blogspot.com -> test.blogspot.com}.
    * </p>
    * @param link The link.
    * @return The domain or {@code null} if invalid.
    */
   public static String domain(final String link) {
      final String host = host(link);
      if(host == null) {
         return null;
      }

      try {
         InternetDomainName idn = InternetDomainName.from(host);
         if(idn.isPublicSuffix()) {
            return idn.toString();
         } else {
            return idn.topPrivateDomain().toString();
         }
      } catch(IllegalArgumentException ie) {
         return null;
      }
   }

   /**
    * Check to see if a URL is {@code http/https}.
    * @param url The URL.
    * @param defaultProtocol The protocol used if url starts with {@code //}. If {@code null}, {@code https} is used.
    * @return The URL or {@code null}.
    */
   public static String httpURL(String url, final String defaultProtocol) {
      url = nullToEmpty(url).trim();
      if(url.isEmpty()) {
         return null;
      }

      if(url.startsWith("http://") || url.startsWith("https://")) {
         return url;
      } else if(url.startsWith("//")) {
         return (isNullOrEmpty(defaultProtocol) ? "https" : defaultProtocol) + ":" + url;
      } else {
         return null;
      }
   }

   /**
    * Determine if an entry contains the image.
    * @param entry The entry.
    * @param src The image source.
    * @return Does the entry contain the image?
    */
   public static boolean containsImage(final Entry.Builder entry, final String src) {
      if(entry.getImages().isEmpty()) {
         return false;
      }
      for(Image image : entry.getImages()) {
         if(image.link.equalsIgnoreCase(src)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Determine if a string ends with another string, ignoring trailing "invisible" characters in the source.
    * @param match The string to match at the end.
    * @param source The source string.
    * @return Does the source end with the match string (ignoring invisible characters).
    */
   public static boolean endsWithIgnoreInvisible(final String match, final String source) {
      int sourcePos = source.length();
      CharMatcher matcher = CharMatcher.invisible();
      while(--sourcePos >= 0) {
         if(!matcher.matches(source.charAt(sourcePos))) {
            break;
         }
      }

      int matchPos = match.length();
      if(matchPos == 0) {
         return false;
      }

      while(--matchPos >= 0) {
         if(sourcePos < 0 || match.charAt(matchPos) != source.charAt(sourcePos)) {
            return false;
         }
         sourcePos--;
      }
      return true;
   }

   /**
    * Determine if a string starts with another string, ignoring leading "invisible" characters in the source.
    * @param match The string to match at the end.
    * @param source The source string.
    * @return Does the source end with the match string (ignoring invisible characters).
    */
   public static boolean startsWithIgnoreInvisible(final String match, final String source) {
      if(match.length() == 0) {
         return false;
      }

      int sourcePos = 0;
      CharMatcher matcher = CharMatcher.invisible();
      while(sourcePos++ < source.length()) {
         if(!matcher.matches(source.charAt(sourcePos))) {
            break;
         }
      }

      int matchPos = 0;

      while(matchPos < match.length()) {
         if(sourcePos >= source.length() || match.charAt(matchPos) != source.charAt(sourcePos)) {
            return false;
         }
         matchPos++;
         sourcePos++;
      }
      return true;
   }


   /**
    * Creates a slug from a string.
    * <p>
    *    Does not strip markup.
    * </p>
    * @param str The string.
    * @return The slug for the string.
    */
   public static String slugify(final String str) {
      StringBuilder buf = new StringBuilder();
      boolean lastWasDash = false;
      for(char ch : str.toLowerCase().trim().toCharArray()) {
         if(Character.isLetterOrDigit(ch)) {
            buf.append(ch);
            lastWasDash = false;
         } else {
            if(!lastWasDash) {
               buf.append("-");
               lastWasDash = true;
            }
         }
      }

      String slug = buf.toString();
      if(slug.length() == 0) {
         return "";
      }

      if(slug.charAt(0) == '-') {
         slug = slug.substring(1);
      }
      if(slug.length() > 0 && slug.charAt(slug.length() - 1) == '-') {
         slug = slug.substring(0, slug.length() - 1);
      }

      return slug;
   }

   /**
    * Unzip a file.
    * @param zipFile The zip file.
    * @param targetDir The target output directory.
    * @param fileConsumer A function called before each file or directory is written.
    * @throws IOException on error.
    */
   public static void unzip(@NonNull final File zipFile,
                            @NonNull final File targetDir,
                            @Nullable final Consumer<File> fileConsumer) throws IOException {
      try(ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
         ZipEntry entry;
         while((entry = zis.getNextEntry()) != null) {
            File newFile = createUnzipFile(targetDir, entry);
            if(fileConsumer != null) {
               fileConsumer.accept(newFile);
            }
            if(entry.isDirectory()) {
               if(newFile.exists() && newFile.isFile()) {
                  throw new IOException(String.format("Unable to create directory, '%s' - file exists at this location.",
                          newFile.getAbsolutePath()));
               } else if(!newFile.exists() && !newFile.mkdirs()) {
                  throw new IOException(String.format("Unable to create directory, '%s'",
                          newFile.getAbsolutePath()));
               }
            } else {
               if(newFile.exists() && !newFile.isFile()) {
                  throw new IOException(String.format("Unable to create file, '%s'",
                          newFile.getAbsolutePath()));
               }

               try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile))) {
                  ByteStreams.copy(zis, bos);
               }
            }
         }
      }
   }

   /**
    * Create a file to write unzipped data, checking to make sure nothing is written outside the target directory.
    * @param targetDir The target directory.
    * @param entry The zip entry.
    * @return The file to write.
    * @throws IOException if unable to create directory or attempt to write outside the target.
    */
   private static File createUnzipFile(final File targetDir, final ZipEntry entry) throws IOException {
      File newFile = new File(targetDir, entry.getName());
      if(newFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath() + File.separator)) {
         File checkParent = newFile.getParentFile();
         if(!checkParent.exists() && !checkParent.mkdirs()) {
            throw new IOException(String.format("Unable to create directory, '%s'",
                    checkParent.getAbsolutePath()));
         }
         return newFile;
      } else {
         throw new IOException(String.format("Attempt to create file outside target directory, '%s'",
                 newFile.getAbsolutePath()));
      }
   }
}