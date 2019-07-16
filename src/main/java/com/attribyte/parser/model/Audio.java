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

/**
 * An immutable audio stream.
 * @author Matt Hamer
 */
public class Audio {

   /**
    * Builds an immutable audio stream.
    */
   public static class Builder {

      /**
       * Gets the id.
       * @return The id.
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
       * Gets the link.
       * @return The link.
       */
      public String getLink() {
         return link;
      }

      /**
       * Gets the alternate text.
       * @return The alternate text.
       */
      public String getAltText() {
         return altText;
      }

      /**
       * Sets the alternate text.
       * @param altText The alternate text.
       * @return A self-reference.
       */
      public Builder setAltText(final String altText) {
         this.altText = altText;
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
       * Gets the type.
       * @return The type.
       */
      public String getMediaType() {
         return mediaType;
      }

      /**
       * Sets the type.
       * @param mediaType The media type.
       * @return A self-reference.
       */
      public Builder setMediaType(final String mediaType) {
         this.mediaType = mediaType;
         return this;
      }

      /**
       * Creates a builder with the required link.
       * @param link The required link.
       */
      private Builder(final String link) {
         this.link = link;
      }

      /**
       * Creates a fully-populated builder.
       * @param id The id.
       * @param link The link.
       * @param altText The alt text.
       * @param title The title.
       * @param mediaType The media type.
       */
      private Builder(final String id, final String link, final String altText,
                      final String title,
                      final String  mediaType) {
         this.id = id;
         this.link = link;
         this.altText = altText;
         this.title = title;
         this.mediaType = mediaType;
      }

      private String id;
      private String link;
      private String altText;
      private String title;
      private String mediaType;

      /**
       * Builds an immutable audio stream.
       * @return The audio.
       */
      public Audio build() {
         return new Audio(id, link, altText, title, mediaType);
      }
   }

   /**
    * Creates an audio stream.
    * @param id The id.
    * @param link The (required) link.
    * @param altText The alternate text.
    * @param title The title.
    * @param mediaType The media type.
    */
   private Audio(final String id, final String link, final String altText,
                 final String title,
                 final String mediaType) {
      this.id = Strings.nullToEmpty(id);
      this.link = link;
      this.altText = Strings.nullToEmpty(altText);
      this.title = Strings.nullToEmpty(title);
      this.mediaType = Strings.nullToEmpty(mediaType);
   }

   /**
    * Creates a builder.
    * @param link The required link.
    * @return The builder.
    * @throws UnsupportedOperationException If the specified link is {@code null} or empty.
    */
   public static Builder builder(final String link) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(link).trim().isEmpty()) {
         throw new UnsupportedOperationException("The audio 'link' must not be null or empty");
      }
      return new Builder(link);
   }

   /**
    * Creates a builder from an existing audio stream.
    * @param audio The audio.
    * @return The builder initialized from the existing audio.
    */
   public static Builder builder(final Audio audio) {
      return new Builder(audio.id, audio.link, audio.altText, audio.title, audio.mediaType);
   }

   /**
    * Creates a copy of this audio stream with a new link.
    * @param link The new link.
    * @return The new audio.
    * @throws UnsupportedOperationException If the specified link is {@code null} or empty.
    */
   public Audio withNewLink(final String link) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(link).trim().isEmpty()) {
         throw new UnsupportedOperationException("The audio 'link' must not be null or empty");
      }
      return new Audio(link, this.id, this.altText, this.title, this.mediaType);
   }

   /**
    * Creates a copy of this audio stream with a new link and type.
    * @param link The new link.
    * @param mediaType The new media type.
    * @return The new audio.
    * @throws UnsupportedOperationException If the specified link is {@code null} or empty.
    */
   public Audio withNewLink(final String link, final String mediaType) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(link).trim().isEmpty()) {
         throw new UnsupportedOperationException("The audio 'link' must not be null or empty");
      }
      return new Audio(link, this.id, this.altText, this.title, mediaType);
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("id", id)
              .add("link", link)
              .add("altText", altText)
              .add("title", title)
              .add("mediaType", mediaType)
              .toString();
   }

   @Override
   public int hashCode() {
      return Strings.nullToEmpty(link).hashCode();
   }

   @Override
   public boolean equals(Object o) {
      if(o instanceof Audio) {
         Audio other = (Audio)o;
         return Strings.nullToEmpty(other.link).equals(Strings.nullToEmpty(link));
      } else {
         return false;
      }
   }

   /**
    * A unique id, or an empty string if none.
    */
   public final String id;

   /**
    * A link to the audio stream. Never {@code null} or empty.
    */
   public final String link;

   /**
    * Text that describes the audio, or an empty string if none.
    */
   public final String altText;

   /**
    * A title for the audio, or an empty string if none.
    */
   public final String title;

   /**
    * The audio type or an empty string if none.
    */
   public final String mediaType;
}
