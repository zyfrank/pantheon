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
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.TransactionWithMetadata;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.List;

import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLong;
import graphql.schema.DataFetchingEnvironment;

public class BlockAdapter extends AdapterBase {
  public BlockAdapter(final BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetaData) {
    this.blockWithMetaData = blockWithMetaData;
  }

  private final BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetaData;
  /*
    private BlockchainQuery getBlockchainQuery(final DataFetchingEnvironment environment) {
      return ((GraphQLDataFetcherContext) environment.getContext()).getBlockchainQuery();
    }
  */
  public BlockAdapter getParent(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    Hash parentHash = blockWithMetaData.getHeader().getParentHash();
    BlockWithMetadata<TransactionWithMetadata, Hash> block = query.blockByHash(parentHash).get();
    if (block != null) {
      return new BlockAdapter(block);
    }
    return null;
  }

  public Bytes32 getHash() {
    return blockWithMetaData.getHeader().getHash();
  }

  public BytesValue getNonce() {
    long nonce = blockWithMetaData.getHeader().getNonce();
    byte[] bytes = Longs.toByteArray(nonce);
    return BytesValue.wrap(bytes);
  }

  public Bytes32 getTransactionsRoot() {
    return blockWithMetaData.getHeader().getTransactionsRoot();
  }

  public int getTransactionCount() {
    return blockWithMetaData.getTransactions().size();
  }

  public Bytes32 getStateRoot() {
    return blockWithMetaData.getHeader().getStateRoot();
  }

  public Bytes32 getReceiptsRoot() {
    return blockWithMetaData.getHeader().getReceiptsRoot();
  }

  public AccountAdapter getMiner(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    UnsignedLong blockNo = environment.getArgument("block");

    return new AccountAdapter(
        query
            .getWorldState(blockNo.longValue())
            .get()
            .get(blockWithMetaData.getHeader().getCoinbase()));
  }

  public BytesValue getExtraData() {
    return blockWithMetaData.getHeader().getExtraData();
  }

  public UnsignedLong getGasLimit() {
    return UnsignedLong.valueOf(blockWithMetaData.getHeader().getGasLimit());
  }

  public UnsignedLong getGasUsed() {
    return UnsignedLong.valueOf(blockWithMetaData.getHeader().getGasUsed());
  }

  public UInt256 getTimestamp() {
    return UInt256.of(blockWithMetaData.getHeader().getTimestamp());
  }

  public BytesValue getLogsBloom() {
    return blockWithMetaData.getHeader().getLogsBloom().getBytes();
  }

  public Bytes32 getMixHash() {
    return blockWithMetaData.getHeader().getMixHash();
  }

  public UInt256 getDifficulty() {
    return blockWithMetaData.getHeader().getDifficulty();
  }

  public UInt256 getTotalDifficulty() {
    return blockWithMetaData.getTotalDifficulty();
  }

  public int getOmmerCount() {
    return blockWithMetaData.getOmmers().size();
  }

  public List<BlockAdapter> getOmmers(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    List<Hash> ommers = blockWithMetaData.getOmmers();
    List<BlockAdapter> results = new ArrayList<BlockAdapter>();
    for (Hash item : ommers) {
      BlockWithMetadata<TransactionWithMetadata, Hash> block = query.blockByHash(item).get();
      if (block != null) {
        results.add(new BlockAdapter(block));
      }
    }
    if (results.size() > 0) {
      return results;
    }
    return null;
  }

  public BlockAdapter getOmmerAt(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    int index = environment.getArgument("index");
    List<Hash> ommers = blockWithMetaData.getOmmers();
    Hash ommer = ommers.get(index);
    if (ommer != null) {
      BlockWithMetadata<TransactionWithMetadata, Hash> block = query.blockByHash(ommer).get();
      if (block != null) {
        return new BlockAdapter(block);
      }
    }
    return null;
  }

  public Bytes32 getOmmerHash() {
    return blockWithMetaData.getHeader().getOmmersHash();
  }

  public List<TransactionAdapter> getTransactions() {
    List<TransactionWithMetadata> trans = blockWithMetaData.getTransactions();
    List<TransactionAdapter> results = new ArrayList<TransactionAdapter>();
    for (TransactionWithMetadata tran : trans) {
      results.add(new TransactionAdapter(tran));
    }
    if (results.size() > 0) {
      return results;
    }
    return null;
  }

  public TransactionAdapter getTransactionAt(final DataFetchingEnvironment environment) {
    int index = environment.getArgument("index");
    List<TransactionWithMetadata> trans = blockWithMetaData.getTransactions();
    TransactionWithMetadata tran = trans.get(index);
    if (tran != null) {
      return new TransactionAdapter(tran);
    }
    return null;
  }

  public AccountAdapter getAccount(final DataFetchingEnvironment environment) {

    BlockchainQuery query = getBlockchainQuery(environment);
    long bn = blockWithMetaData.getHeader().getNumber();
    MutableWorldState ws = query.getWorldState(bn).get();

    if (ws != null) {
      Address addr = environment.getArgument("address");
      return new AccountAdapter(ws.get(addr));
    }
    return null;
  }

  public List<LogAdapter> getLogs(final DataFetchingEnvironment environment) {
    /*   Map<String, Object> filters = environment.getArgument("filter");
    List<Address> addrs = filters.get("addresses");
    List<List<Bytes32>>   topics = filters.get("topics");*/
    return null;
  }
  /*


  # Call executes a local call operation at the current block's state.
  call(data: CallData!): CallResult
  # EstimateGas estimates the amount of gas that will be required for
  # successful execution of a transaction at the current block's state.
  estimateGas(data: CallData!): Long!
  */

}