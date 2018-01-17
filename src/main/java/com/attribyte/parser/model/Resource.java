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

import java.util.List;
import java.util.Optional;

/**
 * An immutable resource like a feed or HTML page.
 * @author Matt Hamer
 */
public class Resource {

   /**
    * Builds immutable resources.
    */
   public static class Builder {

      /**
       * Gets the source link.
       * @return The source link.
       */
      public String getSourceLink() {
         return sourceLink;
      }

      /**
       * Sets the source link.
       * @param sourceLink The source link.
       * @return A self-reference.
       */
      public Builder setSourceLink(final String sourceLink) {
         this.sourceLink = sourceLink;
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
       * Gets the base link.
       * @return The base link.
       */
      public String getBaseLink() {
         return baseLink;
      }

      /**
       * Sets the base link.
       * @param baseLink The base link.
       * @return A self-reference.
       */
      public Builder setBaseLink(final String baseLink) {
         this.baseLink = baseLink;
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
       * Gets the subtitle.
       * @return The subtitle.
       */
      public String getSubtitle() {
         return subtitle;
      }

      /**
       * Sets the subtitle.
       * @param subtitle The subtitle.
       * @return A self-reference.
       */
      public Builder setSubtitle(final String subtitle) {
         this.subtitle = subtitle;
         return this;
      }

      /**
       * Gets the description.
       * @return The description.
       */
      public String getDescription() {
         return description;
      }

      /**
       * Sets the description.
       * @param description The description.
       * @return A self-reference.
       */
      public Builder setDescription(final String description) {
         this.description = description;
         return this;
      }

      /**
       * Gets the icon.
       * @return The icon.
       */
      public Image getIcon() {
         return icon;
      }

      /**
       * Sets the icon.
       * @param icon The icon.
       * @return A self-reference.
       */
      public Builder setIcon(final Image icon) {
         this.icon = icon;
         return this;
      }

      /**
       * Gets the logo.
       * @return The logo.
       */
      public Image getLogo() {
         return logo;
      }

      /**
       * Sets the logo.
       * @param logo The logo.
       * @return A self-reference.
       */
      public Builder setLogo(final Image logo) {
         this.logo = logo;
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
         this.authors = authors != null ? Lists.newArrayList(authors) : Lists.newArrayListWithExpectedSize(4);
         return this;
      }

      /**
       * Adds an author.
       * @param author The author to add.
       * @return A self reference.
       */
      public Builder addAuthor(final Author author) {
         if(authors == null) {
            authors = Lists.newArrayListWithExpectedSize(4);
         }
         authors.add(author);
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
         this.tags = tags != null ? Lists.newArrayList(tags) : Lists.newArrayListWithExpectedSize(4);
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
       * Gets an immutable list of entries.
       * @return The list of entries.
       */
      public ImmutableList<Entry> getEntries() {
         return entries != null ? ImmutableList.copyOf(entries) : ImmutableList.of();
      }

      /**
       * Sets the list of entries.
       * @param entries The list of entries.
       * @return A self-reference.
       */
      public Builder setEntries(final List<Entry> entries) {
         this.entries = entries != null ? Lists.newArrayList(entries) : Lists.newArrayListWithExpectedSize(4);
         return this;
      }

      /**
       * Adds an entry.
       * @param entry The entry to add.
       * @return A self-reference.
       */
      public Builder addEntry(final Entry entry) {
         if(entries == null) {
            entries = Lists.newArrayListWithExpectedSize(16);
         }
         entries.add(entry);
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
       * Gets the rights.
       * @return The rights.
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
       * Gets the site link.
       * @return The site link.
       */
      public String getSiteLink() {
         return siteLink;
      }

      /**
       * Sets the site link.
       * @param siteLink The site link.
       * @return A self-reference.
       */
      public Builder setSiteLink(final String siteLink) {
         this.siteLink = siteLink;
         return this;
      }

      /**
       * Gets an immutable list of feed links.
       * @return The feed links.
       */
      public ImmutableList<String> getFeedLinks() {
         return feedLinks != null ? ImmutableList.copyOf(feedLinks) : ImmutableList.of();
      }

      /**
       * Sets the feed links.
       * @param feedLinks The feed links.
       * @return A self-reference.
       */
      public Builder setFeedLinks(final List<String> feedLinks) {
         this.feedLinks = feedLinks != null ? Lists.newArrayList(feedLinks) : Lists.newArrayListWithExpectedSize(4);
         return this;
      }

      /**
       * Adds a feed link.
       * @param feedLink The link to add.
       * @return A self-reference.
       */
      public Builder addFeedLink(final String feedLink) {
         if(feedLinks == null) {
            feedLinks = Lists.newArrayListWithExpectedSize(4);
         }
         feedLinks.add(feedLink);
         return this;
      }

      /**
       * Gets the amp link.
       * @return The amp link.
       */
      public String getAmpLink() {
         return ampLink;
      }

      /**
       * Sets the amp link.
       * @param ampLink The amp link.
       * @return A self-reference.
       */
      public Builder setAmpLink(final String ampLink) {
         this.ampLink = ampLink;
         return this;
      }

      /**
       * Builds an immutable resource.
       * @return The resource.
       */
      public Resource build() {
         return new Resource(sourceLink, canonicalLink, baseLink, title, subtitle,
                 description, icon, logo, authors, tags, entries, publishedTimestamp,
                 updatedTimestamp, rights, siteLink, feedLinks, ampLink);
      }

      private String sourceLink;
      private String canonicalLink;
      private String baseLink;
      private String title;
      private String subtitle;
      private String description;
      private Image icon;
      private Image logo;
      private List<Author> authors;
      private List<String> tags;
      private List<Entry> entries;
      private long publishedTimestamp;
      private long updatedTimestamp;
      private String rights;
      private String siteLink;
      private List<String> feedLinks;
      private String ampLink;
   }

   private Resource(final String sourceLink, final String canonicalLink, final String baseLink,
                    final String title, final String subtitle, final String description, final Image icon,
                    final Image logo, final List<Author> authors, final List<String> tags,
                    final List<Entry> entries, final long publishedTimestamp,
                    final long updatedTimestamp, final String rights,
                    final String siteLink,
                    final List<String> feedLinks, final String ampLink) {
      this.sourceLink = Strings.nullToEmpty(sourceLink);
      this.canonicalLink = Strings.nullToEmpty(canonicalLink);
      this.baseLink = Strings.nullToEmpty(baseLink);
      this.title = Strings.nullToEmpty(title);
      this.subtitle = Strings.nullToEmpty(subtitle);
      this.description = Strings.nullToEmpty(description);
      this.icon = icon == null ? Optional.empty() : Optional.of(icon);
      this.logo = logo == null ? Optional.empty() : Optional.of(logo);
      this.authors = authors == null ? ImmutableList.of() : ImmutableList.copyOf(authors);
      this.tags = tags == null ? ImmutableList.of() : ImmutableList.copyOf(tags);
      this.entries = entries == null ? ImmutableList.of() : ImmutableList.copyOf(entries);
      this.publishedTimestamp = publishedTimestamp;
      this.updatedTimestamp = updatedTimestamp;
      this.rights = Strings.nullToEmpty(rights);
      this.siteLink = Strings.nullToEmpty(siteLink);
      this.feedLinks = feedLinks == null ? ImmutableList.of() : ImmutableList.copyOf(feedLinks);
      this.ampLink = ampLink != null ? Optional.of(ampLink) : Optional.empty();
   }

   /**
    * Creates a new (empty) builder.
    * @return The builder.
    */
   public static Builder builder() {
      return new Builder();
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("sourceLink", sourceLink)
              .add("canonicalLink", canonicalLink)
              .add("baseLink", baseLink)
              .add("title", title)
              .add("subtitle", subtitle)
              .add("description", description)
              .add("icon", icon)
              .add("logo", logo)
              .add("authors", authors)
              .add("tags", tags)
              .add("entries", entries)
              .add("publishedTimestamp", publishedTimestamp)
              .add("updatedTimestamp", updatedTimestamp)
              .add("rights", rights)
              .add("siteLink", siteLink)
              .add("feedLinks", feedLinks)
              .add("ampLink", ampLink)
              .toString();
   }

   /**
    * The source (input) link.
    */
   public final String sourceLink;

   /**
    * The canonical link for the resource.
    */
   public final String canonicalLink;

   /**
    * The base link used to resolve relative references.
    */
   public final String baseLink;

   /**
    * The title, or empty if none.
    */
   public final String title;

   /**
    * The subtitle or empty if none.
    */
   public final String subtitle;

   /**
    * The description or empty if none.
    */
   public final String description;

   /**
    * The optional icon.
    */
   public final Optional<Image> icon;

   /**
    * The optional logo.
    */
   public final Optional<Image> logo;

   /**
    * The immutable list of authors.
    */
   public final ImmutableList<Author> authors;

   /**
    * The immutable list of tags.
    */
   public final ImmutableList<String> tags;

   /**
    * The immutable list of entries.
    */
   public final ImmutableList<Entry> entries;

   /**
    * The last published timestamp or {@code 0} if unknown.
    */
   public final long publishedTimestamp;

   /**
    * The last updated timestamp or {@code 0} if unknown.
    */
   public final long updatedTimestamp;

   /**
    * The usage rights statement.
    */
   public final String rights;

   /**
    * A link to the site associated with the resource.
    */
   public final String siteLink;

   /**
    * The immutable list of feed links.
    */
   public final ImmutableList<String> feedLinks;

   /**
    * The optional amp link.
    */
   public final Optional<String> ampLink;
}