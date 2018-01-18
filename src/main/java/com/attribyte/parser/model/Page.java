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

package com.attribyte.parser.model;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.net.InternetDomainName;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An immutable HTML page.
 * @author Matt Hamer
 */
public class Page {

   /**
    * Creates a page.
    * @param doc The parsed document.
    * @param canonicalLink The canonical link.
    * @param selfLinks A collection of self-links.
    * @param siteName The site name.
    * @param title The title.
    * @param summary The summary.
    * @param author The author.
    * @param publishTime The publish time.
    * @param anchors A collection of anchors.
    * @param images A collection of images.
    * @param metaImages A collection of images found in metadata.
    * @param videos A collection of videos.
    * @param metaVideos A collection of videos found in metadata.
    * @param audios A collection of audio streams.
    * @param metaAudios A collection of audio streams found in metadata.
    * @param links A collection of links.
    */
   public Page(final Document doc,
               final String canonicalLink,
               final Collection<Link> selfLinks,
               final String siteName,
               final String title,
               final String summary,
               final String author,
               final Date publishTime,
               final Collection<Anchor> anchors,
               final Collection<Image> images,
               final Collection<Image> metaImages,
               final Collection<Video> videos,
               final Collection<Video> metaVideos,
               final Collection<Audio> audios,
               final Collection<Audio> metaAudios,
               final Collection<Link> links) {
      this.canonicalLink = Strings.nullToEmpty(canonicalLink);
      this.selfLinks = selfLinks != null ? ImmutableSet.copyOf(selfLinks) : ImmutableSet.<Link>of();
      this.siteName = Strings.nullToEmpty(siteName).trim();
      this.title = Strings.nullToEmpty(title).trim();
      this.summary = Strings.nullToEmpty(summary).trim();
      this.author = Strings.nullToEmpty(author).trim();
      this.publishTime = publishTime;
      this.anchors = anchors != null ? ImmutableList.copyOf(anchors) : ImmutableList.<Anchor>of();
      this.images = images != null ? ImmutableList.copyOf(images) : ImmutableList.<Image>of();
      this.metaImages = metaImages != null ? ImmutableList.copyOf(metaImages) : ImmutableList.<Image>of();
      this.videos = videos != null ? ImmutableList.copyOf(videos) : ImmutableList.<Video>of();
      this.metaVideos = metaVideos != null ? ImmutableList.copyOf(metaVideos) : ImmutableList.<Video>of();
      this.audios = audios != null ? ImmutableList.copyOf(audios) : ImmutableList.<Audio>of();
      this.metaAudios = metaAudios != null ? ImmutableList.copyOf(metaAudios) : ImmutableList.<Audio>of();
      this.links = links != null ? ImmutableList.copyOf(links) : ImmutableList.<Link>of();
   }

   /**
    * Write debug output.
    * @param debugOutputDir The output directory.
    * @throws IOException On write error.
    */
   public void writeDebug(final File debugOutputDir) throws IOException {

      try {
         URL url = new URL(canonicalLink);
         String host = Strings.nullToEmpty(url.getHost()).trim();
         String file = Hashing.sha256().hashString(canonicalLink, Charsets.UTF_8).toString().substring(0, 16);
         File outputFile = new File(debugOutputDir, host + "/" + file);
         System.out.println("Writing to " + outputFile.getAbsolutePath());
         if(!outputFile.getParentFile().mkdirs()) {
            throw new IOException("Unable to create host directory");
         }
         Files.write(toString().getBytes(Charsets.UTF_8), outputFile);
         System.out.println("Wrote debug to " + outputFile.getAbsolutePath());
      } catch(IOException ioe) {
         throw ioe;
      } catch(Throwable t) {
         throw new IOException("Invalid URL", t);
      }
   }

   /**
    * Gets a list of links with specified relation and media type.
    * @param rel The relation. May be {@code null} to match any.
    * @param type The media type. May be {@code null} to match any.
    * @return The list of links.
    */
   public List<Link> links(final String rel, final String type) {
      return rel == null && type == null ? links : links.stream().filter(link -> link.matchType(rel, type)).collect(Collectors.toList());
   }

   /**
    * Gets a list of all anchors that link to external sites in the order they appear on the page.
    * @return The list of external anchors.
    */
   public List<Anchor> externalAnchors() {

      try {
         String host = new URL(canonicalLink).getHost();
         if(InternetDomainName.isValid(host)) {
            InternetDomainName pageDomain = InternetDomainName.from(host);
            return anchors.stream().filter(anchor -> !anchor.matchesDomain(pageDomain)).collect(Collectors.toList());
         } else {
            return anchors;
         }
      } catch(MalformedURLException | IllegalStateException e) {
         return anchors;
      }
   }

   /**
    * Gets a list of potential feed links.
    * <p>
    *    Links with known feed 'type' attributes are added first in the order they apper.
    *    Links with 'alternate' as the relationship are added next.
    * </p>
    * @return The list of feed links.
    */
   public List<Link> feedLinks() {
      List<Link> feedLinks = Lists.newArrayListWithExpectedSize(8);
      feedLinks.addAll(links.stream().filter(link -> feedTypes.contains(link.type)).collect(Collectors.toList()));
      feedLinks.addAll(links.stream().filter(link -> link.rel.equalsIgnoreCase("alternate") && altFeedTypes.contains(link.type) && !feedTypes.contains(link.type)).collect(Collectors.toList()));
      return feedLinks;
   }

   /**
    * Gets a list of icon links.
    * @return The list if icon links in the order they appear.
    */
   public List<Link> iconLinks() {
      List<Link> feedLinks = Lists.newArrayListWithExpectedSize(8);
      feedLinks.addAll(links.stream().filter(link -> link.matchType("icon", null)).collect(Collectors.toList()));
      feedLinks.addAll(links.stream().filter(link -> link.matchType("shortcut icon", null)).collect(Collectors.toList()));
      return feedLinks;
   }

   /**
    * The canonical link to this page.
    */
   public final String canonicalLink;

   /**
    * A set of all 'self' links discovered in the document, including
    * the canonical link.
    */
   public final ImmutableSet<Link> selfLinks;

   /**
    * The name of the site for this page.
    */
   public final String siteName;

   /**
    * The page title.
    */
   public final String title;

   /**
    * A summary.
    */
   public final String summary;

   /**
    * The author name.
    */
   public final String author;

   /**
    * The publish time.
    */
   public final Date publishTime;

   /**
    * All unique anchors found in the document.
    */
   public final ImmutableList<Anchor> anchors;

   /**
    * All unique images found in the metadata of the document.
    */
   public final ImmutableList<Image> metaImages;

   /**
    * All unique images found in the document.
    */
   public final ImmutableList<Image> images;

   /**
    * All unique links found in the document.
    */
   public final ImmutableList<Link> links;

   /**
    * All videos found in the metadata of the document.
    */
   public final ImmutableList<Video> metaVideos;

   /**
    * All videos found in the document.
    */
   public final ImmutableList<Video> videos;

   /**
    * All audio streams found in the metadata of the document.
    */
   public final ImmutableList<Audio> metaAudios;

   /**
    * All audio streams found in the document.
    */
   public final ImmutableList<Audio> audios;

   /**
    * The system newline string.
    */
   private static final String NEWLINE = System.getProperty("line.separator");

   /**
    * The values of link 'type' attribute recognized as feeds.
    */
   public static final ImmutableSet<String> feedTypes = ImmutableSet.of(
     "application/atom+xml",
     "application/rss+xml"
   );

   public static final ImmutableSet<String> altFeedTypes = ImmutableSet.of(
     "text/xml"
   );
}
