/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.elasticsearch.hadoop.serialization;


/**
 * Translates a JSON field to an actual object. Implementations should only handle the conversion and not influence the parser outside the conversion.
 */
public interface FieldReader {

    /**
     * Returns a non-null object if the content was understood, null otherwise.
     *
     * @param parser
     * @return a non-null object if the content was understood, null otherwise.
     */
    Object read(Parser parser, FieldType esType);

    /**
     * Returns the failed token (for debugging purposes) in case the reader cannot understand the parser content.
     *
     * @return
     */
    CharSequence failedToken();

}