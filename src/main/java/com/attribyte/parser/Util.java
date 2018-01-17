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
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.net.InternetDomainName;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

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
   public static final Map<String, Element> map(final Element parent, final Set<String> tagNames) {
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
   public static final String childText(final Element parent, final String childName) {
      Elements children = parent.getElementsByTag(childName);
      if(children == null || children.size() == 0) {
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
   public static final String childText(final JsonNode parent, final String propertyName) {
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
   public static final Element firstChild(final Element parent, final String childName) {
      Elements children = parent.getElementsByTag(childName);
      return children.size() > 0 ? children.first() : null;
   }

   /**
    * Finds the first element matching the pattern.
    * @param parent The parent element.
    * @param pattern The pattern.
    * @return The element or {@code null} if not found.
    */
   public static final Element firstMatch(final Element parent, final String pattern) {
      Elements match = parent.select(pattern);
      return match.size() > 0 ? match.get(0) : null;
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
      if(Strings.isNullOrEmpty(str)) {
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
      if(link == null) {
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
    * @return The host or {@code null} if invalid.
    */
   public static String host(final String link) {
      try {
         return new URL(link).getHost();
      } catch(MalformedURLException mue) {
         return null;
      }
   }

   /**
    * Determine if an entry contains the image.
    * @param entry The entry.
    * @param src The image source.
    * @return Does the entry contain the image?
    */
   public static final boolean containsImage(final Entry.Builder entry, final String src) {
      if(entry.getImages().size() == 0) {
         return false;
      }
      for(Image image : entry.getImages()) {
         if(image.link.equalsIgnoreCase(src)) {
            return true;
         }
      }
      return false;
   }
}