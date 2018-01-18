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

package com.attribyte.parser;

import org.junit.Test;


import static org.junit.Assert.*;

/**
 * Tests for utility methods.
 * @author Matt Hamer
 */
public class UtilTest {

   @Test
   public void endsWithIgnoreInvisibleMatch() {
      String match = "</rss>";
      String test = "this is the </rss>\n\n\t   ";
      assertTrue(Util.endsWithIgnoreInvisible(match, test));
   }

   @Test
   public void endsWithIgnoreInvisibleFail() {
      String match = "</xrss>";
      String test = "this is the </rss>";
      assertFalse(Util.endsWithIgnoreInvisible(match, test));
   }

   @Test
   public void endsWithIgnoreInvisibleShortTest() {
      String match = "</xrss>";
      String test = "</rss>";
      assertFalse(Util.endsWithIgnoreInvisible(match, test));
   }

   @Test
   public void endsWithIgnoreInvisibleEmptyTest() {
      String match = "</rss>";
      String test = "";
      assertFalse(Util.endsWithIgnoreInvisible(match, test));
   }

   @Test
   public void endsWithIgnoreInvisibleEmptyMatch() {
      String match = "";
      String test = "</rss>";
      assertFalse(Util.endsWithIgnoreInvisible(match, test));
   }

   @Test
   public void startsWithIgnoreInvisibleMatch() {
      String match = "<rss>";
      String test = "\n\n  \t<rss> blah \n\n\t   ";
      assertTrue(Util.startsWithIgnoreInvisible(match, test));
   }

   @Test
   public void startsWithIgnoreInvisibleFail() {
      String match = "<rss2>";
      String test = "\n\n  \t<rss> blah blah \n\n\t   ";
      assertFalse(Util.startsWithIgnoreInvisible(match, test));
   }

   @Test
   public void startsWithIgnoreInvisibleShortTest() {
      String match = "<rss>";
      String test = "<rss2>";
      assertFalse(Util.startsWithIgnoreInvisible(match, test));
   }

   @Test
   public void startsWithIgnoreInvisibleEmptyMatch() {
      String match = "";
      String test = "<rss2>";
      assertFalse(Util.startsWithIgnoreInvisible(match, test));
   }

   @Test
   public void startsWithIgnoreInvisibleEmptyTest() {
      String match = "<rss>";
      String test = "";
      assertFalse(Util.startsWithIgnoreInvisible(match, test));
   }
}
