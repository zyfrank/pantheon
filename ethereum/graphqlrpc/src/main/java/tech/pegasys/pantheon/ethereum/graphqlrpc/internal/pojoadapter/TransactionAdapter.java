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

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.TransactionWithMetadata;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import com.google.common.primitives.UnsignedLong;
import graphql.schema.DataFetchingEnvironment;

public class TransactionAdapter extends AdapterBase {
  private TransactionWithMetadata transactionWithMetadata;

  public TransactionAdapter(final TransactionWithMetadata transactionWithMetadata) {
    this.transactionWithMetadata = transactionWithMetadata;
  }

  public Hash getHash() {
    return transactionWithMetadata.getTransaction().hash();
  }

  public long getNonce() {
    return transactionWithMetadata.getTransaction().getNonce();
  }

  public int getIndex() {
    return 1;
  }
  //  # Index is the index of this transaction in the parent block. This will
  //   # be null if the transaction has not yet been mined.
  //  index: Int
  /*
    private BlockchainQuery getBlockchainQuery(final DataFetchingEnvironment environment) {
      return ((GraphQLDataFetcherContext) environment.getContext()).getBlockchainQuery();
    }
  */
  public AccountAdapter getFrom(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    UnsignedLong from = environment.getArgument("from");

    MutableWorldState ws = query.getWorldState(from.longValue()).get();
    if (ws != null) {
      return new AccountAdapter(ws.get(transactionWithMetadata.getTransaction().getSender()));
    }
    return null;
  }

  public AccountAdapter getTo(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    UnsignedLong to = environment.getArgument("to");

    MutableWorldState ws = query.getWorldState(to.longValue()).get();
    if (ws != null) {
      return new AccountAdapter(ws.get(transactionWithMetadata.getTransaction().getSender()));
    }
    return null;
  }

  public UInt256 getValue() {
    return transactionWithMetadata.getTransaction().getValue().asUInt256();
  }

  public UInt256 getGasPrice() {
    return transactionWithMetadata.getTransaction().getGasPrice().asUInt256();
  }

  public UnsignedLong getGas() {
    return UnsignedLong.valueOf(transactionWithMetadata.getTransaction().getGasLimit());
  }

  public BytesValue getInputData() {
    return transactionWithMetadata.getTransaction().getPayload();
  }

  public BlockAdapter getBlock(final DataFetchingEnvironment environment) {
    long blockNumber = transactionWithMetadata.getBlockNumber();
    BlockchainQuery query = getBlockchainQuery(environment);
    BlockWithMetadata<TransactionWithMetadata, Hash> block = query.blockByNumber(blockNumber).get();
    if (block != null) {
      return new BlockAdapter(block);
    }
    return null;
  }

  // public UnsignedLong getStatus() {
  //
  // }
  /*  # Status is the return status of the transaction. This will be 1 if the
  # transaction succeeded, or 0 if it failed (due to a revert, or due to
  # running out of gas). If the transaction has not yet been mined, this
  # field will be null.
  status: Long*/
  /*
  	public UnsignedLong getGasUsed(){
  long blockNumber = transactionWithMetadata.getBlockNumber();
      BlockchainQuery query = getBlockchainQuery(environment);
  BlockWithMetadata block = (query.blockByNumber(blockNumber).get();
  return UnsignedLong.valueOf(block.getHeader().getGasUsed());
  	}
  */
  /*
     # CumulativeGasUsed is the total gas used in the block up to and including
     # this transaction. If the transaction has not yet been mined, this field
     # will be null.
  cumulativeGasUsed: Long

  # CreatedContract is the account that was created by a contract creation
     # transaction. If the transaction was not a contract creation transaction,
     # or it has not yet been mined, this field will be null.
     createdContract(block: Long): Account
     # Logs is a list of log entries emitted by this transaction. If the
     # transaction has not yet been mined, this field will be null.
  logs: [Log!]
  */
  // public List<LogAdapter> getLogs(){

  // }
}
