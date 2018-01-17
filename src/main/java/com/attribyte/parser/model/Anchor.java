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
import com.google.common.net.InternetDomainName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * An immutable anchor element.
 * @author Matt Hamer
 */
public class Anchor {

   /**
    * Creates an anchor.
    * <ul>
    *    <li>Null values converted to empty string</li>
    *    <li>Values trimmed</li>
    * </ul>
    * @param href The href.
    * @param title The title.
    * @param anchorText The anchor text (text content of 'a' element).
    */
   public Anchor(final String href, final String title, final String anchorText) {
      this.href = Strings.nullToEmpty(href).trim();
      this.title = Strings.nullToEmpty(title).trim();
      this.anchorText = Strings.nullToEmpty(anchorText).trim();
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("href", href)
              .add("title", title)
              .add("anchorText", anchorText).toString();
   }

   @Override
   public int hashCode() {
      return Objects.hash(href, title, anchorText);
   }

   @Override
   public boolean equals(final Object o) {
      if(o instanceof Anchor) {
         Anchor other = (Anchor)o;
         return href.equals(other.href) && title.equals(other.title) && anchorText.equals(other.anchorText);
      } else {
         return false;
      }
   }

   /**
    * Determine if the domain name for the href matches a specified domain name.
    * @param checkDomain The domain to check.
    * @return Does the domain match?
    */
   public boolean matchesDomain(final InternetDomainName checkDomain) {
      try {
         URL url = new URL(href);
         String host = url.getHost();
         return host != null && InternetDomainName.isValid(host) && InternetDomainName.from(host).topPrivateDomain().equals(checkDomain.topPrivateDomain());
      } catch(MalformedURLException | IllegalStateException e) {
         return false;
      }
   }

   /**
    * The href attribute.
    */
   public final String href;

   /**
    * The title attribute.
    */
   public final String title;

   /**
    * The anchor text.
    */
   public final String anchorText;
}
