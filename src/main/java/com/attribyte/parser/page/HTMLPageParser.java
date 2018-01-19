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

package com.attribyte.parser.page;

import com.attribyte.json.JSONToJsoup;
import com.attribyte.parser.model.Anchor;
import com.attribyte.parser.model.Audio;
import com.attribyte.parser.model.DataURI;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Link;
import com.attribyte.parser.model.Page;
import com.attribyte.parser.model.Video;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.attribyte.parser.DateParser.tryParseISO8601;
import static com.attribyte.parser.DateParser.tryParseRFC822;
import static com.attribyte.parser.Util.host;

/**
 * Extract interesting content from HTML as a {@code Page}.
 * @author Matt Hamer
 */
public class HTMLPageParser {

   /**
    * Parse from a URL.
    * @param url The URL.
    * @param maxBodySize The maximum allowed body size.
    * @return The page.
    * @throws IOException on parse error.
    */
   public static Page parseFromURL(final String url, int maxBodySize) throws IOException {
      Connection conn = Jsoup.connect(url);
      conn.followRedirects(true);
      conn.ignoreHttpErrors(true);
      return(parse(conn.get(), url));
   }

   /**
    * Parse a page from a string.
    * @param content The string to parse.
    * @param defaultCanonicalLink The default canonical link to use.
    * @return The page.
    */
   public static Page parse(final String content, final String defaultCanonicalLink) {
      return parse(Jsoup.parse(content), defaultCanonicalLink);
   }


   /**
    * Parse a page from a string containing a fragment of HTML.
    * @param content The string to parse.
    * @param defaultCanonicalLink The default canonical link to use.
    * @return The page.
    */
   public static Page parseFragment(final String content, final String defaultCanonicalLink) {
      return parse(Jsoup.parseBodyFragment(content), defaultCanonicalLink);
   }

   /**
    * Parse a page from a Jsoup document.
    * @param doc The document,
    * @param defaultCanonicalLink The default canonical link for the page.
    * @return The parsed page.
    */
   public static Page parse(final Document doc, final String defaultCanonicalLink) {

      Element entryMetadata = new Element(Tag.valueOf("entry-metadata"), "");
      jsonMetadata(doc, entryMetadata);
      String canonicalLink = bestCanonicalLink(doc, entryMetadata);
      String bestCanonicalLink = Strings.isNullOrEmpty(canonicalLink) ? defaultCanonicalLink : canonicalLink;
      doc.setBaseUri(bestCanonicalLink);

      Elements anchorElements = doc.getElementsByTag("a");
      LinkedHashSet<Anchor> anchors = Sets.newLinkedHashSetWithExpectedSize(anchorElements.size());
      for(Element anchorElement : anchorElements) {
         String href = anchorElement.absUrl("href").trim();
         if(!href.isEmpty()) {
            String title = anchorElement.attr("title").trim();
            String anchorText = anchorElement.text().trim();
            Anchor anchor = new Anchor(href, title, anchorText);
            if(!anchors.contains(anchor)) {
               anchors.add(anchor);
            }
         }
      }

      Elements imageElements = doc.getElementsByTag("img");
      LinkedHashSet<Image> images = Sets.newLinkedHashSetWithExpectedSize(imageElements.size() + 4);
      LinkedHashSet<Image> metaImages = Sets.newLinkedHashSetWithExpectedSize(4);

      metaImages(doc, entryMetadata, images, metaImages);
      for(Element imageElement : imageElements) {
         String src = imageElement.attr("src");
         if(!src.isEmpty()) {
            if(src.startsWith("data:image")) {
               try {
                  DataURI duri = new DataURI(src);
                  if(duri.base64Encoded && !duri.data.isEmpty()) {
                     Image image = Image.builder(duri.data.toByteArray()).setMediaType(duri.mediaType).build();
                     if(!images.contains(image)) {
                        images.add(image);
                     }

                  } //Otherwise ignore...

               } catch(IllegalArgumentException iae) {
                  //Ignore images with invalid data URI in src...
               }
            } else {
               src = imageElement.absUrl("src");
               String alt = imageElement.attr("alt");
               String title = imageElement.attr("title");
               Integer height = Ints.tryParse(imageElement.attr("height"));
               Integer width = Ints.tryParse(imageElement.attr("width"));
               Image image = Image.builder(src).setAltText(alt).setTitle(title)
                       .setHeight(height != null ? height : 0)
                       .setWidth(width != null ? width : 0)
                       .build();
               if(!images.contains(image)) {
                  images.add(image);
               }
            }
         }
      }

      Elements videoElements = doc.getElementsByTag("video");
      LinkedHashSet<Video> videos = Sets.newLinkedHashSetWithExpectedSize(videoElements.size() + 4);
      LinkedHashSet<Video> metaVideos = Sets.newLinkedHashSetWithExpectedSize(4);
      ogMetaVideos(doc, videos, metaVideos);
      for(Element videoElement : videoElements) {
         Integer width = Ints.tryParse(videoElement.attr("width"));
         Integer height = Ints.tryParse(videoElement.attr("height"));
         String title = videoElement.attr("title");
         String altText = videoElement.ownText();
         Elements sources = videoElement.getElementsByTag("source");
         for(Element source : sources) {
            String src = source.absUrl("src");
            if(!src.isEmpty()) {
               String type = source.attr("type");
               Video video = Video.builder(src)
                       .setTitle(title)
                       .setAltText(altText)
                       .setWidth(width != null ? width : 0)
                       .setHeight(height != null ? height : 0)
                       .setType(type)
                       .build();
               if(!videos.contains(video)) {
                  videos.add(video);
               }
            }
         }
      }

      Elements audioElements = doc.getElementsByTag("audio");
      LinkedHashSet<Audio> audios = Sets.newLinkedHashSetWithExpectedSize(videoElements.size() + 4);
      LinkedHashSet<Audio> metaAudios = Sets.newLinkedHashSetWithExpectedSize(4);
      ogMetaAudio(doc, audios, metaAudios);
      for(Element audioElement : audioElements) {
         Elements sources = audioElement.getElementsByTag("source");
         String title = audioElement.attr("title");
         String altText = audioElement.ownText();
         for(Element source : sources) {
            String src = source.absUrl("src");
            if(!src.isEmpty()) {
               String type = source.attr("type");
               Audio audio = Audio.builder(src)
                       .setTitle(title)
                       .setAltText(altText)
                       .setType(type)
                       .build();
               if(!audios.contains(audio)) {
                  audios.add(audio);
               }
            }
         }
      }

      Elements linkElements = doc.getElementsByTag("link");
      LinkedHashSet<Link> links = Sets.newLinkedHashSetWithExpectedSize(linkElements.size() + 4);
      for(Element linkElement : linkElements) {
         String href = linkElement.absUrl("href");
         if(!href.isEmpty()) {
            String rel = linkElement.attr("rel");
            String type = linkElement.attr("type");
            String title = linkElement.attr("title");
            Link link = new Link(href, rel, type, title);
            if(!links.contains(link)) {
               links.add(link);
            }
         }
      }

      Set<Link> linkSet = selfLinks(doc, entryMetadata);
      if(!Strings.isNullOrEmpty(defaultCanonicalLink)) {
         linkSet.add(new Link(defaultCanonicalLink, "self", "text/html", ""));
      }

      final String chost = host(bestCanonicalLink);

      if(chost != null && chost.equals("twitter.com")) {

         String title = bestTitle(doc, entryMetadata);
         int authorEnd = title.toLowerCase().lastIndexOf("on twitter");
         final String author;
         if(authorEnd > 0) {
            author = title.substring(0, authorEnd).trim();
         } else {
            author = bestAuthor(doc, entryMetadata);
         }
         return new Page(
                 doc,
                 bestCanonicalLink,
                 linkSet,
                 bestSiteName(doc),
                 title,
                 bestSummary(doc),
                 author,
                 bestPublishTime(doc, entryMetadata),
                 anchors,
                 images,
                 metaImages,
                 videos,
                 metaVideos,
                 audios,
                 metaAudios,
                 links);
      } else {
         return new Page(
                 doc,
                 bestCanonicalLink,
                 linkSet,
                 bestSiteName(doc),
                 bestTitle(doc, entryMetadata),
                 bestSummary(doc),
                 bestAuthor(doc, entryMetadata),
                 bestPublishTime(doc, entryMetadata),
                 anchors,
                 images,
                 metaImages,
                 videos,
                 metaVideos,
                 audios,
                 metaAudios,
                 links);
      }
   }

   /**
    * Find the "best" site name.
    * @param doc The source document.
    * @return The "best" site name.
    */
   private static String bestSiteName(final Document doc) {
      for(String pattern : siteNameMetaPatterns) {
         Elements match = doc.select(pattern);
         String name = firstAttribute(match, "content");
         if(!name.isEmpty()) {
            return name;
         }
      }
      return "";
   }

   /**
    * Find the "best" canonical link.
    * @param doc The source document.
    * @param otherMetadata Special or external metadata.
    * @return The "best" canonical link or an empty string if none.
    */
   private static String bestCanonicalLink(final Document doc, final Element otherMetadata) {

      for(String pattern : linkPatterns) {
         Elements match = doc.select(pattern);
         String href = firstAbsoluteURL(match, "href");
         if(!href.isEmpty()) {
            return href;
         }
      }

      for(String pattern : linkMetaPatterns) {
         Elements match = doc.select(pattern);
         String href = firstAbsoluteURL(match, "content");
         if(!href.isEmpty()) {
            return href;
         }
      }

      String link = firstElementText(otherMetadata, "link").trim();
      if(!link.isEmpty()) {
         if(link.startsWith("http://") || link.startsWith("https://")) {
            return link;
         }
      }

      link = firstElementText(otherMetadata, "url").trim();

      if(!link.isEmpty()) {
         if(link.startsWith("http://") || link.startsWith("https://")) {
            return link;
         }
      }

      return "";
   }

   /**
    * Find a set of unique "self" links.
    * @param doc The source document.
    * @param otherMetadata Special or external metadata.
    * @return The set of links.
    */
   private static Set<Link> selfLinks(final Document doc, final Element otherMetadata) {
      Set<Link> linkSet = Sets.newHashSet();

      for(String pattern : linkPatterns) {
         Elements match = doc.select(pattern);
         String href = firstAttribute(match, "href");
         if(!href.isEmpty()) {
            linkSet.add(new Link(href, "self", "text/html", ""));
         }
      }

      for(String pattern : altLinkPatterns) {
         Elements match = doc.select(pattern);
         String href = firstAttribute(match, "href");
         if(!href.isEmpty()) {
            linkSet.add(new Link(href, "self", "text/html", ""));
         }
      }

      for(String pattern : linkMetaPatterns) {
         Elements match = doc.select(pattern);
         String href = firstAttribute(match, "content");
         if(!href.isEmpty()) {
            linkSet.add(new Link(href, "self", "text/html", ""));
         }
      }

      String link = firstElementText(otherMetadata, "link");
      if(!link.isEmpty()) {
         linkSet.add(new Link(link, "self", "text/html", ""));
      }

      return linkSet;
   }

   /**
    * Find the "best" title.
    * @param doc The source document.
    * @param otherMetadata Special or external metadata.
    * @return The "best" title or an empty string if none.
    */
   private static String bestTitle(final Document doc, final Element otherMetadata) {

      String title = firstElementText(otherMetadata, "title");
      if(title.isEmpty()) {
         title = firstElementText(otherMetadata, "headline");
      }

      if(!title.isEmpty()) {
         return title;
      }

      for(String pattern : titleMetaPatterns) {
         Elements match = doc.select(pattern);
         title = firstAttribute(match, "content");
         if(!title.isEmpty()) {
            return title;
         }
      }

      return doc.title();
   }

   /**
    * Find the "best" summary.
    * @param doc The source document.
    * @return The "best" author or an empty string if none.
    */
   private static String bestSummary(final Document doc) {
      for(String pattern : descriptionMetaPatterns) {
         Elements match = doc.select(pattern);
         String description = firstAttribute(match, "content");
         if(!description.isEmpty()) {
            return description;
         }
      }
      return "";
   }

   /**
    * Does the author name appear to be valid.
    * @param author The author.
    * @return Is the name valid?
    */
   public static boolean isValidAuthor(final String author) {
      return !author.isEmpty()
              && !author.startsWith("http://")
              && !author.startsWith("https://")
              && countSpaces(author) < 3;
   }

   private static int countSpaces(final String str) {
      if(str == null) return 0;
      int count = 0;
      char[] chars = str.toCharArray();
      for(char ch : chars) {
         if(Character.isSpaceChar(ch)) count++;
      }
      return count;
   }

   /**
    * Find the "best" author.
    * @param doc The source document.
    * @param otherMetadata Special or external metadata.
    * @return The "best" author or an empty string if none.
    */
   private static String bestAuthor(final Document doc, final Element otherMetadata) {

      if(isComposite(otherMetadata, "author")) {
         Element authorElem = firstElement(otherMetadata, "author");
         if(authorElem != null) {
            String name = firstElementText(authorElem, "name");
            if(isValidAuthor(name)) {
               return name;
            }
         }
      } else {
         String name = firstElementText(otherMetadata, "author");
         if(isValidAuthor(name)) {
            return name;
         }
      }

      String name = firstElementText(otherMetadata, "creator");
      if(isValidAuthor(name)) {
         return name;
      }

      name = firstElementText(otherMetadata, "byline");
      if(isValidAuthor(name)) {
         return name;
      }

      name = firstElementText(otherMetadata, "author_name");
      if(isValidAuthor(name)) {
         return name;
      }

      name = firstElementText(otherMetadata, "author_nickname");
      if(isValidAuthor(name)) {
         return name;
      }

      for(String pattern : authorMetaPatterns) {
         Elements match = doc.select(pattern);
         String author = firstAttribute(match, "content");
         if(isValidAuthor(author)) {
            return author;
         }
      }

      for(String pattern : authorElementPatterns) {
         Elements match = doc.body().select(pattern);
         if(match.size() > 0) {
            Element anchor = match.get(0);
            String author = anchor.text().trim();
            if(isValidAuthor(author)) {
               return author;
            }
         }
      }

      return "";
   }

   /**
    * Find the "best" publish time.
    * @param doc The source document.
    * @param otherMetadata Special or external metadata.
    * @return The "best" publish time or null if none.
    */
   private static Date bestPublishTime(final Document doc, final Element otherMetadata) {

      String dateStr = firstElementText(otherMetadata, "pub_date");

      if(dateStr.isEmpty()) {
         dateStr = firstElementText(otherMetadata, "published_date");
      }

      if(dateStr.isEmpty()) {
         dateStr = firstElementText(otherMetadata, "dateCreated");
      }

      if(!dateStr.isEmpty()) {
         Date date = parseDateTime(dateStr);
         if(date != null) {
            return date;
         }
      }

      for(String pattern : publishTimeMetaPatterns) {
         Elements match = doc.select(pattern);
         dateStr = firstAttribute(match, "content");
         if(!dateStr.isEmpty()) {
            Date date = parseDateTime(dateStr);
            if(date != null) {
               return date;
            }
         }
      }

      for(String pattern : publishTimeElementPatterns) {
         Elements match = doc.select(pattern);
         if(match.size() > 0) {
            Element dateElem = match.get(0);
            dateStr = dateElem.text().trim();
            if(!dateStr.isEmpty()) {
               Date date = parseDateTime(dateStr);
               if(date != null) {
                  return date;
               }
            }
         }
      }

      return null;
   }

   /**
    * Find images in metadata.
    * @param doc The source document.
    * @param otherMetadata Special or external metadata.
    * @param images The set of all images.
    * @param metaImages The set of all images found in metadata.
    */
   private static void metaImages(final Document doc, final Element otherMetadata,
                                  final LinkedHashSet<Image> images,
                                  final LinkedHashSet<Image> metaImages) {

      ogMetaImages(doc, images, metaImages);

      String src = firstElementText(otherMetadata, "image_url");
      if(src.isEmpty()) {
         src = firstElementText(otherMetadata, "image");
      }

      if(!src.isEmpty()) {
         Image image = Image.builder(src).build();
         if(!images.contains(image)) {
            images.add(image);
            metaImages.add(image);
         }
      }

      for(String pattern : imageMetaPatterns) {
         Elements match = doc.select(pattern);
         src = firstAttribute(match, "content");
         if(!src.isEmpty()) {
            Image image = Image.builder(src).build();
            if(!images.contains(image)) {
               images.add(image);
               metaImages.add(image);
            }
         }
      }
   }

   /**
    * Parse Open Graph images.
    * @param doc The source document.
    * @param images The set of all images.
    * @param metaImages The set of images found in metadata.
    */
   private static void ogMetaImages(final Document doc,
                                    final LinkedHashSet<Image> images,
                                    final LinkedHashSet<Image> metaImages) {

      final String propertyKey;
      final String contentKey;

      Elements imageMatch = doc.select("meta[property=og:image]");
      if(imageMatch.isEmpty()) {
         imageMatch = doc.select("meta[property=og:image:url]");
         if(imageMatch.isEmpty()) {

            imageMatch = doc.select("meta[name=og:image]");
            if(!imageMatch.isEmpty()) {
               propertyKey = "name";
               contentKey = "content";
            } else {
               return;
            }
         } else {
            propertyKey = "property";
            contentKey = "content";
         }
      } else {
         propertyKey = "property";
         contentKey = "content";
      }

      for(Element elem : imageMatch) {
         String content = elem.absUrl(contentKey);
         if(!content.isEmpty()) {
            Image.Builder imageBuilder = Image.builder(content);
            Element nextSibling = elem;
            while((nextSibling = nextSibling.nextElementSibling()) != null &&
                    !nextSibling.attr(propertyKey).equals("og:image:url") &&
                    nextSibling.attr(propertyKey).startsWith("og:image:")) {
               switch(nextSibling.attr(propertyKey)) {
                  case "og:image:alt":
                     imageBuilder.setAltText(nextSibling.attr(contentKey));
                     break;
                  case "og:image:width":
                     Integer width = Ints.tryParse(nextSibling.attr(contentKey));
                     if(width != null) {
                        imageBuilder.setWidth(width);
                     }
                     break;
                  case "og:image:height":
                     Integer height = Ints.tryParse(nextSibling.attr(contentKey));
                     if(height != null) {
                        imageBuilder.setHeight(height);
                     }
                     break;
               }
            }

            Image image = imageBuilder.build();
            if(!images.contains(image)) {
               metaImages.add(image);
               images.add(image);
            }
         }
      }
   }

   /**
    * Parse Open Graph videos.
    * @param doc The source document.
    * @param videos The set of all videos.
    * @param metaVideos The set of videos found in metadata.
    */
   private static void ogMetaVideos(final Document doc,
                                    final LinkedHashSet<Video> videos,
                                    final LinkedHashSet<Video> metaVideos) {
      final String propertyKey;
      final String contentKey;

      Elements videoMatch = doc.select("meta[property=og:video]");
      if(videoMatch.isEmpty()) {
         videoMatch = doc.select("meta[property=og:video:url]");
         if(videoMatch.isEmpty()) {
            videoMatch = doc.select("meta[name=og:video]");
            if(!videoMatch.isEmpty()) {
               propertyKey = "name";
               contentKey = "content";
            } else {
               return;
            }
         } else {
            propertyKey = "property";
            contentKey = "content";
         }
      } else {
         propertyKey = "property";
         contentKey = "content";
      }

      for(Element elem : videoMatch) {
         String content = elem.absUrl(contentKey);
         if(!content.isEmpty()) {
            Video.Builder videoBuilder = Video.builder(content);
            Element nextSibling = elem;
            while((nextSibling = nextSibling.nextElementSibling()) != null &&
                   !nextSibling.attr(propertyKey).equals("og:video:url") &&
                   nextSibling.attr(propertyKey).startsWith("og:video:")) {
               switch(nextSibling.attr(propertyKey)) {
                  case "og:video:type":
                     videoBuilder.setType(nextSibling.attr(contentKey));
                     break;
                  case "og:video:width":
                     Integer width = Ints.tryParse(nextSibling.attr(contentKey));
                     if(width != null) {
                        videoBuilder.setWidth(width);
                     }
                     break;
                  case "og:video:height":
                     Integer height = Ints.tryParse(nextSibling.attr(contentKey));
                     if(height != null) {
                        videoBuilder.setHeight(height);
                     }
                     break;
               }
            }

            Video video = videoBuilder.build();
            if(!videos.contains(video)) {
               metaVideos.add(video);
               videos.add(video);
            }
         }
      }
   }

   /**
    * Parse Open Graph audio.
    * @param doc The source document.
    * @param audios The set of all audio streams.
    * @param metaAudios The set of audio streams found in metadata.
    */
   private static void ogMetaAudio(final Document doc,
                                   final LinkedHashSet<Audio> audios,
                                   final LinkedHashSet<Audio> metaAudios) {
      final String propertyKey;
      final String contentKey;

      Elements audioMatch = doc.select("meta[property=og:audio]");
      if(audioMatch.isEmpty()) {
         audioMatch = doc.select("meta[property=og:audio:url]");
         if(audioMatch.isEmpty()) {
            audioMatch = doc.select("meta[name=og:audio]");
            if(!audioMatch.isEmpty()) {
               propertyKey = "name";
               contentKey = "content";
            } else {
               return;
            }
         } else {
            propertyKey = "property";
            contentKey = "content";
         }
      } else {
         propertyKey = "property";
         contentKey = "content";
      }

      for(Element elem : audioMatch) {
         String content = elem.absUrl(contentKey);
         if(!content.isEmpty()) {
            Audio.Builder audioBuilder = Audio.builder(content);
            Element nextSibling = elem;
            while((nextSibling = nextSibling.nextElementSibling()) != null &&
                    !nextSibling.attr(propertyKey).equals("og:audio:url") &&
                    nextSibling.attr(propertyKey).startsWith("og:audio:")) {
               switch(nextSibling.attr(propertyKey)) {
                  case "og:audio:type":
                     audioBuilder.setType(nextSibling.attr(contentKey));
                     break;
               }
            }

            Audio audio = audioBuilder.build();
            if(!audios.contains(audio)) {
               metaAudios.add(audio);
               audios.add(audio);
            }
         }
      }
   }

   /**
    * Gets the value of the first matching attribute in a collection of elements.
    * @param match The match elements.
    * @param contentAttr The attribute for content.
    * @return The value or an empty string if not found.
    */
   private static String firstAttribute(final Elements match, final String contentAttr) {
      for(Element elem : match) {
         String content = elem.attr(contentAttr).trim();
         if(!content.isEmpty()) {
            return content;
         }
      }
      return "";
   }

   /**
    * Gets the value of the first matching attribute as an absolute URL in a collection of elements.
    * @param match The match elements.
    * @param contentAttr The attribute for content.
    * @return The absolute URL or an empty string if not found.
    */
   private static String firstAbsoluteURL(final Elements match, final String contentAttr) {
      for(Element elem : match) {
         String content = elem.absUrl(contentAttr).trim();
         if(!content.isEmpty()) {
            return content;
         }
      }
      return "";
   }

   /**
    * Gets the text of the first matching element.
    * @param parent The parent element.
    * @param contentElement The name of the content element.
    * @return The text of the content element or an empty string if not found.
    */
   private static String firstElementText(final Element parent, final String contentElement) {
      if(parent != null) {
         Elements elems = parent.getElementsByTag(contentElement);
         if(elems.size() > 0) {
            return elems.get(0).ownText();
         } else {
            return "";
         }
      } else {
         return "";
      }
   }

   /**
    * Returns the first matching element.
    * @param parent The parent element.
    * @param elementName The name of the content element.
    * @return The matching element or {@code null} if not found.
    */
   private static Element firstElement(final Element parent, final String elementName) {
      if(parent != null) {
         Elements elems = parent.getElementsByTag(elementName);
         if(elems.size() > 0) {
            return elems.get(0);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   /**
    * Determine if an element has a matching composite element.
    * @param parent The parent element.
    * @param elementName The name of the child element.
    * @return Does the element have a matching composite element?
    */
   private static boolean isComposite(final Element parent, final String elementName) {
      if(parent != null) {
         Elements elems = parent.getElementsByTag(elementName);
         return elems.size() > 0 && isComposite(elems.get(0));
      } else {
         return false;
      }
   }

   /**
    * Determine if an element is composite (has one or more child elements).
    * @param elem The element.
    * @return Is the element composite?
    */
   private static boolean isComposite(final Element elem) {
      return elem.children().size() > 0;
   }

   /**
    * Look for metadata as JSON content (like parsely).
    * @param doc The source document.
    * @param rootElem The root element to which metadata (converted to nodes) is added.
    */
   private static void jsonMetadata(final Document doc, final Element rootElem) {

      for(String pattern : jsonMetaPatterns) {
         Elements matchElements = doc.select(pattern);
         String src = firstAttribute(matchElements, "content");
         if(!src.isEmpty()) {
            StringReader reader = new StringReader(src);
            try {
               JSONToJsoup.parse(reader, rootElem, JSONToJsoup.NullBehavior.EMPTY);
            } catch(IOException ioe) {
               //Ignore
            }
         }
      }

      for(String pattern : jsonScriptPatterns) {
         Elements matchElements = doc.select(pattern);
         if(matchElements.size() > 0) {
            String src = matchElements.get(0).html();
            if(!src.isEmpty()) {
               StringReader reader = new StringReader(src);
               try {
                  JSONToJsoup.parse(reader, rootElem, JSONToJsoup.NullBehavior.EMPTY);
               } catch(IOException ioe) {
                  //Ignore
               }
            }
         }
      }
   }

   /**
    * Selectors for the title.
    */
   static final ImmutableList<String> titleMetaPatterns = ImmutableList.of(
           "meta[property=og:title]",
           "meta[name=twitter:title]",
           "meta[name=parsely-title]",
           "meta[name=sailthru.title]",
           "meta[name=title]",
           "meta[property=dc:title]"

   );

   /**
    * Selectors for the description.
    */
   static final ImmutableList<String> descriptionMetaPatterns = ImmutableList.of(
           "meta[name=twitter:description]",
           "meta[name=description]",
           "meta[itemprop=description]",
           "meta[property=og:description]",
           "meta[name=sailthru.description]"
   );

   /**
    * Selectors for the author.
    */
   static final ImmutableList<String> authorMetaPatterns = ImmutableList.of(
           "meta[name=Author]",
           "meta[name=author]",
           "meta[name=dc.creator]",
           "meta[itemprop=name]",
           "meta[property=author]",
           "meta[property=article:author]",
           "meta[property=article:authorName]",
           "meta[name=parsely-author]",
           "meta[name=sailthru.author]",
           "meta[name=twitter:creator]",
           "meta[property=byline]"
   );

   /**
    * Selectors for elements that may contain the author.
    */
   static final ImmutableList<String> authorElementPatterns = ImmutableList.of(
           "a[itemprop=author]",
           "span[itemprop=author]",
           "a[rel=author]",
           "span[property=dc:creator]",
           "div[property=dc:creator]"
   );

   /**
    * Selectors for the canonical link.
    */
   static final ImmutableList<String> linkPatterns = ImmutableList.of(
           "link[rel=canonical]"
   );

   /**
    * Selectors for meta elements that may contain the canonical link.
    */
   static final ImmutableList<String> linkMetaPatterns = ImmutableList.of(
           "meta[property=og:url]",
           "meta[name=twitter:url]",
           "meta[name=parsely-link]"
   );

   /**
    * Selectors for elements that may contain alternate links.
    */
   static final ImmutableList<String> altLinkPatterns = ImmutableList.of(
           "link[rel=shortlink]"
   );

   /**
    * Selectors for meta elements that may contain an image.
    */
   static final ImmutableList<String> imageMetaPatterns = ImmutableList.of(
           "meta[name=twitter:image]",
           "meta[name=twitter:image:src]",
           "meta[name=parsely-image-url]",
           "meta[name=thumbnail]",
           "meta[name=sailthru.image.thumb]"
   );

   /**
    * Selectors for meta elements that may contain the site name.
    */
   static final ImmutableList<String> siteNameMetaPatterns = ImmutableList.of(
           "meta[property=og:site_name]",
           "meta[name=og:site_name]",
           "meta[property=dc.publisher]"
   );

   /**
    * Selectors for meta elements that may contain the publish timestamp.
    */
   static final ImmutableList<String> publishTimeMetaPatterns = ImmutableList.of(
           "meta[property=article:published_time]",
           "meta[property=og:article:published_time]",
           "meta[property=pubDate]",
           "meta[name=parsely-pub-date]",
           "meta[itemprop=datePublished]",
           "meta[property=st:published_at]",
           "meta[name=publish-date]",
           "meta[name=publish_date]",
           "meta[property=og:updated_time]",
           "meta[name=ptime]",
           "meta[property=article:published]",
           "meta[name=sailthru.date]",
           "meta[name=date]",
           "meta[name=dcterms.date]",
           "meta[name=dc.date]"
   );

   /**
    * Selectors for elements that may have content that contain the publish timestamp.
    */
   static final ImmutableList<String> publishTimeElementPatterns = ImmutableList.of(
           "span[itemprop=datePublished]",
           "a[itemprop=datePublished]",
           "time[itemprop=datePublished]"
   );

   /**
    * Selectors for meta elements that may contain additional metadata as JSON.
    */
   static final ImmutableList<String> jsonMetaPatterns = ImmutableList.of(
           "meta[name=parsely-metadata]",
           "meta[name=parsely-page]",
           "meta[name=contextly-page]"
   );

   /**
    * Selectors for script elements that may contain additional metadata as JSON.
    */
   static final ImmutableList<String> jsonScriptPatterns = ImmutableList.of(
           "script[type=application/ld+json]",
           "script[type=json/pageinfo]"
   );

   /**
    * Attempt to parse a date/time.
    * @param dateTimeStr The date/time string.
    * @return The timestamp or {@code 0} if parse failed.
    */
   static final Date parseDateTime(final String dateTimeStr) {
      if(Strings.isNullOrEmpty(dateTimeStr)) {
         return null;
      }

      Long timestamp = tryParseISO8601(dateTimeStr);
      if(timestamp == null) {
         timestamp = tryParseRFC822(dateTimeStr);
      }
      return timestamp != null ? new Date(timestamp) : null;
   }
}