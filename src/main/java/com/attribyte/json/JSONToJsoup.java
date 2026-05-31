/*
 * Copyright 2026 Attribyte Labs, LLC
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

package com.attribyte.json;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import java.io.IOException;
import java.io.Reader;

/**
 * Parses JSON to a tree of <a href="https://jsoup.org/">jsoup</a> nodes.
 * <p>
 * Among other uses, the parsed tree may be queried using jsoup's
 * <a href="https://jsoup.org/apidocs/org/jsoup/select/Selector.html">Selector</a>
 * system.
 * </p>
 * @author Matt Hamer, Attribyte, LLC
 */
public class JSONToJsoup {

   /**
    * The behavior when null values are encountered.
    */
   public  enum NullBehavior {

      /**
       * Ignore {@code null} values.
       */
      IGNORE,

      /**
       * Report {@code null} values by adding the attribute `isNull' with value 'true'.
       */
      REPORT,

      /**
       * Report {@code null} values with an empty element.
       */
      EMPTY
   }

   /**
    * The behavior when a JSON object field has an empty-string key
    * ({@code {"": value}}). JSON permits empty keys; jsoup's
    * {@code Element.appendElement(tagName)} does not.
    *
    * <p>Real-world example — CFR's WordPress events endpoint groups
    * sessions by {@code group_name}, and ungrouped sessions land under
    * the empty-string key:
    * <pre>
    *   "sessions": { "": [ { ...session data... } ] }
    * </pre>
    * Without handling, the whole parse aborts on the first such field.</p>
    */
   public enum EmptyKeyBehavior {

      /**
       * Skip the field entirely — both the empty key and its value. Data
       * under the empty key is lost but the parse continues.
       */
      SKIP,

      /**
       * Substitute {@link #EMPTY_KEY_SUBSTITUTE} as the element name. Preserves
       * the value; lets selectors reach it via the placeholder name.
       * This is the default — most callers care more about not crashing on
       * one weird field than about strictly preserving original key names.
       */
      SUBSTITUTE,

      /**
       * Re-throw jsoup's {@code IllegalArgumentException("String must not
       * be empty")}. Legacy behavior; only useful for callers that want
       * to be strict about JSON shape.
       */
      THROW
   }

   /**
    * Element name used when {@link EmptyKeyBehavior#SUBSTITUTE} is in effect.
    * Underscore-flanked to make it visually distinct from real field names
    * and to keep it valid as a jsoup tag.
    */
   public static final String EMPTY_KEY_SUBSTITUTE = "_empty_";

   /**
    * The default base URI (empty string).
    */
   private static final String BASE_URI = "";

   /**
    * Parse a JSON document to jsoup nodes.
    * Empty-key handling defaults to {@link EmptyKeyBehavior#SUBSTITUTE}.
    * @param charStream The JSON character stream.
    * @param rootElementName The root element name.
    * @param nullBehavior The behavior when handling {@code null} values.
    * @return The root element.
    * @throws IOException on input exception.
    */
   public static Element parse(final Reader charStream,
                               final String rootElementName,
                               final NullBehavior nullBehavior) throws IOException {
      return parse(charStream, rootElementName, nullBehavior, EmptyKeyBehavior.SUBSTITUTE);
   }

   /**
    * Parse a JSON document to jsoup nodes.
    * @param charStream The JSON character stream.
    * @param rootElementName The root element name.
    * @param nullBehavior The behavior when handling {@code null} values.
    * @param emptyKeyBehavior The behavior when an object field has an empty-string key.
    * @return The root element.
    * @throws IOException on input exception.
    */
   public static Element parse(final Reader charStream,
                               final String rootElementName,
                               final NullBehavior nullBehavior,
                               final EmptyKeyBehavior emptyKeyBehavior) throws IOException {
      Element rootElem = new Element(Tag.valueOf(rootElementName), BASE_URI);
      return parse(charStream, rootElem, nullBehavior, emptyKeyBehavior);
   }

   /**
    * Parse a JSON document to jsoup nodes.
    * Empty-key handling defaults to {@link EmptyKeyBehavior#SUBSTITUTE}.
    * @param charStream The JSON character stream.
    * @param rootElem The root element.
    * @param nullBehavior The behavior when handling {@code null} values.
    * @return The root element.
    * @throws IOException on input exception.
    */
   public static Element parse(final Reader charStream,
                               final Element rootElem,
                               final NullBehavior nullBehavior) throws IOException {
      return parse(charStream, rootElem, nullBehavior, EmptyKeyBehavior.SUBSTITUTE);
   }

   /**
    * Parse a JSON document to jsoup nodes.
    * @param charStream The JSON character stream.
    * @param rootElem The root element.
    * @param nullBehavior The behavior when handling {@code null} values.
    * @param emptyKeyBehavior The behavior when an object field has an empty-string key.
    * @return The root element.
    * @throws IOException on input exception.
    */
   public static Element parse(final Reader charStream,
                               final Element rootElem,
                               final NullBehavior nullBehavior,
                               final EmptyKeyBehavior emptyKeyBehavior) throws IOException {

      JsonReader reader = new JsonReader(charStream);
      reader.setStrictness(Strictness.LENIENT);
      JsonToken type = reader.peek();
      switch(type) {
         case BEGIN_ARRAY:
            parseArray(reader, rootElem, nullBehavior, emptyKeyBehavior, rootElem.tagName());
            break;
         case BEGIN_OBJECT:
            parseObject(reader, rootElem, nullBehavior, emptyKeyBehavior);
            break;
         default:
            parseValue(reader, type, rootElem.appendElement(reader.nextName()), nullBehavior);
      }

      return rootElem;
   }

   private static void parseArray(final JsonReader reader,
                                  final Element parent,
                                  final NullBehavior nullBehavior,
                                  final EmptyKeyBehavior emptyKeyBehavior,
                                  final String elemName) throws IOException {
      reader.beginArray();
      while(reader.hasNext()) {
         JsonToken type = reader.peek();
         switch(type) {
            case BEGIN_OBJECT:
               parseObject(reader, parent.appendElement(elemName), nullBehavior, emptyKeyBehavior);
               break;
            case BEGIN_ARRAY:
               parseArray(reader, parent.appendElement(elemName), nullBehavior, emptyKeyBehavior, elemName);
               break;
            case NULL:
            default:
               parseValue(reader, type, parent.appendElement(elemName), nullBehavior);
         }
      }
      reader.endArray();
   }

   private static void parseObject(final JsonReader reader,
                                   final Element parent,
                                   final NullBehavior nullBehavior,
                                   final EmptyKeyBehavior emptyKeyBehavior) throws IOException {
      reader.beginObject();
      while(reader.hasNext()) {
         final String rawName = reader.nextName();
         // Defend against empty-string keys, which JSON permits but jsoup
         // rejects (Element.appendElement throws IllegalArgumentException on
         // empty tag names). See EmptyKeyBehavior javadoc for the real-world
         // example that motivated this.
         final String nextName;
         if(rawName.isEmpty()) {
            switch(emptyKeyBehavior) {
               case SKIP:
                  reader.skipValue();
                  continue;
               case THROW:
                  // Fall through with empty name; jsoup's appendElement will
                  // throw — preserves the legacy behavior for callers that
                  // explicitly opt in.
                  nextName = rawName;
                  break;
               case SUBSTITUTE:
               default:
                  nextName = EMPTY_KEY_SUBSTITUTE;
                  break;
            }
         } else {
            nextName = rawName;
         }
         JsonToken type = reader.peek();
         switch(type) {
            case BEGIN_OBJECT:
               parseObject(reader, parent.appendElement(nextName), nullBehavior, emptyKeyBehavior);
               break;
            case BEGIN_ARRAY:
               parseArray(reader, parent, nullBehavior, emptyKeyBehavior, nextName);
               break;
            case NULL:
            default:
               parseValue(reader, type, parent.appendElement(nextName), nullBehavior);
         }
      }
      reader.endObject();
   }

   private static void parseValue(final JsonReader reader,
                                  final JsonToken type,
                                  final Element parentElem,
                                  final NullBehavior nullBehavior) throws IOException {
      switch(type) {
         case NULL: {
            reader.nextNull();
            switch(nullBehavior) {
               case EMPTY:
                  break;
               case REPORT:
                  parentElem.attr("isNull", "true");
                  break;
               case IGNORE:
                  parentElem.remove();
            }
            break;
         }
         default: {
            final String value;
            switch(type) {
               case BOOLEAN:
                  value = reader.nextBoolean() ? "true" : "false";
                  break;
               default:
                  value = reader.nextString();
                  break;
            }
            parentElem.appendText(value);
         }
      }
   }
}