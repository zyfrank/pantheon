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
package tech.pegasys.pantheon.ethereum.graphqlrpc.internal;

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.LogTopic;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.primitives.UnsignedLong;
import graphql.schema.DataFetchingEnvironment;

public class LogAdapter extends AdapterBase {
  private LogWithMetadata logWithMetadata;

  public LogAdapter(final LogWithMetadata logWithMetadata) {
    this.logWithMetadata = logWithMetadata;
  }

  public Integer getIndex() {
    return logWithMetadata.getLogIndex();
  }

  public List<Bytes32> getTopics() {
    List<LogTopic> topics = logWithMetadata.getTopics();
    List<Bytes32> result = new ArrayList<Bytes32>();
    for (LogTopic topic : topics) {
      result.add(Bytes32.leftPad(topic));
    }
    if (result.size() == 0) {
      return null;
    }
    return result;
  }

  public BytesValue getData() {
    return logWithMetadata.getData();
  }

  public TransactionAdapter getTransaction(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    Hash hash = logWithMetadata.getTransactionHash();
    Optional<TransactionWithMetadata> tran = query.transactionByHash(hash);
    if (tran.get() != null) {
      return new TransactionAdapter(tran.get());
    }
    return null;
  }

  public AccountAdapter getAccount(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    UnsignedLong blockNumber = environment.getArgument("block");
    Optional<MutableWorldState> ws = query.getWorldState(blockNumber.longValue());
    if (ws.get() != null) {
      Hash hash = logWithMetadata.getTransactionHash();
      Optional<TransactionWithMetadata> tran = query.transactionByHash(hash);
      if (tran.get() != null) {
        return new AccountAdapter(ws.get().get(tran.get().getTransaction().getSender()));
      }
    }
    return null;
  }
}
