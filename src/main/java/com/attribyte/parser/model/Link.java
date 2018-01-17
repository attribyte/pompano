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

import java.util.Objects;

/**
 * An immutable link element.
 * @author Matt Hamer
 */
public class Link {

   /**
    * Creates a link.
    * <ul>
    *    <li>Null values converted to empty string</li>
    *    <li>Values trimmed</li>
    * </ul>
    * @param href The href.
    * @param rel The relation attribute.
    * @param type The type attribyte.
    * @param title The title attribute.
    */
   public Link(final String href, final String rel, final String type, final String title) {
      this.href = Strings.nullToEmpty(href).trim();
      this.rel = Strings.nullToEmpty(rel).trim();
      this.type = Strings.nullToEmpty(type).trim();
      this.title = Strings.nullToEmpty(title).trim();
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("href", href)
              .add("rel", rel)
              .add("type", type)
              .add("title", title).toString();
   }

   @Override
   public int hashCode() {
      return Objects.hash(href, rel, type, title);
   }

   @Override
   public boolean equals(final Object o) {
      if(o instanceof Link) {
         Link other = (Link)o;
         return href.equals(other.href) && rel.equals(other.rel) && type.equals(other.type) && title.equals(other.title);
      } else {
         return false;
      }
   }

   /**
    * Matches the type.
    * @param rel The relation. May be <code>null</code> to match any.
    * @param type The media type. May be <code>null</code> to match any.
    * @return Do the types match?
    */
   public boolean matchType(final String rel, final String type) {
      if(rel != null && type != null) {
         return this.rel.equalsIgnoreCase(rel) && this.type.equalsIgnoreCase(type);
      } else if(rel != null) {
         return this.rel.equalsIgnoreCase(rel);
      } else {
         return type == null || this.type.equalsIgnoreCase(type);
      }
   }

   /**
    * The href attribute.
    */
   public final String href;

   /**
    * The relation attribute.
    */
   public final String rel;

   /**
    * The link type.
    */
   public final String type;

   /**
    * The link title.
    */
   public final String title;

}
