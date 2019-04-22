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
package tech.pegasys.pantheon.ethereum.graphqlrpc;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import tech.pegasys.pantheon.ethereum.graphqlrpc.extendscalar.AddressScalar;
import tech.pegasys.pantheon.ethereum.graphqlrpc.extendscalar.BigIntScalar;
import tech.pegasys.pantheon.ethereum.graphqlrpc.extendscalar.Bytes32Scalar;
import tech.pegasys.pantheon.ethereum.graphqlrpc.extendscalar.BytesScalar;
import tech.pegasys.pantheon.ethereum.graphqlrpc.extendscalar.LongScalar;

import java.io.IOException;
import java.net.URL;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

public class GraphQLProvider {
  public GraphQLProvider(final GraphQLDataFetchers graphQLDataFetchers) {
    this.graphQLDataFetchers = graphQLDataFetchers;
  }

  private GraphQL graphQL;

  public void init() throws IOException {
    URL url = Resources.getResource("schema.graphqls");
    String sdl = Resources.toString(url, Charsets.UTF_8);
    GraphQLSchema graphQLSchema = buildSchema(sdl);
    this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
  }

  public GraphQL graphQL() {
    return graphQL;
  }

  private GraphQLDataFetchers graphQLDataFetchers;

  private GraphQLSchema buildSchema(final String sdl) {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
    RuntimeWiring runtimeWiring = buildWiring();
    SchemaGenerator schemaGenerator = new SchemaGenerator();
    return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
  }

  private RuntimeWiring buildWiring() {
    return RuntimeWiring.newRuntimeWiring()
        .scalar(new AddressScalar())
        .scalar(new Bytes32Scalar())
        .scalar(new BytesScalar())
        .scalar(new LongScalar())
        .scalar(new BigIntScalar())
        .type(
            newTypeWiring("Query").dataFetcher("block", graphQLDataFetchers.getBlockDataFetcher()))
        .type(
            newTypeWiring("Block")
                .dataFetcher("parent", graphQLDataFetchers.getBlockParentDataFetcher()))
        .build();
  }
}
