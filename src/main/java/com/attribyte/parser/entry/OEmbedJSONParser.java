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
import com.attribyte.parser.model.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Parse an oEmbed JSON document to create an entry.
 * @author Matt Hamer.
 */
public class OEmbedJSONParser implements com.attribyte.parser.Parser {

   @Override
   public String name() {
      return "oEmbed-json";
   }

   @Override
   public ParseResult parse(final String content, final String sourceLink, final ContentCleaner contentCleaner) {

      try {
         Resource.Builder resource = Resource.builder();
         Entry.Builder entry = Entry.builder();

         resource.setSourceLink(sourceLink);
         entry.setCanonicalLink(sourceLink);

         JsonNode root = jsonReader.readTree(content);

         String title = root.path("title").asText();
         if(!title.isEmpty()) {
            entry.setTitle(title);
         }

         String authorName = root.path("author_name").asText();
         String authorURL = root.path("author_url").asText();
         if(!authorName.isEmpty()) {
            Author.Builder author = Author.builder(authorName);
            if(!authorURL.isEmpty()) {
               author.setLink(authorURL);
            }
            entry.addAuthor(author.build());
         }

         String thumbnailURL = root.path("thumbnail_url").asText();
         if(!thumbnailURL.isEmpty()) {
            Image.Builder primaryImage = Image.builder(thumbnailURL);
            int thumbnailWidth = root.path("thumbnail_width").asInt();
            if(thumbnailWidth > 0) {
               primaryImage.setWidth(thumbnailWidth);
            }
            int thumbnailHeight = root.path("thumbnail_height").asInt();
            if(thumbnailHeight > 0) {
               primaryImage.setHeight(thumbnailHeight);
            }
            entry.setPrimaryImage(primaryImage.build());
         }

         String type = root.path("type").asText();
         switch(type) {
            case "photo": {
               String url = root.path("url").asText();
               if(!url.isEmpty()) {
                  Image.Builder image = Image.builder(url);
                  int width = root.path("width").asInt();
                  if(width > 0) {
                     image.setWidth(width);
                  }
                  int height = root.path("height").asInt();
                  if(height > 0) {
                     image.setHeight(height);
                  }
                  if(entry.getPrimaryImage() == null) {
                     entry.setPrimaryImage(image.build());
                  } else {
                     entry.addImage(image.build());
                  }
               }
            }
            break;
            case "video":
               String videoContent = root.path("html").asText();
               if(!videoContent.isEmpty()) {
                  Document doc = Jsoup.parse(videoContent, sourceLink, org.jsoup.parser.Parser.htmlParser());
                  entry.setOriginalContent(doc);
                  if(contentCleaner != null) {
                     entry.setCleanContent(contentCleaner.toCleanContent(contentCleaner.transform(doc)));
                  }
               }
               break;
            case "rich":
               String richContent = root.path("html").asText();
               if(!richContent.isEmpty()) {
                  Document doc = Jsoup.parse(richContent, sourceLink, org.jsoup.parser.Parser.htmlParser());
                  entry.setOriginalContent(doc);
                  if(contentCleaner != null) {
                     entry.setCleanContent(contentCleaner.toCleanContent(contentCleaner.transform(doc)));
                  }
               }
               break;
         }

         resource.addEntry(entry.build());
         return new ParseResult(name(), resource.build());

      } catch(Error e) {
         throw e;
      } catch(Throwable t) {
         return new ParseResult(name(), new ParseError("oEmbed Parser Failure", t));
      }
   }

   /**
    * The JSON reader.
    */
   private final ObjectReader jsonReader = new ObjectMapper().reader();
}