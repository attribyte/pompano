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
import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Resource;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.stream.Collectors;

import static com.attribyte.parser.DateParser.*;
import static com.attribyte.parser.Util.*;

public class RSSParser extends FeedParser {

   @Override
   public String name() {
      return "rss";
   }

   @Override
   protected void parseSource(final Document doc, final Resource.Builder resource) {

      resource.setTitle(childText(doc, "title"));
      resource.setDescription(childText(doc, "description"));
      resource.setRights(childText(doc, "copyright"));

      long publishTimestamp = publishTime(childText(doc, "pubDate"));
      if(publishTimestamp > 0) {
         resource.setPublishedTimestamp(publishTimestamp);
      }

      long updatedTimestamp = publishTime(childText(doc, "lastBuildDate"));
      if(updatedTimestamp > 0) {
         resource.setUpdatedTimestamp(updatedTimestamp);
      }

      String link = childText(doc, "link");

      if(link.startsWith("http://") || link.startsWith("https://")) {
         resource.setSiteLink(link);
      } else if(link.startsWith("//")) {
         String protocol = protocol(resource.getSourceLink());
         if(protocol != null) {
            resource.setSiteLink(protocol + ":" + link);
         }
      }

      Element image = firstMatch(doc, "image");
      if(image == null) {
         image = firstMatch(doc, "channel > image");
      }

      if(image != null) {
         String imageLink = childText(image, "link");

         if(imageLink.isEmpty()) {
            imageLink = childText(image, "url");
         }

         if(imageLink.isEmpty()) {
            imageLink = image.attr("rdf:resource");
         }

         if(!imageLink.isEmpty()) {
            resource.setIcon(Image.builder(imageLink).build());
         }
      }
   }

   @Override
   protected Entry.Builder parseEntry(final Element item, final ContentCleaner contentCleaner, final String baseUri) {

      Entry.Builder entry = Entry.builder();

      entry.setTitle(childText(item, "title"));

      String content = childText(item, "description");
      if(content.isEmpty()) {
         content = childText(item, "content:encoded");
      }

      if(!content.isEmpty()) {
         Document entryDoc = Jsoup.parseBodyFragment(content, baseUri);
         entry.setOriginalContent(entryDoc.body());
         if(contentCleaner != null) {
            entry.setCleanContent(contentCleaner.toCleanContent(contentCleaner.transform(entryDoc)));
         } else {
            entry.setCleanContent(content);
         }
      }

      Element guidElem = firstChild(item, "guid");
      String linkText = childText(item, "link").trim();
      if(linkText.isEmpty() && guidElem != null && !guidElem.attr("isPermalink").equalsIgnoreCase("false")) {
         linkText = guidElem.text().trim();
      }
      entry.setCanonicalLink(linkText);

      String origLinkText = childText(item, "feedburner:origlink").trim();
      if(!origLinkText.isEmpty()) {
         entry.setCanonicalLink(origLinkText);
         if(!linkText.isEmpty()) {
            entry.addAltLink(linkText);
         }
      }

      String authorName = childText(item, "dc:creator").trim();
      if(!authorName.isEmpty()) {
         entry.addAuthor(Author.builder(authorName).build());
      } else {
         String authorEmail = childText(item, "author");
         if(!authorEmail.isEmpty()) {
            int parenStart = authorEmail.indexOf("(");
            int parenEnd = authorEmail.indexOf(")");
            int atIndex = authorEmail.indexOf("@");
            if(parenStart > 0 && parenEnd > 0 && parenEnd > parenStart) {
               authorName = authorEmail.substring(parenStart + 1, parenEnd).trim(); //Use name between parens...
               authorEmail = authorEmail.substring(0, parenStart).trim();
            } else if(atIndex < 1) {
               authorName = authorEmail; //Not an email
            } //else don't set the author name.

            if(atIndex > 0) {
               entry.addAuthor(Author.builder(authorName).setEmail(authorEmail).build());
            } else {
               entry.addAuthor(Author.builder(authorName).build());
            }
         }
      }

      Element pubDate = firstChild(item, "pubdate");
      if(pubDate == null) {
         pubDate = firstChild(item, "dc:date");
      }
      if(pubDate != null) {
         long publishTimeMillis = publishTime(pubDate.text());
         if(publishTimeMillis > 0L) {
            entry.setPublishedTimestamp(publishTimeMillis);
         }
      }

      List<String> categories = item.getElementsByTag("category")
              .stream()
              .map(Element::text)
              .distinct()
              .collect(Collectors.toList());
      entry.setTags(categories);

      addMedia(item, entry);
      return entry;
   }

   @Override
   protected void parseEntries(final Document doc,
                               final ContentCleaner contentCleaner,
                               final Resource.Builder resource) {

      Elements elements = doc.getElementsByTag("item");
      for(Element element : elements) {
         resource.addEntry(parseEntry(element, contentCleaner, doc.baseUri()).build());
      }
   }

   /**
    * Attempt to parse the publish time for an item (pubDate, dc:date).
    * <p>
    *    Try the expected format first, then the Atom format.
    * </p>
    * @param pubDateStr The string to parse.
    * @return The publish timestamp or {@code 0} if unable to parse.
    */
   private static long publishTime(final String pubDateStr) {
      if(!pubDateStr.isEmpty()) {
         Long timestamp = tryParseRFC822(pubDateStr);
         if(timestamp == null) {
            timestamp = tryParseISO8601(pubDateStr);
         }
         return timestamp != null ? timestamp : 0L;
      } else {
         return 0L;
      }
   }

   /**
    * Adds media elements.
    * @param item  The item element.
    * @param entry The entry to which media is added.
    */
   private static void addMedia(final Element item, final Entry.Builder entry) {

      List<Element> mediaContentElements = item.getElementsByTag("media:content");
      for(Element contentElem : mediaContentElements) {
         String mediumAttr = contentElem.attr("medium");
         String typeAttr = contentElem.attr("type");
         if(typeAttr.toLowerCase().startsWith("image/")) {
            mediumAttr = "image";
         }

         if(mediumAttr.equalsIgnoreCase("image")) {
            String url = contentElem.attr("url");
            if(!Strings.isNullOrEmpty(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
               Image.Builder builder = Image.builder(url);
               String title = childText(contentElem, "media:title");
               if(!Strings.isNullOrEmpty(title)) {
                  builder.setTitle(title);
               }

               Integer width = Ints.tryParse(contentElem.attr("width"));
               if(width != null) {
                  builder.setWidth(width);
               }

               Integer height = Ints.tryParse(contentElem.attr("height"));
               if(height != null) {
                  builder.setHeight(height);
               }

               entry.addImage(builder.build());
               if(entry.getPrimaryImage() == null) {
                  entry.setPrimaryImage(builder.build());
               }
            }
         }
      }

      //<enclosure url="http://icdn4.digitaltrends.com/image/img_0005-100x100-c.jpg" length="0" type="image/png"/>

      Elements enclosureElements = item.getElementsByTag("enclosure");
      if(enclosureElements != null && enclosureElements.size() > 0) {
         for(Element enclosureElem : enclosureElements) {
            String type = enclosureElem.attr("type");
            if(type != null && imageEnclosureTypes.contains(type.toLowerCase().trim())) {
               String url = enclosureElem.attr("url");
               if(!Strings.isNullOrEmpty(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
                  Image.Builder builder = Image.builder(url);
                  entry.addImage(builder.build());
                  if(entry.getPrimaryImage() == null) {
                     entry.setPrimaryImage(builder.build());
                  }
               }
            }
         }
      }
   }

   /**
    * Set of types that define image enclosures.
    */
   private static final ImmutableSet<String> imageEnclosureTypes = ImmutableSet.of(
           "image/png",
           "image/jpg",
           "image/jpeg",
           "image/gif"
   );
}
