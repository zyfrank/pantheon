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
import tech.pegasys.pantheon.ethereum.core.TransactionReceipt;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.LogWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.TransactionReceiptWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.TransactionWithMetadata;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.primitives.UnsignedLong;
import graphql.schema.DataFetchingEnvironment;

public class TransactionAdapter extends AdapterBase {
  private TransactionWithMetadata transactionWithMetadata;

  public TransactionAdapter(final TransactionWithMetadata transactionWithMetadata) {
    this.transactionWithMetadata = transactionWithMetadata;
  }

  public Optional<Hash> getHash() {
    return Optional.of(transactionWithMetadata.getTransaction().hash());
  }

  public Optional<UnsignedLong> getNonce() {
    long nonce = transactionWithMetadata.getTransaction().getNonce();
    return Optional.of(UnsignedLong.valueOf(nonce));
  }

  public Optional<Integer> getIndex() {
    return Optional.of(transactionWithMetadata.getTransactionIndex());
  }

  public Optional<AccountAdapter> getFrom(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    UnsignedLong bn = environment.getArgument("block");

    Optional<MutableWorldState> ws = query.getWorldState(bn.longValue());
    if (ws.isPresent()) {
      return Optional.of(
          new AccountAdapter(ws.get().get(transactionWithMetadata.getTransaction().getSender())));
    }
    return Optional.empty();
  }

  public Optional<AccountAdapter> getTo(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    UnsignedLong to = environment.getArgument("block");

    Optional<MutableWorldState> ws = query.getWorldState(to.longValue());
    if (ws.isPresent()) {
      return Optional.of(
          new AccountAdapter(ws.get().get(transactionWithMetadata.getTransaction().getSender())));
    }
    return Optional.empty();
  }

  public Optional<UInt256> getValue() {
    return Optional.of(transactionWithMetadata.getTransaction().getValue().asUInt256());
  }

  public Optional<UInt256> getGasPrice() {
    return Optional.of(transactionWithMetadata.getTransaction().getGasPrice().asUInt256());
  }

  public Optional<UnsignedLong> getGas() {
    return Optional.of(
        UnsignedLong.valueOf(transactionWithMetadata.getTransaction().getGasLimit()));
  }

  public Optional<BytesValue> getInputData() {
    return Optional.of(transactionWithMetadata.getTransaction().getPayload());
  }

  public Optional<BlockAdapter> getBlock(final DataFetchingEnvironment environment) {
    long blockNumber = transactionWithMetadata.getBlockNumber();
    BlockchainQuery query = getBlockchainQuery(environment);
    Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> block =
        query.blockByNumber(blockNumber);
    return block.map(item -> new BlockAdapter(item));
  }

  public Optional<UnsignedLong> getStatus(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    Optional<TransactionReceiptWithMetadata> rpt =
        query.transactionReceiptByTransactionHash(transactionWithMetadata.getTransaction().hash());
    Optional<UnsignedLong> result = Optional.empty();
    if (rpt.isPresent()) {
      TransactionReceipt receipt = rpt.get().getReceipt();
      result = Optional.of(UnsignedLong.valueOf(receipt.getStatus()));
    }
    return result;
  }

  public Optional<UnsignedLong> getGasUsed(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    Optional<TransactionReceiptWithMetadata> rpt =
        query.transactionReceiptByTransactionHash(transactionWithMetadata.getTransaction().hash());
    if (rpt.isPresent()) {
      return Optional.of(UnsignedLong.valueOf(rpt.get().getGasUsed()));
    }
    return Optional.empty();
  }

  public Optional<UnsignedLong> getCumulativeGasUsed(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    Optional<TransactionReceiptWithMetadata> rpt =
        query.transactionReceiptByTransactionHash(transactionWithMetadata.getTransaction().hash());
    if (rpt.isPresent()) {
      TransactionReceipt receipt = rpt.get().getReceipt();
      return Optional.of(UnsignedLong.valueOf(receipt.getCumulativeGasUsed()));
    }
    return Optional.empty();
  }

  public Optional<AccountAdapter> getCreatedContract(final DataFetchingEnvironment environment) {
    boolean contractCreated = transactionWithMetadata.getTransaction().isContractCreation();
    if (contractCreated) {
      Optional<Address> addr = transactionWithMetadata.getTransaction().getTo();
      if (addr.isPresent()) {
        BlockchainQuery query = getBlockchainQuery(environment);
        UnsignedLong bn = environment.getArgument("block");
        Optional<MutableWorldState> ws = query.getWorldState(bn.longValue());
        if (ws.isPresent()) {
          return Optional.of(new AccountAdapter(ws.get().get(addr.get())));
        }
      }
    }
    return Optional.empty();
  }

  public List<LogAdapter> getLogs(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    Hash hash = transactionWithMetadata.getTransaction().hash();
    Optional<TransactionReceiptWithMetadata> tranRpt =
        query.transactionReceiptByTransactionHash(hash);
    List<LogAdapter> results = new ArrayList<LogAdapter>();
    if (tranRpt.isPresent()) {
      List<LogWithMetadata> logs =
          BlockchainQuery.generateLogWithMetadataForTransaction(
              tranRpt.get().getReceipt(),
              transactionWithMetadata.getBlockNumber(),
              transactionWithMetadata.getBlockHash(),
              hash,
              transactionWithMetadata.getTransactionIndex(),
              false);
      for (LogWithMetadata log : logs) {
        results.add(new LogAdapter(log));
      }
    }
    return results;
  }
}