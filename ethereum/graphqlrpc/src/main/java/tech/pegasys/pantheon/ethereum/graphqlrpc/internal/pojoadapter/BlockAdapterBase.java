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
package tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.LogTopic;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.graphqlrpc.GraphQLDataFetcherContext;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.LogWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.LogsQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.TransactionWithMetadata;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.transaction.CallParameter;
import tech.pegasys.pantheon.ethereum.transaction.TransactionSimulator;
import tech.pegasys.pantheon.ethereum.transaction.TransactionSimulatorResult;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.primitives.Longs;
import graphql.schema.DataFetchingEnvironment;

public class BlockAdapterBase extends AdapterBase {

  private final BlockHeader header;

  public BlockAdapterBase(final BlockHeader header) {
    this.header = header;
  }

  public Optional<NormalBlockAdapter> getParent(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    Hash parentHash = header.getParentHash();
    Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> block =
        query.blockByHash(parentHash);
    return block.map(item -> new NormalBlockAdapter(item));
  }

  public Optional<Bytes32> getHash() {
    return Optional.of(header.getHash());
  }

  public Optional<BytesValue> getNonce() {
    long nonce = header.getNonce();
    byte[] bytes = Longs.toByteArray(nonce);
    return Optional.of(BytesValue.wrap(bytes));
  }

  public Optional<Bytes32> getTransactionsRoot() {
    return Optional.of(header.getTransactionsRoot());
  }

  public Optional<Bytes32> getStateRoot() {
    return Optional.of(header.getStateRoot());
  }

  public Optional<Bytes32> getReceiptsRoot() {
    return Optional.of(header.getReceiptsRoot());
  }

  public Optional<AccountAdapter> getMiner(final DataFetchingEnvironment environment) {

    BlockchainQuery query = getBlockchainQuery(environment);
    long blockNumber = header.getNumber();
    Long bn = environment.getArgument("block");
    if (bn != null) {
      blockNumber = bn.longValue();
    }
    return Optional.of(
        new AccountAdapter(query.getWorldState(blockNumber).get().get(header.getCoinbase())));
  }

  public Optional<BytesValue> getExtraData() {
    return Optional.of(header.getExtraData());
  }

  public Optional<Long> getGasLimit() {
    return Optional.of(Long.valueOf(header.getGasLimit()));
  }

  public Optional<Long> getGasUsed() {
    return Optional.of(Long.valueOf(header.getGasUsed()));
  }

  public Optional<UInt256> getTimestamp() {
    return Optional.of(UInt256.of(header.getTimestamp()));
  }

  public Optional<BytesValue> getLogsBloom() {
    return Optional.of(header.getLogsBloom().getBytes());
  }

  public Optional<Bytes32> getMixHash() {
    return Optional.of(header.getMixHash());
  }

  public Optional<UInt256> getDifficulty() {
    return Optional.of(header.getDifficulty());
  }

  public Optional<Bytes32> getOmmerHash() {
    return Optional.of(header.getOmmersHash());
  }

  public Optional<Long> getNumber() {
    long bn = header.getNumber();
    return Optional.of(Long.valueOf(bn));
  }

  public Optional<AccountAdapter> getAccount(final DataFetchingEnvironment environment) {

    BlockchainQuery query = getBlockchainQuery(environment);
    long bn = header.getNumber();
    MutableWorldState ws = query.getWorldState(bn).get();

    if (ws != null) {
      Address addr = environment.getArgument("address");
      return Optional.of(new AccountAdapter(ws.get(addr)));
    }
    return Optional.empty();
  }

  public List<LogAdapter> getLogs(final DataFetchingEnvironment environment) {

    Map<String, Object> filter = environment.getArgument("filter");

    @SuppressWarnings("unchecked")
    List<Address> addrs = (List<Address>) filter.get("addresses");
    @SuppressWarnings("unchecked")
    List<List<Bytes32>> topics = (List<List<Bytes32>>) filter.get("topics");

    List<List<LogTopic>> transformedTopics = new ArrayList<>();
    for (List<Bytes32> topic : topics) {
      transformedTopics.add(topic.stream().map(LogTopic::of).collect(Collectors.toList()));
    }

    final LogsQuery query =
        new LogsQuery.Builder().addresses(addrs).topics(transformedTopics).build();

    final BlockchainQuery blockchain = getBlockchainQuery(environment);

    Hash hash = header.getHash();
    List<LogWithMetadata> logs = blockchain.matchingLogs(hash, query);
    List<LogAdapter> results = new ArrayList<>();
    for (LogWithMetadata log : logs) {
      results.add(new LogAdapter(log));
    }
    return results;
  }

  public Optional<Long> getEstimateGas(final DataFetchingEnvironment environment) {
    Optional<CallResult> result = executeCall(environment);
    if (result.isPresent()) {
      return Optional.of(result.get().getGasUsed());
    }
    return Optional.empty();
  }

  public Optional<CallResult> getCall(final DataFetchingEnvironment environment) {
    return executeCall(environment);
  }

  private Optional<CallResult> executeCall(final DataFetchingEnvironment environment) {
    Map<String, Object> callData = environment.getArgument("data");
    Address from = (Address) callData.get("from");
    Address to = (Address) callData.get("to");
    Long gas = (Long) callData.get("gas");
    UInt256 gasPrice = (UInt256) callData.get("gasPrice");
    UInt256 value = (UInt256) callData.get("value");
    BytesValue data = (BytesValue) callData.get("data");

    final BlockchainQuery query = getBlockchainQuery(environment);
    ProtocolSchedule<?> protocolSchedule =
        ((GraphQLDataFetcherContext) environment.getContext()).getProtocolSchedule();
    long bn = header.getNumber();

    final TransactionSimulator transactionSimulator =
        new TransactionSimulator(
            query.getBlockchain(), query.getWorldStateArchive(), protocolSchedule);

    long gasParam = -1;
    Wei gasPriceParam = null;
    Wei valueParam = null;
    if (gas != null) {
      gasParam = gas.longValue();
    }
    if (gasPrice != null) {
      gasPriceParam = Wei.of(gasPrice);
    }
    if (value != null) {
      valueParam = Wei.of(value);
    }
    final CallParameter param =
        new CallParameter(from, to, gasParam, gasPriceParam, valueParam, data);

    final Optional<TransactionSimulatorResult> opt = transactionSimulator.process(param, bn);
    if (opt.isPresent()) {
      TransactionSimulatorResult result = opt.get();
      long status = 0;
      if (result.isSuccessful()) {
        status = 1;
      }
      CallResult callResult =
          new CallResult(
              Long.valueOf(status), Long.valueOf(result.getGasEstimate()), result.getOutput());
      return Optional.of(callResult);
    }
    return Optional.empty();
  }
}
