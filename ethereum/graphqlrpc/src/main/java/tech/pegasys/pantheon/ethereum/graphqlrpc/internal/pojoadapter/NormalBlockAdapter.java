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

import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.TransactionWithMetadata;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import graphql.schema.DataFetchingEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NormalBlockAdapter extends BlockAdapterBase {
  private static final Logger LOG = LogManager.getLogger();

  public NormalBlockAdapter(
      final BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetaData) {
    super(blockWithMetaData.getHeader());
    this.blockWithMetaData = blockWithMetaData;
  }

  private final BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetaData;

  public Optional<Integer> getTransactionCount() {
    return Optional.of(blockWithMetaData.getTransactions().size());
  }

  public Optional<UInt256> getTotalDifficulty() {
    return Optional.of(blockWithMetaData.getTotalDifficulty());
  }

  public Optional<Integer> getOmmerCount() {
    return Optional.of(blockWithMetaData.getOmmers().size());
  }

  public List<UncleBlockAdapter> getOmmers(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    List<Hash> ommers = blockWithMetaData.getOmmers();
    List<UncleBlockAdapter> results = new ArrayList<UncleBlockAdapter>();
    LOG.info("here  ommers size:  " + ommers.size());
    Hash hash = blockWithMetaData.getHeader().getHash();
    for (int i = 0; i < ommers.size(); i++) {
      Optional<BlockHeader> header = query.getOmmer(hash, i);
      LOG.info("here  ommers size:  " + ommers.size());
      header.ifPresent(
          item -> {
            results.add(new UncleBlockAdapter(item));
            ;
          });
    }

    return results;
  }

  public Optional<UncleBlockAdapter> getOmmerAt(final DataFetchingEnvironment environment) {
    BlockchainQuery query = getBlockchainQuery(environment);
    int index = environment.getArgument("index");
    List<Hash> ommers = blockWithMetaData.getOmmers();
    if (ommers.size() > index) {
      Hash hash = blockWithMetaData.getHeader().getHash();
      Optional<BlockHeader> header = query.getOmmer(hash, index);
      return header.map(item -> new UncleBlockAdapter(item));
    }
    return Optional.empty();
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
}
