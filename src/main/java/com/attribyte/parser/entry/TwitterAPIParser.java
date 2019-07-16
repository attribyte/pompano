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
import com.attribyte.parser.model.Aspect;
import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Link;
import com.attribyte.parser.model.Resource;
import com.attribyte.parser.model.Video;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parse a Twitter JSON document to create an entry.
 */
public class TwitterAPIParser implements com.attribyte.parser.Parser {

   /**
    * The source property in which the maximum seen id is saved.
    */
   public static String MAX_ID_META = "maxId";

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
         long maxId = 0L;
         if(!root.isArray()) {
            resource.addEntry(parseEntry(root, contentCleaner).build());
            maxId = longValue(root, "id").orElse(0L);
         } else {
            for(JsonNode entryNode : root) {
               resource.addEntry(parseEntry(entryNode, contentCleaner).build());
               long id = longValue(entryNode, "id").orElse(0L);
               if(id > maxId) {
                  maxId = id;
               }
            }
         }

         resource.setMetadata(MAX_ID_META, Long.toString(maxId));
         resource.setSourceLink(sourceLink);
         return new ParseResult(name(), resource.build());

      } catch(Error e) {
         throw e;
      } catch(Throwable t) {
         t.printStackTrace();
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
      String contentText = textValue(contentNode, "full_text")
              .orElse(textValue(contentNode, "text")
                      .orElse("")
              );

      Map<String, String> replaceText = Maps.newHashMapWithExpectedSize(4);
      Map<String, Image> images = Maps.newHashMapWithExpectedSize(4);
      Map<String, Video> videos = Maps.newHashMapWithExpectedSize(4);
      LinkedHashSet<String> citations = Sets.newLinkedHashSetWithExpectedSize(8);

      path(contentNode, "entities").ifPresent(entitiesNode -> {
         path(entitiesNode, "hashtags").ifPresent(hashtagsNode -> {
            hashtagsNode.forEach(tagNode -> {
               textValue(tagNode, "text").ifPresent(tag -> {
                  entry.addTag(tag);
                  replaceText.put("#" + tag, hashtagMarkup(tag));
                  replaceText.put("#" + tag.toLowerCase(), hashtagMarkup(tag));
               });
            });
         });

         path(entitiesNode, "user_mentions").ifPresent(mentionsNode -> {
            mentionsNode.forEach(mentionNode -> {
               textValue(mentionNode, "screen_name").ifPresent(name -> {
                  replaceText.put("@" + name, mentionMarkup(name));
                  replaceText.put("@" + name.toLowerCase(), mentionMarkup(name));
               });
            });
         });

         path(entitiesNode, "urls").ifPresent(urlsNode -> {
            urlsNode.forEach(urlNode -> {
               String url = textValue(urlNode, "url").orElse("");
               if(!url.isEmpty()) {
                  citations.add(url);
                  String display = textValue(urlNode, "display_url").orElse("");
                  String expanded = textValue(urlNode, "expanded_url").orElse("");
                  if(!expanded.isEmpty()) {
                     citations.add(expanded);
                     if(!display.isEmpty()) {
                        replaceText.put(url, linkMarkup(expanded, display));
                     }
                  }
               }
            });
         });
      });

      List<JsonNode> mediaEntities = Lists.newArrayListWithExpectedSize(2);
      path(entryNode, "entities").ifPresent(mediaEntities::add);
      path(entryNode, "extended_entities").ifPresent(mediaEntities::add);

      for(JsonNode entityNode : mediaEntities) {
         path(entityNode, "media").ifPresent(mediaArray -> {
            for(JsonNode mediaNode : mediaArray) {
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
                        Video video = buildVideo(mediaNode);
                        if(video != null) {
                           videos.put(url, video);
                        }
                        break;
                     default:
                        break;
                  }
               }
            }
         });
      }

      path(entryNode, "user").ifPresent(userNode -> {
         textValue(userNode, "screen_name").ifPresent(name -> {
            Author.Builder author = Author.builder(name);
            textValue(userNode, "name").ifPresent(author::setDisplayName);
            textValue(userNode, "url").ifPresent(author::setLink);
            textValue(userNode, "id_str").ifPresent(author::setId);
            textValue(userNode, "description").ifPresent(author::setDescription);
            textValue(userNode, "profile_image_url_https").ifPresent(url -> {
               author.setImage(Image.builder(url).build());
            });
            entry.addAuthor(author.build());
         });
      });


      images.values().forEach(entry::addImage);
      videos.values().forEach(entry::addVideo);

      for(Map.Entry<String, String> kv : replaceText.entrySet()) {
         contentText = contentText.replace(kv.getKey(), kv.getValue());
      }


      Document doc = Jsoup.parse(contentText, "", org.jsoup.parser.Parser.htmlParser());
      for(Element anchor : doc.body().select("a[href]")) {
         String href = anchor.attr("href");
         if(href.startsWith("https://") || href.startsWith("http://")) {
            citations.add(href);
         }
      }

      path(entryNode, "quoted_status").ifPresent(quotedStatusNode -> {
         Entry quotedEntry = quotedEntry(doc, quotedStatusNode, contentCleaner);
         if(!Strings.isNullOrEmpty(quotedEntry.canonicalLink)) {
            citations.add(quotedEntry.canonicalLink);
         }
      });

      path(entryNode, "retweeted_status").ifPresent(quotedStatusNode -> {
         Entry quotedEntry = quotedEntry(doc, quotedStatusNode, contentCleaner);
         if(!Strings.isNullOrEmpty(quotedEntry.canonicalLink)) {
            citations.add(quotedEntry.canonicalLink);
         }
      });

      entry.setOriginalContent(doc);
      setCanonicalLink(entry);
      if(contentCleaner != null) {
         entry.setCleanContent(contentCleaner.toCleanContent(contentCleaner.transform(doc)));
      }

      citations.forEach(citation -> entry.addCitation(new Link(citation)));

      return entry;
   }

   private Entry quotedEntry(final Document doc, final JsonNode quotedStatusNode, final ContentCleaner contentCleaner) {
      Entry quotedEntry = parseEntry(quotedStatusNode, contentCleaner).build();
      quotedEntry.originalContent().ifPresent(quotedDoc -> {
         Element blockquote = doc.body().appendElement("blockquote");
         blockquote.attr("cite",  quotedEntry.canonicalLink);
         quotedDoc.body().childNodes().forEach(node -> {
            blockquote.appendChild(node.clone());
         });
      });
      return quotedEntry;
   }

   private Video buildVideo(final JsonNode mediaNode) {

      String imageURL = textValue(mediaNode, "media_url_https").orElse("");
      Image image = Image.builder(imageURL).build();
      List<Video> variants = Lists.newArrayListWithExpectedSize(4);
      String id = textValue(mediaNode, "id_str").orElse("");

      path(mediaNode, "video_info").ifPresent(infoNode -> {

         Optional<Aspect> aspect = path(infoNode, "aspect_ratio").flatMap(ratioNode -> {
            if(ratioNode.size() == 2) {
               return Optional.of(new Aspect(ratioNode.get(0).asInt(), ratioNode.get(1).asInt()));
            } else {
               return Optional.empty();
            }
         });

         long durationMillis = longValue(infoNode, "duration_millis").orElse(0L);

         path(infoNode, "variants").ifPresent(variantNodes -> {
            for(JsonNode variantNode : variantNodes) {
               textValue(variantNode, "url").ifPresent(variantURL -> {
                  Video.Builder variant = Video.builder(variantURL);
                  variant.setId(id);
                  aspect.ifPresent(variant::setAspect);
                  variant.setDurationMillis(durationMillis);
                  variant.setImage(image);
                  intValue(variantNode, "bitrate").ifPresent(variant::setBitrate);
                  textValue(variantNode, "content_type").ifPresent(variant::setMediaType);
                  variants.add(variant.build());
               });
            }
         });
      });

      if(!variants.isEmpty()) {
         return variants.get(0).withVariants(variants.subList(1, variants.size()));
      } else {
         return null;
      }
   }

   private static final String CANONICAL_LINK_TEMPLATE = "https://twitter.com/%s/status/%s";
   private static final String CANONICAL_LINK_NO_NAME_TEMPLATE = "https://twitter.com/i/web/status/%s";

   /**
    * Creates the canonical link for an entry.
    * @return The canonical link.
    */
   private void setCanonicalLink(final Entry.Builder entry) {
      List<Author> authors = entry.getAuthors();
      String screenName = authors.isEmpty() ? "" : Strings.nullToEmpty(authors.get(0).name);
      if(screenName.isEmpty()) {
         entry.setCanonicalLink(String.format(CANONICAL_LINK_NO_NAME_TEMPLATE, entry.getId()));
      } else {
         entry.setCanonicalLink(String.format(CANONICAL_LINK_TEMPLATE, screenName, entry.getId()));
      }
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

   /**
    * Gets the long value for a path, if present.
    * @param entryNode The entry node.
    * @param path The path.
    * @return The long value or {@code empty}.
    */
   private Optional<Long> longValue(final JsonNode entryNode, final String path) {
      return path(entryNode, path).flatMap(node -> node.canConvertToLong() ? Optional.of(node.longValue()) : Optional.empty());
   }

   /**
    * Gets the int value for a path, if present.
    * @param entryNode The entry node.
    * @param path The path.
    * @return The int value or {@code empty}.
    */
   private Optional<Integer> intValue(final JsonNode entryNode, final String path) {
      return path(entryNode, path).flatMap(node -> node.canConvertToInt() ? Optional.of(node.intValue()) : Optional.empty());
   }

   /**
    * Parse the twitter time string.
    * @param str The string.
    * @return The timestamp.
    */
   private static long parseTwitterTime(final String str) {
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