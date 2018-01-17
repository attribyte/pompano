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
import com.attribyte.parser.DefaultAMPCleaner;
import com.attribyte.parser.ParseError;
import com.attribyte.parser.ParseResult;
import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Iterator;

import static com.attribyte.parser.DateParser.tryParseISO8601;
import static com.attribyte.parser.Util.childText;
import static com.attribyte.parser.Util.containsImage;
import static com.attribyte.parser.Util.firstMatch;

/**
 * Parse HTML metadata to create an entry.
 * @author Matt Hamer.
 */
public class AmpParser implements com.attribyte.parser.Parser {

   @Override
   public String name() {
      return "HTML Amp";
   }

   @Override
   public ParseResult parse(final String content, final String sourceLink, final ContentCleaner contentCleaner) {

      try {
         Resource.Builder resource = Resource.builder();
         resource.setSourceLink(sourceLink);

         Entry.Builder entry = Entry.builder();
         Document doc = Jsoup.parse(content, sourceLink, org.jsoup.parser.Parser.htmlParser());

         Element head = doc.head();
         if(head == null) {
            return new ParseResult(new ParseError("AMP document must have a 'head'"));
         }

         Element canonicalLinkElem = firstMatch(head, "link[rel=canonical]");
         if(canonicalLinkElem == null) {
            return new ParseResult(new ParseError("AMP document must have a canonical link"));
         }

         String href = canonicalLinkElem.attr("href").trim();
         if(href.isEmpty()) {
            return new ParseResult(new ParseError("AMP document must have a valid canonical link"));
         }

         parseMetaJSON(head, entry);
         parseTwitterMeta(head, entry);

         if(Strings.isNullOrEmpty(entry.getTitle())) {
            Element titleElem = firstMatch(head, "title");
            if(titleElem != null) {
               String title = titleElem.text().trim();
               if(!title.isEmpty()) {
                  entry.setTitle(title);
               }
            }
         }

         entry.setCanonicalLink(href);
         if(contentCleaner != null) {
            Document transformedDoc = contentCleaner.transform(doc);
            entry.setCleanContent(contentCleaner.toCleanContent(transformedDoc));
            entry.setOriginalContent(transformedDoc.body());
         } else {
            entry.setCleanContent(defaultAMPCleaner.transform(doc).toString());
            entry.setOriginalContent(doc.body());
         }

         return new ParseResult(resource.build());

      } catch(Error e) {
         throw e;
      } catch(Throwable t) {
         return new ParseResult(new ParseError("HTML Metadata Parser Failure", t));
      }
   }

   private void parseTwitterMeta(final Element head, final Entry.Builder entry) {
      if(!Strings.isNullOrEmpty(entry.getTitle())) {
         Element twitterTitle = firstMatch(head, "meta[name=twitter:title]");
         String title = twitterTitle != null ? twitterTitle.attr("content").trim() : "";
         if(!title.isEmpty()) {
            entry.setTitle(title);
         }
      }
      if(!Strings.isNullOrEmpty(entry.getSummary())) {
         Element twitterDescription = firstMatch(head, "meta[name=twitter:description]");
         String summary = twitterDescription != null ? twitterDescription.attr("content").trim() : "";
         if(!summary.isEmpty()) {
            entry.setSummary(summary);
         }
      }

      Element twitterImage = firstMatch(head, "meta[name=twitter:image]");
      if(twitterImage != null && entry.getImages().size() == 0) {
         String url = twitterImage.attr("content").trim();
         if(!url.isEmpty()) {
            entry.addImage(Image.builder(url).build());
         }
      }
   }

   private void parseMetaJSON(final Element head, final Entry.Builder entry) {
      Elements parents = head.select("script[type=application/ld+json]");
      JsonNode obj = null;
      for(Element elem : parents) {
         try {
            obj = parseJSON(elem);
            if(childText(obj, "@context").equals("http://schema.org")) {
               break;
            }
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
      }

      if(obj == null) {
         return;
      }

      String context = childText(obj, "@context");
      if(context.equals("http://schema.org")) {
         String type = childText(obj, "@type");
         String headline = childText(obj, "headline");
         if(!headline.isEmpty() && Strings.isNullOrEmpty(entry.getTitle())) {
            entry.setTitle(headline);
         }
         String datePublished = childText(obj, "datePublished");
         Long publishedTimesttamp = tryParseISO8601(datePublished);
         if(publishedTimesttamp != null) {
            entry.setPublishedTimestamp(publishedTimesttamp);
         }

         String dateModified = childText(obj, "dateModified");
         Long modifiedTimestamp = tryParseISO8601(dateModified);
         if(modifiedTimestamp != null) {
            entry.setUpdatedTimestamp(modifiedTimestamp);
         }

         String description = childText(obj, "description");
         if(!description.isEmpty() && Strings.isNullOrEmpty(entry.getSummary())) {
            entry.setSummary(description);
         }

         JsonNode imageNode = obj.get("image");
         if(imageNode != null) {
            Iterator<JsonNode> iter;
            if(imageNode.isArray()) {
               iter = imageNode.iterator();
            } else if(imageNode.isObject()) {
               iter = ImmutableList.of(imageNode).iterator();
            } else {
               iter = ImmutableList.<JsonNode>of().iterator();
            }

            iter.forEachRemaining(node -> {
               String url = childText(node, "url").trim();
               if(!url.isEmpty() && !containsImage(entry, url)) {
                  JsonNode heightNode = node.findPath("height");
                  JsonNode widthNode = node.findPath("width");
                  int height = heightNode.isInt() ? heightNode.intValue() : 0;
                  int width = widthNode.isInt() ? widthNode.intValue() : 0;
                  Image.Builder builder = Image.builder(url);
                  if(height > 0 && width > 0) {
                     builder.setHeight(height).setWidth(width);
                  }
                  entry.addImage(builder.build());
               }
            });
         }

         if(entry.getAuthors().size() == 0) {
            JsonNode author = obj.get("author");
            final String authorName;
            if(author.isObject()) {
               authorName = childText(author, "name").trim();
            } else if(author.isTextual()) {
               authorName = author.textValue().trim();
            } else {
               authorName = "";
            }
            if(!authorName.isEmpty()) {
               entry.addAuthor(Author.builder(authorName).build());
            }
         }
      }
   }

   /**
    * Parses a JSON node.
    * @param element The parent element.
    * @return The parsed JSON node.
    * @throws IOException on JSON parse error.
    */
   private JsonNode parseJSON(final Element element) throws IOException {
      return jsonReader.readTree(element.data());
   }

   /**
    * The JSON reader.
    */
   private final ObjectReader jsonReader = new ObjectMapper().reader();

   /**
    * The default AMP cleaner.
    */
   private final DefaultAMPCleaner defaultAMPCleaner = new DefaultAMPCleaner();
}