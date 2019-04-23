/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.pantheon.ethereum.graphqlrpc.internal.response;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Objects;

@JsonPropertyOrder({"graphqlrpc", "id", "error"})
public class GraphQLRpcErrorResponse implements GraphQLRpcResponse {

  private final Object id;
  private final GraphQLRpcError error;

  public GraphQLRpcErrorResponse(final Object id, final GraphQLRpcError error) {
    this.id = id;
    this.error = error;
  }

  @JsonGetter("id")
  public Object getId() {
    return id;
  }

  @JsonGetter("error")
  public GraphQLRpcError getError() {
    return error;
  }

  @Override
  @JsonIgnore
  public GraphQLRpcResponseType getType() {
    return GraphQLRpcResponseType.ERROR;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final GraphQLRpcErrorResponse that = (GraphQLRpcErrorResponse) o;
    return Objects.equal(id, that.id) && error == that.error;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, error);
  }
}
