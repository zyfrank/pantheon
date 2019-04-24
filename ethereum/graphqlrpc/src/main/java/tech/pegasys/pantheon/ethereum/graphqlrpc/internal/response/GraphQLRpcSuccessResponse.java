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

@JsonPropertyOrder({"graphqlrpc", "errors", "data"})
public class GraphQLRpcSuccessResponse implements GraphQLRpcResponse {

  private final Object errors;
  private final Object data;

  public GraphQLRpcSuccessResponse(final Object errors, final Object data) {
    this.errors = errors;
    this.data = data;
  }

  public GraphQLRpcSuccessResponse(final Object data) {
    this.data = data;
    errors = null;
  }

  @JsonGetter("errors")
  public Object getErrors() {
    return errors;
  }

  @JsonGetter("data")
  public Object getData() {
    return data;
  }

  @Override
  @JsonIgnore
  public GraphQLRpcResponseType getType() {
    return GraphQLRpcResponseType.SUCCESS;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final GraphQLRpcSuccessResponse that = (GraphQLRpcSuccessResponse) o;
    return Objects.equal(errors, that.errors) && Objects.equal(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(errors, data);
  }
}
