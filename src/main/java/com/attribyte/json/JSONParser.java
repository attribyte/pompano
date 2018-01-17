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

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class JSONParser {

   private static final Attributes EMPTY_ATTRIBUTES = new Attributes() {

      @Override
      public int getLength() {
         return 0;
      }

      @Override
      public String getURI(final int index) {
         return null;
      }

      @Override
      public String getLocalName(final int index) {
         return null;
      }

      @Override
      public String getQName(final int index) {
         return null;
      }

      @Override
      public String getType(final int index) {
         return null;
      }

      @Override
      public String getValue(final int index) {
         return null;
      }

      @Override
      public int getIndex(final String uri, final String localName) {
         return 0;
      }

      @Override
      public int getIndex(final String qName) {
         return 0;
      }

      @Override
      public String getType(final String uri, final String localName) {
         return null;
      }

      @Override
      public String getType(final String qName) {
         return null;
      }

      @Override
      public String getValue(final String uri, final String localName) {
         return null;
      }

      @Override
      public String getValue(final String qName) {
         return null;
      }
   };

   private static final String NAMESPACE_URI = "";

   /**
    * Parse a JSON document.
    * @param source The input source.
    * @param reportNullValues Should null values be reported?
    * @param handler The SAX content handler.
    * @throws IOException on input exception.
    * @throws SAXException on parse exception.
    */
   public static void parse(InputSource source,
                            boolean reportNullValues,
                            ContentHandler handler) throws IOException, SAXException {

      Reader charStream = source.getCharacterStream();
      if(charStream == null) {
         InputStream stream = source.getByteStream();
         if(stream == null) {
            throw new SAXException("A reader or stream must be specified");
         }

         String encoding = source.getEncoding();
         if(!Strings.isNullOrEmpty((encoding))) {
            charStream = new InputStreamReader(stream, encoding);
         } else {
            charStream = new InputStreamReader(stream, Charsets.UTF_8);
         }
      }

      JsonReader reader = new JsonReader(charStream);
      reader.setLenient(true);
      handler.startDocument();
      JsonToken type = reader.peek();
      switch(type) {
         case BEGIN_ARRAY:
            handler.startElement(NAMESPACE_URI, "json", "json", EMPTY_ATTRIBUTES);
            parseArray(reader, handler, reportNullValues, "item");
            handler.endElement(NAMESPACE_URI, "json", "json");
            break;
         case BEGIN_OBJECT:
            handler.startElement(NAMESPACE_URI, "json", "json", EMPTY_ATTRIBUTES);
            parseObject(reader, handler, reportNullValues);
            handler.endElement(NAMESPACE_URI, "json", "json");
            break;
         default:
            String elemName = reader.nextName();
            parseValue(reader, type, handler, reportNullValues, elemName);
      }
      handler.endDocument();
   }

   private static void parseArray(final JsonReader reader,
                                  final ContentHandler handler,
                                  final boolean reportNullValues,
                                  final String elemName) throws IOException, SAXException {
      reader.beginArray();
      while(reader.hasNext()) {
         JsonToken type = reader.peek();
         switch(type) {
            case BEGIN_OBJECT:
               handler.startElement(NAMESPACE_URI, elemName, elemName, EMPTY_ATTRIBUTES);
               parseObject(reader, handler, reportNullValues);
               handler.endElement(NAMESPACE_URI, elemName, elemName);
               break;
            case BEGIN_ARRAY:
               handler.startElement(NAMESPACE_URI, elemName, elemName, EMPTY_ATTRIBUTES);
               parseArray(reader, handler, reportNullValues, elemName);
               handler.endElement(NAMESPACE_URI, elemName, elemName);
               break;
            case NULL:
            default:
               parseValue(reader, type, handler, reportNullValues, elemName);
         }
      }
      reader.endArray();
   }

   private static void parseObject(final JsonReader reader,
                                   final ContentHandler handler,
                                   final boolean reportNullValues) throws IOException, SAXException {
      reader.beginObject();
      while(reader.hasNext()) {
         String elemName = reader.nextName();
         JsonToken type = reader.peek();
         switch(type) {
            case BEGIN_OBJECT:
               handler.startElement(NAMESPACE_URI, elemName, elemName, EMPTY_ATTRIBUTES);
               parseObject(reader, handler, reportNullValues);
               handler.endElement(NAMESPACE_URI, elemName, elemName);
               break;
            case BEGIN_ARRAY:
               handler.startElement(NAMESPACE_URI, elemName, elemName, EMPTY_ATTRIBUTES);
               parseArray(reader, handler, reportNullValues, "item");
               handler.endElement(NAMESPACE_URI, elemName, elemName);
               break;
            case NULL:
            default:
               parseValue(reader, type, handler, reportNullValues, elemName);
         }

      }
      reader.endObject();
   }

   private static void parseValue(final JsonReader reader,
                                  final JsonToken type,
                                  final ContentHandler handler,
                                  final boolean reportNullValues,
                                  final String elemName) throws IOException, SAXException {
      switch(type) {
         case NULL: {
            reader.nextNull();
            if(reportNullValues) {
               handler.startElement(NAMESPACE_URI, elemName, elemName, EMPTY_ATTRIBUTES);
               handler.endElement(NAMESPACE_URI, elemName, elemName);
            }
            break;
         }
         default: {
            handler.startElement(NAMESPACE_URI, elemName, elemName, EMPTY_ATTRIBUTES);
            String value;
            switch(type) {
               case BOOLEAN:
                  value = reader.nextBoolean() ? "true" : "false";
                  break;
               default:
                  value = reader.nextString();
                  break;
            }

            char[] valueChars = value.toCharArray();
            handler.characters(valueChars, 0, valueChars.length);
            handler.endElement(NAMESPACE_URI, elemName, elemName);
         }
      }
   }
}
