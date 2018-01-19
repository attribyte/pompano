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

import com.attribyte.parser.model.DataURI;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@code DataURI}.
 * @author Matt Hamer
 */
public class DataURITest {

   @Test
   public void text() {
      String src = "data:text/plain,12345";
      DataURI duri = new DataURI(src);
      assertEquals("text/plain", duri.mediaType);
      assertFalse(duri.base64Encoded);
      assertEquals("12345", duri.data.toStringUtf8());
   }

   @Test
   public void invalidScheme() {
      String src = "datax:text/plain,INVALID";
      try {
         DataURI duri = new DataURI(src);
         fail("Expecting illegal argument exception to be thrown");

      } catch(IllegalArgumentException ie) {
         //Excpected
      }
   }

   @Test
   public void invalidData() {
      String src = "datax:text/plain  ";
      try {
         DataURI duri = new DataURI(src);
         fail("Expecting illegal argument exception to be thrown");

      } catch(IllegalArgumentException ie) {
         //Excpected
      }
   }

   @Test
   public void base64() {
      String data = BaseEncoding.base64().encode("12345".getBytes(Charsets.UTF_8));
      String src = "data:text/plain;base64," + data;
      DataURI duri = new DataURI(src);
      assertEquals("text/plain", duri.mediaType);
      assertTrue(duri.base64Encoded);
      assertEquals("12345", duri.data.toStringUtf8());
   }

   @Test
   public void invalidBase64() {
      String data = BaseEncoding.base64().encode("12345".getBytes(Charsets.UTF_8));
      String src = "data:text/plain;base64," + data + "INVALID";
      try {
         DataURI duri = new DataURI(src);
         fail("Expecting illegal argument exception to be thrown");

      } catch(IllegalArgumentException ie) {
         //Excpected
      }
   }

   @Test
   public void emptyData() {
      String src = "data:text/plain,";
      DataURI duri = new DataURI(src);
      assertEquals("text/plain", duri.mediaType);
      assertFalse(duri.base64Encoded);
      assertTrue(duri.data.isEmpty());
   }

   @Test
   public void emptyContentType() {
      String src = "data:,12345";
      DataURI duri = new DataURI(src);
      assertTrue(duri.mediaType.isEmpty());
      assertFalse(duri.base64Encoded);
      assertEquals("12345", duri.data.toStringUtf8());
   }

   @Test
   public void base64EmptyData() {
      String src = "data:text/plain;base64,";
      DataURI duri = new DataURI(src);
      assertEquals("text/plain", duri.mediaType);
      assertTrue(duri.base64Encoded);
      assertTrue(duri.data.isEmpty());
   }

   @Test
   public void minimal() {
      String src = "data:,";
      DataURI duri = new DataURI(src);
      assertTrue(duri.mediaType.isEmpty());
      assertFalse(duri.base64Encoded);
      assertTrue(duri.data.isEmpty());
   }
}
