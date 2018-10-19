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

import com.attribyte.parser.model.Anchor;
import com.attribyte.parser.model.Audio;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Video;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jsoup.nodes.Document;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * Extract all links found in a document, including images.
 */
public class LinkExtractor {

   /**
    * Creates a link extractor.
    * @param sourceURL The source URL.
    */
   public LinkExtractor(final String sourceURL) {
      this(sourceURL, ImmutableSet.of());
   }

   /**
    * Creates a link extractor.
    * @param sourceURL The source URL.
    * @param skipLinks A set of links to skip.
    */
   public LinkExtractor(final String sourceURL,
                        final Collection<String> skipLinks) {
      this(sourceURL, skipLinks, s -> s);
   }

   /**
    * Creates a link extractor.
    * @param sourceURL The source URL.
    * @param skipLinks A collection of urls for links to skip.
    * @param canonicalizeFn A function that returns the canonical version of a URL.
    */
   public LinkExtractor(final String sourceURL,
                        final Collection<String> skipLinks,
                        final Function<String, String> canonicalizeFn) {
      this.canonicalizeFn = canonicalizeFn;
      seenSet = Sets.newHashSetWithExpectedSize(32);
      if(!Strings.isNullOrEmpty(sourceURL)) {
         seenSet.add(canonicalize(sourceURL));
      }
      if(skipLinks != null) {
         for(String link : skipLinks) {
            seenSet.add(canonicalize(link));
         }
      }
      defaultProtocol = Strings.nullToEmpty(sourceURL).startsWith("http:") ? "http:" : "https:";
   }

   /**
    * Extracts all links from a document.
    * @param doc The document
    * @return A self-reference.
    */
   public LinkExtractor extractLinks(final Document doc) {
      Set<Anchor> anchors = Sets.newHashSet();
      HTMLPageParser.anchors(doc, anchors);
      anchors.forEach(this::processAnchor);
      Set<Image> images = Sets.newHashSet();
      HTMLPageParser.images(doc, images);
      images.forEach(this::processImage);
      Set<Video> videos = Sets.newHashSet();
      HTMLPageParser.videos(doc, videos);
      videos.forEach(this::processVideo);
      Set<Audio> audio = Sets.newHashSet();
      HTMLPageParser.audio(doc, audio);
      audio.forEach(this::processAudio);
      doc.select("blockquote[cite], q[cite]").forEach(e-> {
         String href = e.attr("cite");
         String curl = canonicalize(href);
         if(!seenSet.contains(curl)) {
            links.add(addProtocol(href));
         }
      });
      return this;
   }

   /**
    * Adds an externally processed URL.
    * @param url The URL.
    * @return A self-reference.
    */
   public final LinkExtractor add(final String url) {
      String curl = canonicalize(url);
      if(!curl.isEmpty() && !seenSet.contains(curl)) {
         seenSet.add(curl);
         links.add(addProtocol(url));
      }
      return this;
   }

   /**
    * Adds a url to ignore.
    * @param url The URL to ignore.
    */
   public final void ignore(final String url) {
      String curl = canonicalize(url);
      if(!curl.isEmpty()) {
         seenSet.add(curl);
      }
   }

   /**
    * Processes an anchor element.
    * @param anchor The anchor.
    */
   private void processAnchor(final Anchor anchor) {
      String curl = canonicalize(anchor.href);
      if(!seenSet.contains(curl)) {
         anchors.add(anchor);
         seenSet.add(curl);
         links.add(addProtocol(anchor.href));
      }
   }

   /**
    * Processes an image.
    * @param image The image.
    */
   private void processImage(final Image image) {
      String curl = canonicalize(image.link);
      if(!seenSet.contains(curl)) {
         images.add(image);
         seenSet.add(curl);
         links.add(addProtocol(image.link));
      }
   }

   /**
    * Processes a video.
    * @param video The video.
    */
   private void processVideo(final Video video) {
      String curl = canonicalize(video.link);
      if(!seenSet.contains(curl)) {
         videos.add(video);
         seenSet.add(curl);
         links.add(addProtocol(video.link));
      }
   }

   /**
    * Processes audio.
    * @param audio The audio.
    */
   private void processAudio(final Audio audio) {
      String curl = canonicalize(audio.link);
      if(!seenSet.contains(curl)) {
         this.audio.add(audio);
         seenSet.add(curl);
         links.add(addProtocol(audio.link));
      }
   }

   /**
    * Gets all links.
    * @return The links.
    */
   public List<String> links() {
      return ImmutableList.copyOf(links);
   }

   /**
    * Gets all anchors.
    * @return The anchors.
    */
   public List<Anchor> anchors() {
      return ImmutableList.copyOf(anchors);
   }

   /**
    * Gets all images.
    * @return The images.
    */
   public List<Image> images() {
      return ImmutableList.copyOf(images);
   }

   /**
    * Gets all videos.
    * @return The videos.
    */
   public List<Video> videos() {
      return ImmutableList.copyOf(videos);
   }

   /**
    * Gets all audio.
    * @return The audio.
    */
   public List<Audio> audio() {
      return ImmutableList.copyOf(audio);
   }

   /**
    * Adds a protocol to the URL, if missing.
    * @param url The url.
    * @return The url with protocol added, if needed.
    */
   private String addProtocol(final String url) {
      if(Strings.isNullOrEmpty(url)) {
         return "";
      }

      if(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("mailto:")) {
         return url;
      } else if(url.startsWith("//")) {
         return defaultProtocol + url;
      } else {
         return defaultProtocol + "//" + url;
      }
   }

   /**
    * Canonicalize a URL with the supplied function.
    * @param url The URL.
    * @return The canonicalized URL.
    */
   private String canonicalize(final String url) {
      return Strings.nullToEmpty(canonicalizeFn.apply(addProtocol(url)));
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("defaultProtocol", defaultProtocol)
              .add("links", links)
              .add("anchors", anchors)
              .add("images", images)
              .add("videos", videos)
              .add("audio", audio)
              .toString();
   }

   /**
    * The default protocol used for links that start with {@code //} or have no protocol.
    */
   public final String defaultProtocol;

   /**
    * The function used to canonicalize links.
    */
   private final Function<String, String> canonicalizeFn;

   /**
    * The set of (canonicalized) links already processed.
    */
   private final Set<String> seenSet;

   /**
    * All discovered links, from any source.
    */
   private final Set<String> links = Sets.newHashSet();

   /**
    * The discovered anchors.
    */
   private final List<Anchor> anchors = Lists.newArrayList();

   /**
    * The discovered images.
    */
   private final List<Image> images = Lists.newLinkedList();

   /**
    * The discovered videos.
    */
   private final List<Video> videos = Lists.newLinkedList();

   /**
    * The discovered audio.
    */
   private final List<Audio> audio = Lists.newLinkedList();
}