/*
 * Copyright 2026 Attribyte Labs, LLC
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

package com.attribyte.parser.pdf;

final class PdfLine {

   PdfLine(String text, float fontSize, float yPosition, float xPosition,
           boolean isBold, int pageNumber, boolean paragraphStart) {
      this.text = text;
      this.fontSize = fontSize;
      this.yPosition = yPosition;
      this.xPosition = xPosition;
      this.isBold = isBold;
      this.pageNumber = pageNumber;
      this.paragraphStart = paragraphStart;
   }

   final String text;
   final float fontSize;
   final float yPosition;
   final float xPosition;
   final boolean isBold;
   final int pageNumber;
   final boolean paragraphStart;
}