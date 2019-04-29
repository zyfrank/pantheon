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
import com.google.common.primitives.UnsignedLong;
import graphql.schema.DataFetchingEnvironment;

public class BlockAdapter extends AdapterBase {
  public BlockAdapter(final BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetaData) {
    this.blockWithMetaData = blockWithMetaData;
  }

  private final BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetaData;

  public Optional<BlockAdapter> getParent(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    Hash parentHash = blockWithMetaData.getHeader().getParentHash();
    Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> block =
        query.blockByHash(parentHash);
    return block.map(item -> new BlockAdapter(item));
  }

  public Optional<Bytes32> getHash() {
    return Optional.of(blockWithMetaData.getHeader().getHash());
  }

  public Optional<BytesValue> getNonce() {
    long nonce = blockWithMetaData.getHeader().getNonce();
    byte[] bytes = Longs.toByteArray(nonce);
    return Optional.of(BytesValue.wrap(bytes));
  }

  public Optional<Bytes32> getTransactionsRoot() {
    return Optional.of(blockWithMetaData.getHeader().getTransactionsRoot());
  }

  public Optional<Integer> getTransactionCount() {
    return Optional.of(blockWithMetaData.getTransactions().size());
  }

  public Optional<Bytes32> getStateRoot() {
    return Optional.of(blockWithMetaData.getHeader().getStateRoot());
  }

  public Optional<Bytes32> getReceiptsRoot() {
    return Optional.of(blockWithMetaData.getHeader().getReceiptsRoot());
  }

  public Optional<AccountAdapter> getMiner(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    UnsignedLong blockNo = environment.getArgument("block");

    return Optional.of(
        new AccountAdapter(
            query
                .getWorldState(blockNo.longValue())
                .get()
                .get(blockWithMetaData.getHeader().getCoinbase())));
  }

  public Optional<BytesValue> getExtraData() {
    return Optional.of(blockWithMetaData.getHeader().getExtraData());
  }

  public Optional<UnsignedLong> getGasLimit() {
    return Optional.of(UnsignedLong.valueOf(blockWithMetaData.getHeader().getGasLimit()));
  }

  public Optional<UnsignedLong> getGasUsed() {
    return Optional.of(UnsignedLong.valueOf(blockWithMetaData.getHeader().getGasUsed()));
  }

  public Optional<UInt256> getTimestamp() {
    return Optional.of(UInt256.of(blockWithMetaData.getHeader().getTimestamp()));
  }

  public Optional<BytesValue> getLogsBloom() {
    return Optional.of(blockWithMetaData.getHeader().getLogsBloom().getBytes());
  }

  public Optional<Bytes32> getMixHash() {
    return Optional.of(blockWithMetaData.getHeader().getMixHash());
  }

  public Optional<UInt256> getDifficulty() {
    return Optional.of(blockWithMetaData.getHeader().getDifficulty());
  }

  public Optional<UInt256> getTotalDifficulty() {
    return Optional.of(blockWithMetaData.getTotalDifficulty());
  }

  public Optional<Integer> getOmmerCount() {
    return Optional.of(blockWithMetaData.getOmmers().size());
  }

  public List<BlockAdapter> getOmmers(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    List<Hash> ommers = blockWithMetaData.getOmmers();
    List<BlockAdapter> results = new ArrayList<BlockAdapter>();
    for (Hash item : ommers) {
      Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> block = query.blockByHash(item);
      block.ifPresent(ele -> results.add(new BlockAdapter(ele)));
    }
    return results;
  }

  public Optional<BlockAdapter> getOmmerAt(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    int index = environment.getArgument("index");
    List<Hash> ommers = blockWithMetaData.getOmmers();
    if (ommers.size() > index) {
      Hash ommer = ommers.get(index);
      Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> block = query.blockByHash(ommer);
      return block.map(item -> new BlockAdapter(item));
    }
    return Optional.empty();
  }

  public Optional<Bytes32> getOmmerHash() {
    return Optional.of(blockWithMetaData.getHeader().getOmmersHash());
  }

  public List<TransactionAdapter> getTransactions() {
    List<TransactionWithMetadata> trans = blockWithMetaData.getTransactions();
    List<TransactionAdapter> results = new ArrayList<TransactionAdapter>();
    for (TransactionWithMetadata tran : trans) {
      results.add(new TransactionAdapter(tran));
    }
    return results;
  }

  public Optional<TransactionAdapter> getTransactionAt(final DataFetchingEnvironment environment) {
    int index = environment.getArgument("index");
    List<TransactionWithMetadata> trans = blockWithMetaData.getTransactions();

    if (trans.size() > index) {
      return Optional.of(new TransactionAdapter(trans.get(index)));
    }

    return Optional.empty();
  }

  public Optional<UnsignedLong> getNumber() {
    long bn = blockWithMetaData.getHeader().getNumber();
    return Optional.of(UnsignedLong.valueOf(bn));
  }

  public Optional<AccountAdapter> getAccount(final DataFetchingEnvironment environment) {

    BlockchainQuery query = getBlockchainQuery(environment);
    long bn = blockWithMetaData.getHeader().getNumber();
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

    Hash hash = blockWithMetaData.getHeader().getHash();
    List<LogWithMetadata> logs = blockchain.matchingLogs(hash, query);
    List<LogAdapter> results = new ArrayList<>();
    for (LogWithMetadata log : logs) {
      results.add(new LogAdapter(log));
    }
    return results;
  }

  public Optional<UnsignedLong> getEstimateGas(final DataFetchingEnvironment environment) {
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
    UnsignedLong gas = (UnsignedLong) callData.get("gas");
    UInt256 gasPrice = (UInt256) callData.get("gasPrice");
    UInt256 value = (UInt256) callData.get("value");
    BytesValue data = (BytesValue) callData.get("data");

    final BlockchainQuery query = getBlockchainQuery(environment);
    ProtocolSchedule<?> protocolSchedule =
        ((GraphQLDataFetcherContext) environment.getContext()).getProtocolSchedule();
    long bn = blockWithMetaData.getHeader().getNumber();

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
              UnsignedLong.valueOf(status),
              UnsignedLong.valueOf(result.getGasEstimate()),
              result.getOutput());
      return Optional.of(callResult);
    }
    return Optional.empty();
  }
}