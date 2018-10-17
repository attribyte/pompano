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



import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Optional;

/**
 * An immutable entry.
 * @author Matt Hamer
 */
public class Entry {

   public static class Builder {

      /**
       * Gets the id.
       * @return the id.
       */
      public String getId() {
         return id;
      }

      /**
       * Sets the id.
       * @param id The id.
       * @return A self-reference.
       */
      public Builder setId(final String id) {
         this.id = id;
         return this;
      }

      /**
       * Gets the title.
       * @return The title.
       */
      public String getTitle() {
         return title;
      }

      /**
       * Sets the title.
       * @param title The title.
       * @return A self-reference.
       */
      public Builder setTitle(final String title) {
         this.title = title;
         return this;
      }

      /**
       * Gets the summary.
       * @return The summary.
       */
      public String getSummary() {
         return summary;
      }

      /**
       * Sets the summary.
       * @param summary The summary.
       * @return A self-reference.
       */
      public Builder setSummary(final String summary) {
         this.summary = summary;
         return this;
      }

      /**
       * Gets the clean content.
       * @return The clean content.
       */
      public String getCleanContent() {
         return cleanContent;
      }

      /**
       * Sets the clean content.
       * @param cleanContent The clean content.
       * @return A self-reference.
       */
      public Builder setCleanContent(final String cleanContent) {
         this.cleanContent = cleanContent;
         return this;
      }

      /**
       * Gets the (parsed) original content.
       * @return The original content as a {@code body} element.
       */
      public Document getOriginalContent() {
         return originalContent;
      }

      /**
       * Sets the (parsed) original content.
       * @param originalContent The original content.
       * @return A self-reference.
       */
      public Builder setOriginalContent(final Document originalContent) {
         this.originalContent = originalContent;
         return this;
      }

      /**
       * Gets the canonical link.
       * @return The canonical link.
       */
      public String getCanonicalLink() {
         return canonicalLink;
      }

      /**
       * Sets the canonical link.
       * @param canonicalLink The canonical link.
       * @return A self-reference.
       */
      public Builder setCanonicalLink(final String canonicalLink) {
         this.canonicalLink = canonicalLink;
         return this;
      }

      /**
       * Gets an immutable list of alt links.
       * @return The list of alt links.
       */
      public ImmutableList<String> getAltLinks() {
         return altLinks != null ? ImmutableList.copyOf(altLinks) : ImmutableList.of();
      }

      /**
       * Sets the list of alt links.
       * @param altLinks The list of alt links.
       * @return A self-reference.
       */
      public Builder setAltLinks(final List<String> altLinks) {
         if(altLinks != null) {
            this.altLinks = Lists.newArrayList(altLinks);
         }
         return this;
      }

      /**
       * Adds an alternate link.
       * @param altLink The alternate link to add.
       * @return A self-reference.
       */
      public Builder addAltLink(final String altLink) {
         if(altLinks == null) {
            altLinks = Lists.newArrayListWithExpectedSize(4);
         }
         altLinks.add(altLink);
         return this;
      }

      /**
       * Gets the published timestamp.
       * @return The published timestamp.
       */
      public long getPublishedTimestamp() {
         return publishedTimestamp;
      }

      /**
       * Sets the published timestamp.
       * @param publishedTimestamp The published timestamp.
       * @return A self-reference.
       */
      public Builder setPublishedTimestamp(final long publishedTimestamp) {
         this.publishedTimestamp = publishedTimestamp;
         return this;
      }

      /**
       * Gets the updated timestamp.
       * @return The updated timestamp.
       */
      public long getUpdatedTimestamp() {
         return updatedTimestamp;
      }

      /**
       * Sets the updated timestamp.
       * @param updatedTimestamp The updated timestamp.
       * @return A self-reference.
       */
      public Builder setUpdatedTimestamp(final long updatedTimestamp) {
         this.updatedTimestamp = updatedTimestamp;
         return this;
      }

      /**
       * Gets an immutable list of authors.
       * @return The list of authors.
       */
      public ImmutableList<Author> getAuthors() {
         return authors != null ? ImmutableList.copyOf(authors) : ImmutableList.of();
      }

      /**
       * Sets the list of authors.
       * @param authors The list of authors.
       * @return A self-reference.
       */
      public Builder setAuthors(final List<Author> authors) {
         if(authors != null) {
            this.authors = Lists.newArrayList(authors);
         }
         return this;
      }

      /**
       * Adds an author.
       * @param author The author to add.
       * @return A self-reference.
       */
      public Builder addAuthor(final Author author) {
         if(authors == null) {
            authors = Lists.newArrayListWithExpectedSize(4);
         }
         authors.add(author);
         return this;
      }

      /**
       * Gets an immutable list of images.
       * @return The list of images.
       */
      public ImmutableList<Image> getImages() {
         return images != null ? ImmutableList.copyOf(images) : ImmutableList.of();
      }

      /**
       * Sets the list of images.
       * @param images The list of images.
       * @return A self-reference.
       */
      public Builder setImages(final List<Image> images) {
         if(images != null) {
            this.images = Lists.newArrayList(images);
         }
         return this;
      }

      /**
       * Adds an image.
       * @param image The image to add.
       * @return A self-reference.
       */
      public Builder addImage(final Image image) {
         if(images == null) {
            images = Lists.newArrayListWithExpectedSize(4);
         }
         images.add(image);
         return this;
      }

      /**
       * Gets an immutable list of videos.
       * @return The list of videos.
       */
      public ImmutableList<Video> getVideos() {
         return videos != null ? ImmutableList.copyOf(videos) : ImmutableList.of();
      }

      /**
       * Sets the list of videos.
       * @param videos The list of videos.
       * @return A self-reference.
       */
      public Builder setVideos(final List<Video> videos) {
         if(videos != null) {
            this.videos = Lists.newArrayList(videos);
         }
         return this;
      }

      /**
       * Adds a video.
       * @param video The video to add.
       * @return A self-reference.
       */
      public Builder addVideo(final Video video) {
         if(videos == null) {
            videos = Lists.newArrayListWithExpectedSize(4);
         }
         videos.add(video);
         return this;
      }

      /**
       * Gets an immutable list of tags.
       * @return The list of tags.
       */
      public ImmutableList<String> getTags() {
         return tags != null ? ImmutableList.copyOf(tags) : ImmutableList.of();
      }

      /**
       * Sets the list of tags.
       * @param tags The list of tags.
       * @return A self-reference.
       */
      public Builder setTags(final List<String> tags) {
         if(tags != null) {
            this.tags = Lists.newArrayList(tags);
         }
         return this;
      }

      /**
       * Adds a tag.
       * @param tag The tag to add.
       * @return A self-reference.
       */
      public Builder addTag(final String tag) {
         if(tags == null) {
            tags = Lists.newArrayListWithExpectedSize(4);
         }
         tags.add(tag);
         return this;
      }

      /**
       * Gets the rights.
       * @return The rigts.
       */
      public String getRights() {
         return rights;
      }

      /**
       * Sets the rights.
       * @param rights The rights.
       * @return A self-reference.
       */
      public Builder setRights(final String rights) {
         this.rights = rights;
         return this;
      }

      /**
       * Gets the primary image.
       * @return The primary image.
       */
      public Image getPrimaryImage() {
         return primaryImage;
      }

      /**
       * Sets the primary image.
       * @param primaryImage The primary image.
       * @return A self-reference.
       */
      public Builder setPrimaryImage(final Image primaryImage) {
         this.primaryImage = primaryImage;
         return this;
      }

      /**
       * Gets the primary video.
       * @return The primary video.
       */
      public Video getPrimaryVideo() {
         return primaryVideo;
      }

      /**
       * Sets the primary video.
       * @param primaryVideo The primary video.
       * @return A self-reference.
       */
      public Builder setPrimaryVideo(final Video primaryVideo) {
         this.primaryVideo = primaryVideo;
         return this;
      }

      /**
       * Gets an immutable list of audio streams.
       * @return The list of audio streams.
       */
      public ImmutableList<Audio> getAudios() {
         return audios != null ? ImmutableList.copyOf(audios) : ImmutableList.of();
      }

      /**
       * Sets the list of audio streams.
       * @param audios The list of audio streams.
       * @return A self-reference.
       */
      public Builder setAudios(final List<Audio> audios) {
         if(audios != null) {
            this.audios = Lists.newArrayList(audios);
         }
         return this;
      }

      /**
       * Adds an audio stream.
       * @param audio The audio to add.
       * @return A self-reference.
       */
      public Builder addAudio(final Audio audio) {
         if(audios == null) {
            audios = Lists.newArrayListWithExpectedSize(4);
         }
         audios.add(audio);
         return this;
      }

      /**
       * Gets the primary audio stream.
       * @return The primary audio stream.
       */
      public Audio getPrimaryAudio() {
         return primaryAudio;
      }

      /**
       * Sets the primary audio stream.
       * @param primaryAudio The primary audio stream.
       * @return A self-reference.
       */
      public Builder setPrimaryAudio(final Audio primaryAudio) {
         this.primaryAudio = primaryAudio;
         return this;
      }

      /**
       * Gets an immutable list citations.
       * @return The list of citations.
       */
      public ImmutableList<Link> getCitations() {
         return citations != null ? ImmutableList.copyOf(citations) : ImmutableList.of();
      }

      /**
       * Sets the list of citations.
       * @param citations The list of citations.
       * @return A self-reference.
       */
      public Builder setCitations(final List<Link> citations) {
         if(citations != null) {
            this.citations = Lists.newArrayList(citations);
         }
         return this;
      }

      /**
       * Adds a citation.
       * @param citation The citation to add.
       * @return A self-reference.
       */
      public Builder addCitation(final Link citation) {
         if(citations == null) {
            citations = Lists.newArrayListWithExpectedSize(4);
         }
         citations.add(citation);
         return this;
      }

      /**
       * Builds an immutable entry.
       * @return The immutable entry.
       */
      public Entry build() {
         return new Entry(id, title, summary, cleanContent, canonicalLink, altLinks,
                 publishedTimestamp, updatedTimestamp, authors,
                 primaryImage, images, primaryVideo, videos,
                 primaryAudio, audios,
                 tags, rights, originalContent, citations);
      }

      private String id;
      private String title;
      private String summary;
      private String cleanContent;
      private Document originalContent;
      private String canonicalLink;
      private List<String> altLinks;
      private long publishedTimestamp;
      private long updatedTimestamp;
      private List<Author> authors;
      private Image primaryImage;
      private List<Image> images;
      private Video primaryVideo;
      private List<Video> videos;
      private Audio primaryAudio;
      private List<Audio> audios;
      private List<String> tags;
      private String rights;
      private List<Link> citations;
   }

   private Entry(final String id, final String title, final String summary, final String cleanContent,
                 final String canonicalLink,
                 final List<String> altLinks,
                 final long publishedTimestamp, final long updatedTimestamp,
                 final List<Author> authors,
                 final Image primaryImage,
                 final List<Image> images,
                 final Video primaryVideo,
                 final List<Video> videos,
                 final Audio primaryAudio,
                 final List<Audio> audios,
                 final List<String> tags,
                 final String rights, final Document originalContent,
                 final List<Link> citations) {
      this.id = Strings.nullToEmpty(id);
      this.title = Strings.nullToEmpty(title);
      this.summary = Strings.nullToEmpty(summary);
      this.cleanContent = Strings.nullToEmpty(cleanContent);
      this.canonicalLink = Strings.nullToEmpty(canonicalLink);
      this.altLinks = altLinks == null ? ImmutableList.of() : ImmutableList.copyOf(altLinks);
      this.publishedTimestamp = publishedTimestamp;
      this.updatedTimestamp = updatedTimestamp;
      this.authors = authors == null ? ImmutableList.of() : ImmutableList.copyOf(authors);
      this.primaryImage = primaryImage == null ? Optional.empty() : Optional.of(primaryImage);
      this.images = images == null ? ImmutableList.of() : ImmutableList.copyOf(images);
      this.primaryVideo = primaryVideo == null ? Optional.empty() : Optional.of(primaryVideo);
      this.videos = videos == null ? ImmutableList.of() : ImmutableList.copyOf(videos);
      this.primaryAudio = primaryAudio == null ? Optional.empty() : Optional.of(primaryAudio);
      this.audios = audios == null ? ImmutableList.of() : ImmutableList.copyOf(audios);
      this.tags = tags == null ? ImmutableList.of() : ImmutableList.copyOf(tags);
      this.rights = Strings.nullToEmpty(rights);
      this.originalContent = originalContent;
      this.citations = citations != null ? ImmutableList.copyOf(citations) : ImmutableList.of();
   }

   /**
    * Creates a new builder.
    * @return The new (empty) builder.
    */
   public static Builder builder() {
      return new Builder();
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("id", id)
              .add("title", title)
              .add("summary", summary)
              .add("cleanContent", cleanContent)
              .add("canonicalLink", canonicalLink)
              .add("altLinks", altLinks)
              .add("publishedTimestamp", publishedTimestamp)
              .add("updatedTimestamp", updatedTimestamp)
              .add("authors", authors)
              .add("primaryImage", primaryImage)
              .add("images", images)
              .add("primaryVideo", primaryVideo)
              .add("videos", videos)
              .add("primaryAudio", primaryAudio)
              .add("audios", audios)
              .add("tags", tags)
              .add("rights", rights)
              .add("originalContent", originalContent)
              .add("citations", citations)
              .toString();
   }

   /**
    * The id or an empty string if none.
    */
   public final String id;

   /**
    * The title or an empty string if none.
    */
   public final String title;

   /**
    * The summary or an empty string if none.
    */
   public final String summary;

   /**
    * The cleaned content or an empty string if none.
    */
   public final String cleanContent;

   /**
    * The canonical link or an empty string if none.
    */
   public final String canonicalLink;

   /**
    * The alt links, if any.
    */
   public final ImmutableList<String> altLinks;

   /**
    * The published timestamp or {@code 0} if unknown.
    */
   public final long publishedTimestamp;

   /**
    * The updated timestamp or {@code 0} if unknown.
    */
   public final long updatedTimestamp;

   /**
    * The immutable list of authors.
    */
   public final ImmutableList<Author> authors;

   /**
    * The optional primary image.
    */
   public final Optional<Image> primaryImage;

   /**
    * The immutable list of images.
    */
   public final ImmutableList<Image> images;

   /**
    * The optional primary video.
    */
   public final Optional<Video> primaryVideo;

   /**
    * The immutable list of videos.
    */
   public final ImmutableList<Video> videos;

   /**
    * The optional primary audio stream.
    */
   public final Optional<Audio> primaryAudio;

   /**
    * The immutable list of all audio streams.
    */
   public final ImmutableList<Audio> audios;

   /**
    * The immutable list of tags.
    */
   public final ImmutableList<String> tags;

   /**
    * The rights or an empty string if none.
    */
   public final String rights;

   /**
    * An immutable list of citations (from the entry).
    */
   public final ImmutableList<Link> citations;

   /**
    * The (parsed) original content.
    * @return A copy of the (mutable) original content element, if any.
    */
   public final Optional<Document> originalContent() {
      return originalContent == null ? Optional.empty() : Optional.of(originalContent.clone());
   }

   /**
    * The original content. Mutable, so not exposed.
    */
   private final Document originalContent;
}