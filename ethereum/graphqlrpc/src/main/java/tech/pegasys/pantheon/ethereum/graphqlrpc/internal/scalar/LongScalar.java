/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.graphqlrpc.internal.scalar;

import com.google.common.primitives.UnsignedLong;
import graphql.Internal;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

@Internal
public class LongScalar extends GraphQLScalarType {

  public LongScalar() {
    super(
        "Long",
        "Long is a 64 bit unsigned integer",
        new Coercing<Object, Object>() {
          @Override
          public String serialize(final Object input) throws CoercingSerializeException {
            if (input instanceof UnsignedLong) {
              return "0x" + ((UnsignedLong) input).toString(16);
            }
            throw new CoercingSerializeException("Unable to serialize " + input + " as an Long");
          }

          @Override
          public String parseValue(final Object input) throws CoercingParseValueException {
            if (input instanceof UnsignedLong) {
              return "0x" + ((UnsignedLong) input).toString(16);
            }
            throw new CoercingParseValueException(
                "Unable to parse variable value " + input + " as an Long");
          }

          @Override
          public Object parseLiteral(final Object input) throws CoercingParseLiteralException {
            if (!(input instanceof StringValue)) {
              throw new CoercingParseLiteralException(
                  "Value is not any Long : '" + String.valueOf(input) + "'");
            }
            UnsignedLong result;
            try {
              String inputString = ((StringValue) input).getValue();
              result = UnsignedLong.valueOf(inputString.replaceAll("0x", ""), 16);
            } catch (NumberFormatException e) {
              throw new CoercingParseLiteralException(
                  "Value is not any Long : '" + String.valueOf(input) + "'");
            }
            return result;
          }
        });
  }
}