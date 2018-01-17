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

import com.attribyte.parser.model.Resource;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

/**
 * A parsed resource, on successful parse along with any errors.
 * @author Matt Hamer
 */
public class ParseResult {

   /**
    * Creates a parse result with a parsed resource and no errors.
    * @param resource The resource.
    */
   public ParseResult(final Resource resource) {
      this(resource, null);
   }

   /**
    * Creates a parse result with a single error and no parsed resource.
    * @param error The error.
    */
   public ParseResult(final ParseError error) {
      this.resource = Optional.empty();
      this.errors = ImmutableList.of(error);
   }

   /**
    * Creates a parse result with a list of errors and no parsed resource.
    * @param errors The errors.
    */
   public ParseResult(final List<ParseError> errors) {
      this.resource = Optional.empty();
      this.errors = errors != null ? ImmutableList.copyOf(errors) : ImmutableList.of();
   }

   /**
    * Creates a parse result with a parsed resource and a list of (non-fatal) errors.
    * @param resource The resource.
    * @param errors The errors.
    */
   public ParseResult(final Resource resource, final List<ParseError> errors) {
      this.resource = Optional.of(resource);
      this.errors = errors != null ? ImmutableList.copyOf(errors) : ImmutableList.of();
   }

   /**
    * Does the result have any errors?
    * @return Does the result have any errors?
    */
   public boolean hasErrors() {
      return !errors.isEmpty();
   }

   /**
    * The resource, if parse was successful.
    */
   public final Optional<Resource> resource;

   /**
    * An immutable list of errors.
    */
   public final ImmutableList<ParseError> errors;
}