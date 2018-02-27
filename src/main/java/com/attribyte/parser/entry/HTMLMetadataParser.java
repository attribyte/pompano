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
import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Link;
import com.attribyte.parser.model.Page;
import com.attribyte.parser.model.Resource;
import com.attribyte.parser.page.HTMLPageParser;
import com.google.common.base.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.util.List;
import java.util.Optional;


/**
 * Parse HTML metadata to create an entry.
 * @author Matt Hamer.
 */
public class HTMLMetadataParser implements com.attribyte.parser.Parser {

   @Override
   public String name() {
      return "html-metadata";
   }

   @Override
   public ParseResult parse(final String content, final String sourceLink, final ContentCleaner contentCleaner) {

      try {

         Resource.Builder resource = Resource.builder();
         resource.setSourceLink(sourceLink);

         Document doc = Jsoup.parse(content, sourceLink, Parser.htmlParser());

         Entry.Builder entry = Entry.builder();
         entry.setOriginalContent(doc);

         resource.setSourceLink(sourceLink);
         entry.setCanonicalLink(sourceLink);

         Page page = HTMLPageParser.parse(doc, sourceLink);
         if(!Strings.isNullOrEmpty(page.canonicalLink)) {
            entry.setCanonicalLink(page.canonicalLink);
            resource.setCanonicalLink(page.canonicalLink);
         }

         resource.setTitle(page.title);
         entry.setTitle(page.title);
         entry.setSummary(page.summary);

         List<Link> ampLinks = page.links("amphtml", null);
         if(ampLinks.size() > 0) {
            resource.setAmpLink(ampLinks.get(0).href);
         }

         if(!Strings.isNullOrEmpty(page.author)) {
            entry.addAuthor(Author.builder(page.author).build());
         }

         if(page.publishTime != null) {
            entry.setPublishedTimestamp(page.publishTime.getTime());
         }

         if(page.metaImages != null && page.metaImages.size() > 0) {
            entry.setImages(page.metaImages);
            entry.setPrimaryImage(page.metaImages.get(0));
         }

         if(page.metaVideos != null && page.metaVideos.size() > 0) {
            entry.setVideos(page.metaVideos);
            entry.setPrimaryVideo(page.metaVideos.get(0));
         }

         if(page.metaAudios != null && page.metaAudios.size() > 0) {
            entry.setAudios(page.metaAudios);
            entry.setPrimaryAudio(page.metaAudios.get(0));
         }

         Optional<Link> canonicalLink = page.links("canonical", null).stream().findFirst();
         canonicalLink.ifPresent(link -> entry.setCanonicalLink(link.href));

         List<Link> iconLinks = page.links("apple-touch-icon-precomposed", null);
         if(iconLinks.size() == 0) {
            iconLinks = page.links("icon", null);
         }

         if(iconLinks.size() == 0) {
            iconLinks = page.links("shortcut icon", null);
         }

         if(iconLinks.size() > 0) {
            resource.setIcon(Image.builder(iconLinks.get(0).href).build());
         }

         resource.addEntry(entry.build());
         return new ParseResult(name(), resource.build());

      } catch(Error e) {
         throw e;
      } catch(Throwable t) {
         return new ParseResult(name(), new ParseError("HTML Metadata Parser Failure", t));
      }
   }
}