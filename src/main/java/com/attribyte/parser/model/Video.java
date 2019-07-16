/*
 * Copyright 2018,2019 Attribyte, LLC
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

import java.util.Collection;
import java.util.List;

/**
 * An immutable video.
 * @author Matt Hamer
 */
public class Video {

   /**
    * Builds an immutable video.
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
       * Gets the width.
       * @return The width.
       */
      public int getWidth() {
         return width;
      }

      /**
       * Sets the width.
       * @param width The width.
       * @return A self-reference.
       */
      public Builder setWidth(final int width) {
         this.width = width;
         return this;
      }

      /**
       * Gets the height.
       * @return The height.
       */
      public int getHeight() {
         return height;
      }

      /**
       * Sets the height.
       * @param height  The height.
       * @return A self-reference.
       */
      public Builder setHeight(final int height) {
         this.height = height;
         return this;
      }

      /**
       * Gets the media type.
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
       * Gets the bitrate.
       * @return The bitrate.
       */
      public int getBitrate() {
         return bitrate;
      }

      /**
       * Sets the bitrate.
       * @param bitrate The bitrate.
       * @return A self-reference.
       */
      public Builder setBitrate(final int bitrate) {
         this.bitrate = bitrate;
         return this;
      }

      /**
       * Gets the duration in milliseconds.
       * @return The duration in milliseconds.
       */
      public long getDurationMillis() {
         return durationMillis;
      }

      /**
       * Sets the duration in milliseconds.
       * @param durationMillis The duration in milliseconds.
       * @return A self-reference.
       */
      public Builder setDurationMillis(final long durationMillis) {
         this.durationMillis = durationMillis;
         return this;
      }

      /**
       * Gets the aspect.
       * @return The aspect or {@code null}.
       */
      public Aspect getAspect() {
         return aspect;
      }

      /**
       * Sets the aspect.
       * @param aspect The aspect.
       * @return A self-reference.
       */
      public Builder setAspect(final Aspect aspect) {
         this.aspect = aspect;
         return this;
      }


      /**
       * Gets the variants.
       * @return The variants.
       */
      public ImmutableList<Video> getVariants() {
         return variants != null ? ImmutableList.copyOf(variants) : ImmutableList.of();
      }

      /**
       * Adds a variant.
       * @param variant The variant.
       * @return A self-reference.
       */
      public Builder addVariant(final Video variant) {
         if(variants != null) {
            variants = Lists.newArrayListWithExpectedSize(4);
         }
         variants.add(variant);
         return this;
      }

      /**
       * Sets/replaces all the variants.
       * @param variants The variants.
       * @return A self-reference.
       */
      public Builder setVariants(final Collection<Video> variants) {
         if(variants == null || variants.isEmpty()) {
            this.variants = null;
         } else {
            this.variants = Lists.newArrayList(variants);
         }
         return this;
      }

      /**
       * Gets the image.
       * @return The image.
       */
      public Image getImage() {
         return image;
      }

      /**
       * Sets the image.
       * @param image The image.
       * @return A self-reference.
       */
      public Builder setImage(final Image image) {
         this.image = image;
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
       * @param width The width.
       * @param height The height.
       * @param mediaType The media type.
       * @param bitrate The bitrate.
       * @param durationMillis The duration in milliseconds.
       * @param aspect The aspect.
       * @param image An image for the video.
       * @param variants The variants.
       */
      private Builder(final String id, final String link, final String altText,
                      final String title, final int width, final int height,
                      final String  mediaType,
                      final int bitrate, final long durationMillis, final Aspect aspect,
                      final Image image,
                      final Collection<Video> variants) {
         this.id = id;
         this.link = link;
         this.altText = altText;
         this.title = title;
         this.width = width;
         this.height = height;
         this.mediaType = mediaType;
         this.bitrate = bitrate;
         this.durationMillis = durationMillis;
         this.aspect = aspect;
         this.image = image;
         this.variants = variants != null ? Lists.newArrayList(variants) : null;
      }

      private String id;
      private String link;
      private String altText;
      private String title;
      private int width;
      private int height;
      private String mediaType;
      private int bitrate;
      private long durationMillis;
      private Aspect aspect;
      private List<Video> variants;
      private Image image;

      /**
       * Builds an immutable video.
       * @return The video.
       */
      public Video build() {
         return new Video(id, link, altText, title, width, height, mediaType, bitrate, durationMillis, aspect,
                 image, variants);
      }
   }

   /**
    * Creates a video.
    * @param id The id.
    * @param link The (required) link.
    * @param altText The alternate text.
    * @param title The title.
    * @param width The width.
    * @param height The height.
    * @param mediaType The media type.
    * @param bitrate The bitrate.
    * @param durationMillis The duration in milliseconds.
    * @param aspect The aspect.
    * @param image An image for the video.
    * @param variants The list of variants.
    */
   private Video(final String id, final String link, final String altText,
                 final String title, final int width, final int height,
                 final String mediaType,
                 final int bitrate, final long durationMillis, final Aspect aspect,
                 final Image image,
                 final Collection<Video> variants) {
      this.id = Strings.nullToEmpty(id);
      this.link = link;
      this.altText = Strings.nullToEmpty(altText);
      this.title = Strings.nullToEmpty(title);
      this.width = width;
      this.height = height;
      this.mediaType = Strings.nullToEmpty(mediaType);
      this.bitrate = bitrate;
      this.durationMillis = durationMillis;
      this.aspect = aspect;
      this.image = image;
      this.variants = variants != null ? ImmutableList.copyOf(variants) : ImmutableList.of();
   }

   /**
    * Creates a builder.
    * @param link The required link.
    * @return The builder.
    * @throws UnsupportedOperationException If the specified link is {@code null} or empty.
    */
   public static Builder builder(final String link) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(link).trim().isEmpty()) {
         throw new UnsupportedOperationException("The video 'link' must not be null or empty");
      }
      return new Builder(link);
   }

   /**
    * Creates a builder from an existing video.
    * @param video The video.
    * @return The builder initialized from the existing video.
    */
   public static Builder builder(final Video video) {
      return new Builder(video.id, video.link, video.altText, video.title, video.width, video.height,
              video.mediaType, video.bitrate, video.durationMillis, video.aspect, video.image,
              video.variants);
   }

   /**
    * Creates a copy of this video with a new link.
    * @param link The new link.
    * @return The new video.
    * @throws UnsupportedOperationException If the specified link is {@code null} or empty.
    */
   public Video withNewLink(final String link) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(link).trim().isEmpty()) {
         throw new UnsupportedOperationException("The video 'link' must not be null or empty");
      }
      return new Video(link, this.id, this.altText, this.title, this.width, this.height,
              this.mediaType, this.bitrate, this.durationMillis, this.aspect, this.image,
              this.variants);
   }

   /**
    * Creates a copy of this video with a new link and media type.
    * @param link The new link.
    * @param mediaType The new media type.
    * @return The new video.
    * @throws UnsupportedOperationException If the specified link is {@code null} or empty.
    */
   public Video withNewLink(final String link, final String mediaType) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(link).trim().isEmpty()) {
         throw new UnsupportedOperationException("The video 'link' must not be null or empty");
      }
      return new Video(link, this.id, this.altText, this.title, this.width, this.height,
              mediaType, this.bitrate, this.durationMillis, this.aspect, this.image,
              this.variants);
   }

   /**
    * Creates a copy of this video with a new link, media type and bitreate.
    * @param link The new link.
    * @param mediaType The new media type.
    * @param bitrate The new bitrate.
    * @return The new video.
    * @throws UnsupportedOperationException If the specified link is {@code null} or empty.
    */
   public Video withNewLink(final String link, final String mediaType, final int bitrate) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(link).trim().isEmpty()) {
         throw new UnsupportedOperationException("The video 'link' must not be null or empty");
      }
      return new Video(link, this.id, this.altText, this.title, this.width, this.height,
              mediaType, bitrate, this.durationMillis, this.aspect, this.image, this.variants);
   }

   /**
    * Creates a copy of this video with variants.
    * @param variants The variants.
    * @return The new video.
    */
   public Video withVariants(final Collection<Video> variants) {
      return new Video(link, this.id, this.altText, this.title, this.width, this.height,
              mediaType, bitrate, this.durationMillis, this.aspect, this.image, variants);
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("id", id)
              .add("link", link)
              .add("altText", altText)
              .add("title", title)
              .add("width", width)
              .add("height", height)
              .add("mediaType", mediaType)
              .add("bitrate", bitrate)
              .add("durationMillis", durationMillis)
              .add("aspect", aspect)
              .add("image", image)
              .add("variants", variants)
              .toString();
   }

   @Override
   public int hashCode() {
      return Strings.nullToEmpty(link).hashCode();
   }

   @Override
   public boolean equals(Object o) {
      if(o instanceof Video) {
         Video other = (Video)o;
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
    * A link to the video. Never {@code null} or empty.
    */
   public final String link;

   /**
    * Text that describes the video, or an empty string if none.
    */
   public final String altText;

   /**
    * A title for the video, or an empty string if none.
    */
   public final String title;

   /**
    * The video width or {@code 0} if unknown.
    */
   public final int width;

   /**
    * The video height or {@code 0} if unknown.
    */
   public final int height;

   /**
    * The video type or an empty string if none.
    */
   public final String mediaType;

   /**
    * The video bitrate.
    */
   public final int bitrate;

   /**
    * The duration in milliseconds.
    */
   public final long durationMillis;

   /**
    * The aspect.
    */
   public final Aspect aspect;

   /**
    * An image representing the video.
    */
   public final Image image;

   /**
    * Variants of this video.
    */
   public final ImmutableList<Video> variants;
}
