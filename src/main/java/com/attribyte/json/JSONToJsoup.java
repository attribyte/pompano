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

package com.attribyte.json;

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
    * The default base URI (empty string).
    */
   private static final String BASE_URI = "";

   /**
    * Parse a JSON document to jsoup nodes.
    * @param charStream The JSON character stream.
    * @param rootElementName The root element name.
    * @param nullBehavior The behavior when handling {@code null} values.
    * @return The root element.
    * @throws IOException on input exception.
    */
   public static Element parse(final Reader charStream,
                               final String rootElementName,
                               final NullBehavior nullBehavior) throws IOException {
      Element rootElem = new Element(Tag.valueOf(rootElementName), BASE_URI);
      return parse(charStream, rootElem, nullBehavior);

   }

   /**
    * Parse a JSON document to jsoup nodes.
    * @param charStream The JSON character stream.
    * @param rootElem The root element.
    * @param nullBehavior The behavior when handling {@code null} values.
    * @return The root element.
    * @throws IOException on input exception.
    */
   public static Element parse(final Reader charStream,
                               final Element rootElem,
                               final NullBehavior nullBehavior) throws IOException {

      JsonReader reader = new JsonReader(charStream);
      reader.setLenient(true);
      JsonToken type = reader.peek();
      switch(type) {
         case BEGIN_ARRAY:
            parseArray(reader, rootElem, nullBehavior, rootElem.tagName());
            break;
         case BEGIN_OBJECT:
            parseObject(reader, rootElem, nullBehavior);
            break;
         default:
            parseValue(reader, type, rootElem.appendElement(reader.nextName()), nullBehavior);
      }

      return rootElem;
   }

   private static void parseArray(final JsonReader reader,
                                  final Element parent,
                                  final NullBehavior nullBehavior,
                                  final String elemName) throws IOException {
      reader.beginArray();
      while(reader.hasNext()) {
         JsonToken type = reader.peek();
         switch(type) {
            case BEGIN_OBJECT:
               parseObject(reader, parent.appendElement(elemName), nullBehavior);
               break;
            case BEGIN_ARRAY:
               parseArray(reader, parent.appendElement(elemName), nullBehavior, elemName);
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
                                   final NullBehavior nullBehavior) throws IOException {
      reader.beginObject();
      while(reader.hasNext()) {
         final String nextName = reader.nextName();
         JsonToken type = reader.peek();
         switch(type) {
            case BEGIN_OBJECT:
               parseObject(reader, parent.appendElement(nextName), nullBehavior);
               break;
            case BEGIN_ARRAY:
               parseArray(reader, parent, nullBehavior, nextName);
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