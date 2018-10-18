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

import java.util.Comparator;

import static com.attribyte.parser.DateParser.parseISO8601;

public class SitemapLink {

   /**
    * Compare links by last modified time.
    */
   public static final Comparator<SitemapLink> modifiedTimestampComparator = new Comparator<SitemapLink>() {
      @Override
      public int compare(final SitemapLink o1, final SitemapLink o2) {
         return Long.compare(o1.lastModifiedTimestamp, o2.lastModifiedTimestamp);
      }
   };

   /**
    * Creates a sitemap link.
    * @param url The URL.
    * @param lastModifiedTimestamp The reported last modified timestamp.
    * @param changeFrequency The change frequency.
    */
   public SitemapLink(final String url, final long lastModifiedTimestamp, final ChangeFrequency changeFrequency) {
      this.url = url;
      this.lastModifiedTimestamp = lastModifiedTimestamp;
      this.changeFrequency = changeFrequency;
   }

   /**
    * Creates a sitemap link with ISO8601 timestamp and a change frequency string.
    * @param url The URL.
    * @param lastModifiedTime The last modified time.
    * @param changeFrequency The change frequency.
    * @throws java.text.ParseException on invalid timestamp.
    */
   public SitemapLink(final String url, String lastModifiedTime,
                      final String changeFrequency) throws java.text.ParseException {
      this.url = url;
      lastModifiedTime = Strings.nullToEmpty(lastModifiedTime).trim();
      this.lastModifiedTimestamp = Strings.nullToEmpty(lastModifiedTime).isEmpty() ? 0L : parseISO8601(lastModifiedTime);
      this.changeFrequency = ChangeFrequency.fromString(changeFrequency);
   }

   public enum ChangeFrequency {

      /**
       * Always changing.
       */
      ALWAYS(0),

      /**
       * Changing hourly.
       */
      HOURLY(3600),

      /**
       * Changing daily.
       */
      DAILY(3600 * 24),

      /**
       * Changing weekly.
       */
      WEEKLY (3600 * 24 * 7),

      /**
       * Changing monthly.
       */
      MONTHLY (3600 * 24 * 30),

      /**
       * Changing yearly.
       */
      YEARLY(3600 * 24 * 365),

      /**
       * Never changing.
       */
      NEVER(-1);

      ChangeFrequency(long seconds) {
         this.intervalMillis = seconds * 1000L;
      }

      /**
       * Create frequency from a string.
       * @param str The string.
       * @return The frequency.
       */
      public static ChangeFrequency fromString(final String str) {
         switch(str.toLowerCase()) {
            case "always" : return ALWAYS;
            case "hourly" : return HOURLY;
            case "daily" : return DAILY;
            case "weekly" : return WEEKLY;
            case "monthly" : return MONTHLY;
            case "yearly" : return YEARLY;
            default: return NEVER;
         }
      }

      /**
       * Is this link ready for a check?
       * @param lastCheckMillis The last time the link was checked.
       * @return Is this link ready?
       */
      public boolean checkNow(final long lastCheckMillis) {
         if(intervalMillis < 0L) {
            return false;
         } else {
            return intervalMillis == 0L || lastCheckMillis + intervalMillis < System.currentTimeMillis();
         }
      }

      private final long intervalMillis;
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("url", url)
              .add("lastModifiedTimestamp", lastModifiedTimestamp)
              .add("changeFrequency", changeFrequency)
              .toString();
   }

   /**
    * The URL.
    */
   public final String url;

   /**
    * The (reported) last modified timestamp.
    */
   public final long lastModifiedTimestamp;

   /**
    * The change frequency.
    */
   public final ChangeFrequency changeFrequency;
}
