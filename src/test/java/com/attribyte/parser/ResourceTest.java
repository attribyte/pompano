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

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

import java.io.IOException;

/**
 * Base class for tests that require resources to be loaded from the classpath.
 * @author Matt Hamer
 */
public abstract class ResourceTest {

   /**
    * Loads a test resource as a string.
    * @param name The resource name.
    * @return The resource as a string.
    * @throws IOException on load failure.
    */
   protected String testResource(final String name) throws IOException {
      return new String(ByteStreams.toByteArray(getClass().getResourceAsStream(name)), Charsets.UTF_8);
   }
}