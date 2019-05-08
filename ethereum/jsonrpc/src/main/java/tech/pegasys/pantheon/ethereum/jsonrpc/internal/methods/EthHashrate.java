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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods;

import tech.pegasys.pantheon.ethereum.blockcreation.MiningCoordinator;
import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.Quantity;

import java.util.Optional;

public class EthHashrate implements JsonRpcMethod {

  private final MiningCoordinator miningCoordinator;

  public EthHashrate(final MiningCoordinator miningCoordinator) {
    this.miningCoordinator = miningCoordinator;
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_HASHRATE.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest req) {
    try {
      final Optional<Long> hashesPerSecond = miningCoordinator.hashesPerSecond();
      if (hashesPerSecond.isPresent()) {
        return new JsonRpcSuccessResponse(req.getId(), Quantity.create(hashesPerSecond.get()));
      }
      return new JsonRpcErrorResponse(req.getId(), JsonRpcError.NO_HASHES_PER_SECOND);
    } catch (final UnsupportedOperationException ex) {
      return new JsonRpcErrorResponse(req.getId(), JsonRpcError.INVALID_REQUEST);
    }
  }
}
