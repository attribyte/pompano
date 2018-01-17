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
import com.attribyte.parser.model.Resource;
import com.google.common.base.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

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

         return new ParseResult(resource.build());

      } catch(Error e) {
         throw e;
      } catch(Throwable t) {
         return new ParseResult(new ParseError("Atom Parser Failure", t));
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
}