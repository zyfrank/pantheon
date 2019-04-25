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

import tech.pegasys.pantheon.ethereum.blockcreation.MiningCoordinator;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.SyncStatus;
import tech.pegasys.pantheon.ethereum.core.Synchronizer;
import tech.pegasys.pantheon.ethereum.eth.EthProtocol;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.TransactionWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter.BlockAdapter;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter.SyncStateAdapter;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter.TransactionAdapter;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.response.GraphQLRpcError;
import tech.pegasys.pantheon.ethereum.p2p.wire.Capability;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import com.google.common.primitives.UnsignedLong;
import graphql.schema.DataFetcher;

public class GraphQLDataFetchers {
  public GraphQLDataFetchers(final Set<Capability> supportedCapabilities) {
    final OptionalInt version =
        supportedCapabilities.stream()
            .filter(cap -> EthProtocol.NAME.equals(cap.getName()))
            .mapToInt(Capability::getVersion)
            .max();
    highestEthVersion = version.isPresent() ? version.getAsInt() : null;
  }

  private final Integer highestEthVersion;

  public DataFetcher<Optional<Integer>> getProtocolVersionDataFetcher() {
    return dataFetchingEnvironment -> {
      return Optional.of(highestEthVersion);
    };
  }

  public DataFetcher<Optional<SyncStateAdapter>> getSyncingDataFetcher() {
    return dataFetchingEnvironment -> {
      Synchronizer synchronizer =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getSynchronizer();
      Optional<SyncStatus> syncStatus = synchronizer.getSyncStatus();
      return syncStatus.map(item -> new SyncStateAdapter(item));
    };
  }

  public DataFetcher<Optional<UInt256>> getGasPriceDataFetcher() {
    return dataFetchingEnvironment -> {
      MiningCoordinator miningCoordinator =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getMiningCoordinator();

      return Optional.of(miningCoordinator.getMinTransactionGasPrice().asUInt256());
    };
  }

  public DataFetcher<List<BlockAdapter>> getRangeBlockDataFetcher() {

    return dataFetchingEnvironment -> {
      long from = ((UnsignedLong) dataFetchingEnvironment.getArgument("from")).longValue();
      long to = ((UnsignedLong) dataFetchingEnvironment.getArgument("to")).longValue();
      if (from > to) {
        throw new CustomException(GraphQLRpcError.INVALID_PARAMS);
      }

      BlockchainQuery blockchain =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getBlockchainQuery();

      List<BlockAdapter> results = new ArrayList<BlockAdapter>();
      for (long i = from; i <= to; i++) {
        BlockWithMetadata<TransactionWithMetadata, Hash> block = blockchain.blockByNumber(i).get();
        if (block != null) {
          results.add(new BlockAdapter(block));
        }
      }
      return results;
    };
  }

  public DataFetcher<Optional<BlockAdapter>> getBlockDataFetcher() {

    return dataFetchingEnvironment -> {
      BlockchainQuery blockchain =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getBlockchainQuery();
      UnsignedLong number = dataFetchingEnvironment.getArgument("number");
      Bytes32 hash = dataFetchingEnvironment.getArgument("hash");
      if ((number != null) && (hash != null)) {
        throw new CustomException(GraphQLRpcError.INVALID_PARAMS);
      }

      Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> block;
      if (number != null) {
        block = blockchain.blockByNumber(number.longValue());
      } else {
        block = blockchain.blockByHash(Hash.wrap(hash));
      }
      if (!block.isPresent()) {
        block = blockchain.latestBlock();
      }
      return block.map(item -> new BlockAdapter(item));
    };
  }

  public DataFetcher<Optional<TransactionAdapter>> getTransactionDataFetcher() {
    return dataFetchingEnvironment -> {
      BlockchainQuery blockchain =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getBlockchainQuery();
      Bytes32 hash = dataFetchingEnvironment.getArgument("hash");
      Optional<TransactionWithMetadata> tran = blockchain.transactionByHash(Hash.wrap(hash));
      return tran.map(item -> new TransactionAdapter(item));
    };
  }
}
