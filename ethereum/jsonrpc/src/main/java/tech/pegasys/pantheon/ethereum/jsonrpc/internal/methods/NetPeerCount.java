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

import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.Quantity;
import tech.pegasys.pantheon.ethereum.p2p.P2pDisabledException;
import tech.pegasys.pantheon.ethereum.p2p.api.P2PNetwork;

public class NetPeerCount implements JsonRpcMethod {
  private final P2PNetwork p2pNetwork;

  public NetPeerCount(final P2PNetwork p2pNetwork) {
    this.p2pNetwork = p2pNetwork;
  }

  @Override
  public String getName() {
    return RpcMethod.NET_PEER_COUNT.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest req) {
    try {
      return new JsonRpcSuccessResponse(req.getId(), Quantity.create(p2pNetwork.getPeers().size()));
    } catch (P2pDisabledException e) {
      return new JsonRpcErrorResponse(req.getId(), JsonRpcError.P2P_DISABLED);
    }
  }
}
