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

package com.attribyte.parser.entry;

import com.attribyte.parser.ContentCleaner;
import com.attribyte.parser.ParseError;
import com.attribyte.parser.ParseResult;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Resource;
import com.google.common.base.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import static com.attribyte.parser.Util.httpURL;

public abstract class FeedParser implements com.attribyte.parser.Parser {

   @Override
   public ParseResult parse(final String content, final String sourceLink, final ContentCleaner contentCleaner) {

      try {

         Resource.Builder resource = Resource.builder();
         if(!Strings.isNullOrEmpty(sourceLink)) {
            resource.setSourceLink(sourceLink);
         }

         Document doc = Jsoup.parse(content, sourceLink, Parser.xmlParser());
         parseEntries(doc, contentCleaner, resource);
         parseSource(doc, resource);

         return new ParseResult(name(), resource.build());

      } catch(Error e) {
         throw e;
      } catch(Throwable t) {
         return new ParseResult(name(), new ParseError("Parse Failure", t));
      }
   }

   /**
    * Parse the feed source properties.
    * @param doc The parsed document.
    * @param resource The resource to which source properties are added.
    */
   protected abstract void parseSource(final Document doc, final Resource.Builder resource);

   /**
    * Parse an entry.
    * @param elem The entry element.
    * @param contentCleaner The content cleaner.
    * @param baseUri The base URI for relative references.
    * @return The parsed entry (builder).
    */
   protected abstract Entry.Builder parseEntry(final Element elem, final ContentCleaner contentCleaner, final String baseUri);

   /**
    * Parse all entries and add to the resource.
    * @param doc The parent document.
    * @param contentCleaner The content cleaner.
    * @param resource The resource.
    */
   protected abstract void parseEntries(final Document doc,
                                        final ContentCleaner contentCleaner,
                                        final Resource.Builder resource);

   /**
    * Create an image builder for a URL.
    * @param url The URL.
    * @param protocol For protocol-less links.
    * @return The builder or {@code null}.
    */
   protected static Image.Builder image(final String url, final String protocol) {
      String imageURL = httpURL(url, protocol);
      if(imageURL != null) {
         return Image.builder(imageURL);
      } else {
         return null;
      }
   }

   /**
    * Adds an image to an entry and if entry has no primary image, sets primary image to this image.
    * @param entry The entry (builder).
    * @param builder The image builder.
    * @return The input entry builder.
    */
   protected static Entry.Builder addImage(final Entry.Builder entry,
                                           final Image.Builder builder) {
      if(builder != null) {
         Image image = builder.build();
         entry.addImage(image);
         if(entry.getPrimaryImage() == null) {
            entry.setPrimaryImage(image);
         }
      }
      return entry;
   }

   /**
    * Adds an image to an entry if a valid URL and sets the primary image, if unset.
    * @param entry The entry.
    * @param url The url.
    * @param protocol The protocol to use for protocol-less links.
    * @return The input entry builder.
    */
   protected static Entry.Builder addImage(final Entry.Builder entry,
                                           final String url,
                                           final String protocol) {
      return addImage(entry, image(url, protocol));
   }
}
