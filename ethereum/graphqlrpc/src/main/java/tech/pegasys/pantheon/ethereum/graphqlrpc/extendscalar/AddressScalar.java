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
package tech.pegasys.pantheon.ethereum.graphqlrpc.extendscalar;

import java.util.Optional;

import graphql.Internal;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

@Internal
public class AddressScalar extends GraphQLScalarType {

  public AddressScalar() {
    super(
        "Address",
        "A Byte32 scalar",
        new Coercing<String, String>() {
          @Override
          public String serialize(final Object input) throws CoercingSerializeException {
            Optional<String> byte32;
            if (input instanceof String) {
              byte32 = Optional.of(String.valueOf(input));
            } else {
              byte32 = toByte32(input);
            }
            if (byte32.isPresent()) {
              return byte32.get();
            }
            throw new CoercingSerializeException(
                "Expected a 'Byte32' like object but was '" + input + "'.");
          }

          @Override
          public String parseValue(final Object input) throws CoercingParseValueException {

            if (input instanceof String) {
              return String.valueOf(String.valueOf(input));
            }
            Optional<String> url = toByte32(input);
            if (!url.isPresent()) {
              throw new CoercingParseValueException(
                  "Expected a 'URL' like object but was '" + input + "'.");
            }
            return url.get();
          }

          @Override
          public String parseLiteral(final Object input) throws CoercingParseLiteralException {
            if (!(input instanceof StringValue)) {
              throw new CoercingParseLiteralException(
                  "Expected AST type 'StringValue' but was '" + input + "'.");
            }
            return ((StringValue) input).getValue();
          }
        });
  }

  private static Optional<String> toByte32(final Object input) {
    if (input instanceof String) {
      return Optional.of((String) input);
    }
    return Optional.empty();
  }
}
