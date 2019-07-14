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
import com.attribyte.parser.model.Video;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parse a Twitter JSON document to create an entry.
 */
public class TwitterAPIParser implements com.attribyte.parser.Parser {

   @Override
   public String name() {
      return "twitter";
   }

   @Override
   public ParseResult parse(final String content, final String sourceLink, final ContentCleaner contentCleaner) {

      try {
         Resource.Builder resource = Resource.builder();
         resource.setSourceLink(sourceLink);
         JsonNode root = jsonReader.readTree(content);
         if(!root.isArray()) {
            resource.addEntry(parseEntry(root, contentCleaner).build());
         } else {
            root.forEach(entryNode -> {
               resource.addEntry(parseEntry(entryNode, contentCleaner).build());
            });
         }
         return new ParseResult(name(), resource.build());

      } catch(Error e) {
         throw e;
      } catch(Throwable t) {
         return new ParseResult(name(), new ParseError("Twitter Parser Failure", t));
      }
   }

   private Entry.Builder parseEntry(final JsonNode entryNode, final ContentCleaner contentCleaner) {
      Entry.Builder entry = Entry.builder();
      textValue(entryNode, "id_str").ifPresent(entry::setId);
      textValue(entryNode, "created_at").ifPresent(val -> {
         entry.setPublishedTimestamp(parseTwitterTime(val));
      });

      JsonNode contentNode = path(entryNode, "extended_tweet").orElse(entryNode);

      String contentText = (contentNode == entryNode ?
              textValue(contentNode, "text") :
              textValue(contentNode, "full_text")).orElse("");

      Map<String, String> replaceText = Maps.newHashMapWithExpectedSize(4);
      Map<String, Image> images = Maps.newHashMapWithExpectedSize(4);
      Map<String, Video> videos = Maps.newHashMapWithExpectedSize(4);

      path(contentNode, "entities").ifPresent(entitiesNode -> {

         path(entitiesNode, "hashtags").ifPresent(hashtagsNode -> {
            hashtagsNode.forEach(tagNode -> {
               textValue(tagNode, "text").ifPresent(tag -> {
                  entry.addTag(tag);
                  replaceText.put("#" + tag, hashtagMarkup(tag));
               });
            });
         });

         path(entitiesNode, "media").ifPresent(mediaNode -> {
            String type = textValue(mediaNode, "type").orElse("");
            String url = textValue(mediaNode, "media_url_https").orElse("");
            String content_url = textValue(mediaNode, "url").orElse("");

            if(!url.isEmpty()) {
               switch(type) {
                  case "photo":
                  case "animated_gif":
                     images.put(url, Image.builder(url).build());
                     replaceText.put(content_url, imageMarkup(url));
                     break;
                  case "video":
                     videos.put(url, Video.builder(url).build());
                     break;
                  default: break;
               }
            }

         });

         path(entitiesNode, "user_mentions").ifPresent(mentionsNode -> {
            mentionsNode.forEach(mentionNode -> {
               textValue(mentionNode, "screen_name").ifPresent(name -> {
                  replaceText.put("@" + name, mentionMarkup(name));
               });
            });

         });

         path(entitiesNode, "urls").ifPresent(urlsNode -> {
            urlsNode.forEach(urlNode -> {
               String url = textValue(urlNode, "url").orElse("");
               String display = textValue(urlNode, "display_url").orElse("");
               String expanded = textValue(urlNode, "expanded_url").orElse("");
               if(!url.isEmpty() && !display.isEmpty() && !expanded.isEmpty()) {
                  replaceText.put(url, linkMarkup(expanded, display));
               }
            });
         });
      });

      path(entryNode, "user").ifPresent(userNode -> {
         textValue(userNode, "screen_name").ifPresent(name -> {
            Author.Builder author = Author.builder(name);
            textValue(userNode, "name").ifPresent(author::setDisplayName);
            textValue(userNode, "url").ifPresent(author::setLink);
            textValue(userNode, "id_str").ifPresent(author::setId);
            textValue(userNode, "description").ifPresent(author::setDescription);
            entry.addAuthor(author.build());
         });
      });


      images.values().forEach(entry::addImage);
      videos.values().forEach(entry::addVideo);

      for(Map.Entry<String, String> kv : replaceText.entrySet()) {
         contentText = contentText.replace(kv.getKey(), kv.getValue());
      }

      Document doc = Jsoup.parse(contentText, "", org.jsoup.parser.Parser.htmlParser());
      entry.setOriginalContent(doc);
      if(contentCleaner != null) {
         entry.setCleanContent(contentCleaner.toCleanContent(contentCleaner.transform(doc)));
      }

      return entry;
   }

   private static final String ANCHOR_TEMPLATE = "<a href=\"%s\">%s</a>";

   /**
    * Creates a link as markup.
    * @param url The URL.
    * @param display The content to display.
    * @return The content with link replaced.
    */
   private String linkMarkup(final String url, final String display) {
      return String.format(ANCHOR_TEMPLATE, url, display);
   }


   private static final String MENTION_TEMPLATE = "<a href=\"https://twitter.com/%s\">@%s</a>";

   /**
    * Creates an '@' mention as a markup link.
    * @param screenName The screen name.
    * @return The markup.
    */
   private String mentionMarkup(final String screenName) {
      return String.format(MENTION_TEMPLATE, screenName, screenName);
   }

   private static final String HASHTAG_TEMPLATE = "<a href=\"https://twitter.com/hashtag/%s\">#%s</a>";

   /**
    * Creates a hashtag markup link.
    * @param tag The tag.
    * @return The markup.
    */
   private String hashtagMarkup(final String tag) {
      return String.format(HASHTAG_TEMPLATE, tag, tag);
   }


   private static final String IMAGE_TEMPLATE = "<img src=\"%s\"/>";

   /**
    * Creates a marked-up image.
    * @param src The image source.
    * @return The markup.
    */
   private String imageMarkup(final String src) {
      return String.format(IMAGE_TEMPLATE, src);
   }

   /**
    * Gets the node at a path, if present.
    * @param entryNode The entry node.
    * @param path The path.
    * @return The node or {@code empty}.
    */
   private Optional<JsonNode> path(final JsonNode entryNode, final String path) {
      JsonNode node = entryNode.path(path);
      return node.isMissingNode() ? Optional.empty() : Optional.of(node);
   }

   /**
    * Gets the text value for a path, if present.
    * @param entryNode The entry node.
    * @param path The path.
    * @return The text value or {@code empty}.
    */
   private Optional<String> textValue(final JsonNode entryNode, final String path) {
      return path(entryNode, path).map(JsonNode::textValue);
   }


   static long parseTwitterTime(final String str) {
      try {
         return !Strings.isNullOrEmpty(str) ? TWITTER_FORMAT.parseMillis(str) : System.currentTimeMillis();
      } catch(Exception e) {
         return System.currentTimeMillis();
      }
   }

   //Wed Nov 18 21:45:12 +0000 2009

   private static final DateTimeFormatter TWITTER_FORMAT = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss Z yyyy").withLocale(Locale.ENGLISH);

   /**
    * The JSON reader.
    */
   private final ObjectReader jsonReader = new ObjectMapper().reader();
}