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

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.debug.TraceOptions;
import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.TransactionTraceParams;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTracer;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.TransactionWithMetadata;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.DebugTraceTransactionResult;
import tech.pegasys.pantheon.ethereum.vm.DebugOperationTracer;

import java.util.Optional;

public class DebugTraceTransaction implements JsonRpcMethod {

  private final JsonRpcParameter parameters;
  private final TransactionTracer transactionTracer;
  private final BlockchainQueries blockchain;

  public DebugTraceTransaction(
      final BlockchainQueries blockchain,
      final TransactionTracer transactionTracer,
      final JsonRpcParameter parameters) {
    this.blockchain = blockchain;
    this.transactionTracer = transactionTracer;
    this.parameters = parameters;
  }

  @Override
  public String getName() {
    return RpcMethod.DEBUG_TRACE_TRANSACTION.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final Hash hash = parameters.required(request.getParams(), 0, Hash.class);
    final Optional<TransactionWithMetadata> transactionWithMetadata =
        blockchain.transactionByHash(hash);
    if (transactionWithMetadata.isPresent()) {
      final TraceOptions traceOptions =
          parameters
              .optional(request.getParams(), 1, TransactionTraceParams.class)
              .map(TransactionTraceParams::traceOptions)
              .orElse(TraceOptions.DEFAULT);
      final DebugTraceTransactionResult debugTraceTransactionResult =
          debugTraceTransactionResult(hash, transactionWithMetadata.get(), traceOptions);

      return new JsonRpcSuccessResponse(request.getId(), debugTraceTransactionResult);
    } else {
      return new JsonRpcSuccessResponse(request.getId(), null);
    }
  }

  private DebugTraceTransactionResult debugTraceTransactionResult(
      final Hash hash,
      final TransactionWithMetadata transactionWithMetadata,
      final TraceOptions traceOptions) {
    final Hash blockHash = transactionWithMetadata.getBlockHash();

    final DebugOperationTracer execTracer = new DebugOperationTracer(traceOptions);

    return transactionTracer
        .traceTransaction(blockHash, hash, execTracer)
        .map(DebugTraceTransactionResult::new)
        .orElse(null);
  }
}
