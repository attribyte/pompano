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

import com.google.common.base.MoreObjects;

import java.util.Optional;

/**
 * Reports an error during parse.
 * @author Matt Hamer
 */
public class ParseError {

   /**
    * Creates a parse error with a position.
    * @param message The error message.
    * @param position The position.
    */
   public ParseError(final String message, final int position) {
      this(message, null, 0);
   }

   /**
    * Creates a parse error with unknown position.
    * @param message The error message.
    */
   public ParseError(final String message) {
      this(message, 0);
   }

   /**
    * Creates a parse error with an exception and unknown position.
    * @param message The message.
    * @param exception An exception.
    */
   public ParseError(final String message, final Throwable exception) {
      this(message, exception, 0);
   }

   /**
    * Creates a parse error with an exception and position.
    * @param message The message.
    * @param exception An exception.
    * @param position The position.
    */
   public ParseError(final String message, final Throwable exception, final int position) {
      this.message = message;
      this.exception = exception != null ? Optional.of(exception) : Optional.empty();
      this.position = position;
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("message", message)
              .add("exception", exception)
              .add("position", position)
              .toString();
   }

   /**
    * The error message.
    */
   public final String message;

   /**
    * An optional exception;
    */
   public final Optional<Throwable> exception;

   /**
    * The character position in the parsed resource or {@code 0} if unknown.
    */
   public final int position;
}
