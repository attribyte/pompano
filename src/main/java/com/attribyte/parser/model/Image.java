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
import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;

import java.util.Optional;

/**
 * An immutable image.
 * @author Matt Hamer
 */
public class Image {

   /**
    * Builds an immutable image.
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
       * Creates a builder with a link.
       * @param link The link.
       */
      private Builder(final String link) {
         this.link = link;
      }

      /**
       * Creates a builder with image data.
       * @param data The image data.
       */
      private Builder(final byte[] data) {
         this.link = Hashing.sha256().hashBytes(data).toString();
         this.data = data;
      }

      /**
       * Creates a fully-populated builder.
       * @param id The id.
       * @param link The link.
       * @param altText The alt text.
       * @param title The title.
       * @param width The width.
       * @param height The height.
       * @param data The image data.
       */
      private Builder(final String id, final String link, final String altText,
                      final String title, final int width, final int height,
                      final byte[] data) {
         this.id = id;
         this.link = link;
         this.altText = altText;
         this.title = title;
         this.width = width;
         this.height = height;
         this.data = data;
      }

      private String id;
      private String link;
      private String altText;
      private String title;
      private int width;
      private int height;
      private byte[] data;

      /**
       * Builds an immutable image.
       * @return The image.
       */
      public Image build() {
         return new Image(id, link, altText, title, width, height, data);
      }
   }

   /**
    * Creates an image.
    * @param id The id.
    * @param link The (required) link.
    * @param altText The alternate text.
    * @param title The title.
    * @param width The width.
    * @param height The height.
    * @param data The data.
    */
   private Image(final String id, final String link, final String altText,
                 final String title, final int width, final int height,
                 final byte[] data) {
      this.id = Strings.nullToEmpty(id);
      this.link = link;
      this.altText = Strings.nullToEmpty(altText);
      this.title = Strings.nullToEmpty(title);
      this.width = width;
      this.height = height;
      this.data = data != null && data.length > 0 ? Optional.of(ByteString.copyFrom(data)) : Optional.empty();
   }

   /**
    * Creates an image.
    * @param id The id.
    * @param link The (required) link.
    * @param altText The alternate text.
    * @param title The title.
    * @param width The width.
    * @param height The height.
    * @param data The data.
    */
   private Image(final String id, final String link, final String altText,
                 final String title, final int width, final int height,
                 final Optional<ByteString> data) {
      this.id = Strings.nullToEmpty(id);
      this.link = link;
      this.altText = Strings.nullToEmpty(altText);
      this.title = Strings.nullToEmpty(title);
      this.width = width;
      this.height = height;
      this.data = data;
   }

   /**
    * Creates a builder for images with a link.
    * @param link The link.
    * @return The builder.
    * @throws UnsupportedOperationException If the specified link is {@code null} or empty.
    */
   public static Builder builder(final String link) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(link).trim().isEmpty()) {
         throw new UnsupportedOperationException("The image 'link' must not be null or empty");
      }
      return new Builder(link);
   }

   /**
    * Creates a builder for images with data.
    * @param data The data.
    * @return The builder.
    * @throws UnsupportedOperationException If the specified data is {@code null} or empty.
    */
   public static Builder builder(final byte[] data) throws UnsupportedOperationException {
      if(data == null || data.length == 0) {
         throw new UnsupportedOperationException("The image 'data' must not be null or empty");
      } else {
         return new Builder(data);
      }
   }

   /**
    * Creates a builder from an existing image.
    * @param image The image.
    * @return The builder initialized from the existing image.
    */
   public static Builder builder(final Image image) {
      return new Builder(image.id, image.link, image.altText, image.title, image.width, image.height, image.data.map(ByteString::toByteArray).orElse(null));
   }

   /**
    * Creates a copy of this image with a new link.
    * @param link The new link.
    * @return The new image.
    * @throws UnsupportedOperationException If the specified link is {@code null} or empty.
    */
   public Image withNewLink(final String link) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(link).trim().isEmpty()) {
         throw new UnsupportedOperationException("The image 'link' must not be null or empty");
      }
      return new Image(link, this.id, this.altText, this.title, this.width, this.height, this.data);
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
              .add("data", data)
              .toString();
   }

   @Override
   public int hashCode() {
      return Strings.nullToEmpty(link).hashCode();
   }

   @Override
   public boolean equals(Object o) {
      if(o instanceof Image) {
         Image other = (Image)o;
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
    * A link to the image. Never {@code null} or empty.
    */
   public final String link;

   /**
    * Text that describes the image, or an empty string if none.
    */
   public final String altText;

   /**
    * A title for the image, or an empty string if none.
    */
   public final String title;

   /**
    * The image width or {@code 0} if unknown.
    */
   public final int width;

   /**
    * The image height or {@code 0} if unknown.
    */
   public final int height;

   /**
    * The image data (bytes), if available.
    */
   public final Optional<ByteString> data;
}
