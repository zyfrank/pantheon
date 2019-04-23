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

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.List;

import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLong;
import graphql.schema.DataFetchingEnvironment;

public class BlockAdapter {
  public BlockAdapter(final BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetaData) {
    this.blockWithMetaData = blockWithMetaData;
  }

  private final BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetaData;
  /*
  public Hash getParentHash() {
    return blockWithMetaData.getHeader().getParentHash();
  }
  */
  private BlockchainQuery getBlockchainQuery(final DataFetchingEnvironment environment) {
    return ((GraphQLDataFetcherContext) environment.getContext()).getBlockchainQuery();
  }

  public BlockAdapter getParent(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    Hash parentHash = blockWithMetaData.getHeader().getParentHash();
    return new BlockAdapter(query.blockByHash(parentHash).get());
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

  // miner(block: Long): Account!
  //	# ExtraData is an arbitrary data field supplied by the miner.
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
      results.add(new BlockAdapter(query.blockByHash(item).get()));
    }
    return results;
  }

  public BlockAdapter getOmmerAt(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    int index = environment.getArgument("index");
    List<Hash> ommers = blockWithMetaData.getOmmers();
    Hash result = ommers.get(index);
    if (result != null) {
      return new BlockAdapter(query.blockByHash(result).get());
    }
    return null;
  }

  public Bytes32 getOmmerHash() {
    return blockWithMetaData.getHeader().getOmmersHash();
  }
  /*
  # Transactions is a list of transactions associated with this block. If
  # transactions are unavailable for this block, this field will be null.
  transactions: [Transaction!]
  # TransactionAt returns the transaction at the specified index. If
  # transactions are unavailable for this block, or if the index is out of
  # bounds, this field will be null.
  transactionAt(index: Int!): Transaction
  # Logs returns a filtered set of logs from this block.
  logs(filter: BlockFilterCriteria!): [Log!]!
  # Account fetches an Ethereum account at the current block's state.
  account(address: Address!): Account!
  # Call executes a local call operation at the current block's state.
  call(data: CallData!): CallResult
  # EstimateGas estimates the amount of gas that will be required for
  # successful execution of a transaction at the current block's state.
  estimateGas(data: CallData!): Long!
  */

}
