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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Set;

import static com.attribyte.parser.DateParser.*;
import static com.attribyte.parser.Util.httpURL;
import static com.attribyte.parser.Util.protocol;

public class AtomParser extends FeedParser {

   @Override
   public String name() {
      return "atom";
   }

   @Override
   protected void parseSource(final Document doc, final Resource.Builder resource) {

      String title = childText(doc, "title");
      if(!title.isEmpty()) {
         resource.setTitle(title);
      }

      String subtitle = childText(doc, "subtitle");
      if(!subtitle.isEmpty()) {
         resource.setSubtitle(subtitle);
      }

      String updated = childText(doc, "updated");
      if(!updated.isEmpty()) {
         Long maybeUpdated = tryParseISO8601(updated);
         if(maybeUpdated != null) {
            resource.setUpdatedTimestamp(maybeUpdated);
         }
      }

      String description = childText(doc, "subtitle");
      if(description.isEmpty()) {
         description = childText(doc, "tagline");
      }

      if(!description.isEmpty()) {
         resource.setDescription(description);
      }

      String protocol = protocol(resource.getSourceLink());

      List<Element> altElements = doc.select("feed > link[rel=alternate]");
      if(altElements.size() > 0) {
         Element altLink = altElements.get(0);
         String link = httpURL(altLink.attr("href"), protocol);
         if(link != null) {
            resource.setSiteLink(link);
         }
      }
   }

   @Override
   protected Entry.Builder parseEntry(final Element elem, final ContentCleaner contentCleaner, final String baseUri) {

      Entry.Builder entry = Entry.builder();
      entry.setTitle(childText(elem, "title"));
      String protocol = protocol(baseUri);

      List<Element> altLinks = elem.select("link[rel=alternate]");
      for(Element altLink : altLinks) {
         String href = httpURL(altLink.attr("href"), protocol);
         if(href != null) {
            entry.setCanonicalLink(href);
            break;
         }
      }

      Elements feedburnerLinks = elem.getElementsByTag("feedburner:origlink");
      for(Element flink : feedburnerLinks) {
         String href = httpURL(flink.text(), protocol);
         if(href != null) {
            if(entry.getCanonicalLink() != null) {
               entry.addAltLink(entry.getCanonicalLink());
            }
            entry.setCanonicalLink(href);
            break;
         }
      }

      long published = publishTime(childText(elem, "published"));
      if(published < 1L) {
         published = publishTime(childText(elem, "issued"));
      }

      if(published > 0) {
         entry.setPublishedTimestamp(published);
      }

      long updated = publishTime(childText(elem, "updated"));
      if(updated > 0) {
         entry.setUpdatedTimestamp(updated);
         if(entry.getPublishedTimestamp() < 1) {
            entry.setPublishedTimestamp(updated);
         }
      }

      Elements authors = elem.getElementsByTag("author");
      for(Element author : authors) {
         String name = childText(author, "name");
         if(!name.isEmpty()) {
            Author.Builder authorBuilder = Author.builder(name);
            String authorEmail = childText(author, "email");
            if(!authorEmail.isEmpty()) {
               authorBuilder.setEmail(authorEmail);
            }
            entry.addAuthor(authorBuilder.build());
            break;
         }
      }

      entry.setSummary(childText(elem, "summary"));

      String content = childText(elem, "content");
      if(!content.isEmpty()) {
         Document entryDoc = Jsoup.parseBodyFragment(content, baseUri);
         entry.setOriginalContent(entryDoc);
         if(contentCleaner != null) {
            entry.setCleanContent(contentCleaner.toCleanContent(contentCleaner.transform(entryDoc)));
         } else {
            entry.setCleanContent(content);
         }
      }

      Elements categories = elem.getElementsByTag("category");
      Set<String> names = Sets.newHashSetWithExpectedSize(8);
      for(Element category : categories) {
         String name = category.attr("term");
         if(name.isEmpty()) {
            name = category.attr("label");
         }
         if(!name.isEmpty() && !names.contains(name)) {
            entry.addTag(name);
            names.add(name);
         }
      }

      // Look for images in enclosures.
      Elements links = elem.getElementsByTag("link");
      for(Element linkElem : links) {
         String rel = linkElem.attr("rel");
         if((rel.equalsIgnoreCase("enclosure"))) {
            boolean isAllowedImage = isAllowedEnclosureImage(linkElem.attr("type"));
            if(isAllowedImage) {
               addImage(entry, linkElem.attr("href"), protocol);
            }
         }
      }

      return entry;
   }

   @Override
   protected void parseEntries(final Document doc,
                               final ContentCleaner contentCleaner,
                               final Resource.Builder resource) {

      Elements elements = doc.getElementsByTag("entry");
      for(Element element : elements) {
         resource.addEntry(parseEntry(element, contentCleaner, doc.baseUri()).build());
      }
   }

   /**
    * Attempt to parse the publish time for an item (pubDate, dc:date).
    * <p>
    *    Try the expected format first, then the RSS format.
    * </p>
    * @param pubDateStr The string to parse.
    * @return The publish timestamp or {@code 0} if unable to parse.
    */
   private static long publishTime(final String pubDateStr) {
      if(!pubDateStr.isEmpty()) {
         Long timestamp = tryParseISO8601(pubDateStr);
         if(timestamp == null) {
            timestamp = tryParseRFC822(pubDateStr);
         }
         return timestamp != null ? timestamp : 0L;
      } else {
         return 0L;
      }
   }

   /**
    * The content of an element as text.
    * @param parent The parent element.
    * @param name The child name.
    * @return The text.
    */
   private static String childText(Element parent, String name) {
      Elements elements = parent.getElementsByTag(name);
      if(elements.size() == 0) {
         return "";
      }
      Element elem = elements.get(0);
      String type = elem.attr("type");
      if(type.equalsIgnoreCase("xhtml")) {
         Elements div = elem.getElementsByTag("div");
         if(div.size() > 0) {
            StringBuilder buf = new StringBuilder();
            div.first().children().forEach(e -> buf.append(e.toString()));
            return buf.toString();
         } else {
            return "";
         }
      } else {
         return elem.text();
      }
   }

   /**
    * Image content types allowed for enclosures.
    */
   private static final ImmutableMap<String, String> enclosureImageContentTypeMap =
           new ImmutableMap.Builder<String, String>()
                   .put("image/gif", "image/gif")
                   .put("image/png", "image/png")
                   .put("image/jpeg", "image/jpg")
                   .put("image/jpe", "image/jpg")
                   .put("image/jpg", "image/jpg").build();

   /**
    * Is the image allowed as an enclosure.
    * @param contentType The content type.
    * @return Is the image allowed?
    */
   private static boolean isAllowedEnclosureImage(final String contentType) {
      return contentType != null && enclosureImageContentTypeMap.containsKey(contentType.trim().toLowerCase());
   }
}
