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
import tech.pegasys.pantheon.ethereum.eth.EthProtocol;
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

  public DataFetcher<Integer> getProtocolVersionDataFetcher() {
    return dataFetchingEnvironment -> {
      return highestEthVersion;
    };
  }

  public DataFetcher<UInt256> getGasPriceDataFetcher() {
    return dataFetchingEnvironment -> {
      MiningCoordinator miningCoordinator =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getMiningCoordinator();

      return miningCoordinator.getMinTransactionGasPrice().asUInt256();
    };
  }

  public DataFetcher<List<BlockAdapter>> getRangeBlockDataFetcher() {

    return dataFetchingEnvironment -> {
      // UnsignedLong from = dataFetchingEnvironment.getArgument("from");
      // UnsignedLong to = dataFetchingEnvironment.getArgument("to");
      // to do add checking

      List<BlockAdapter> results = new ArrayList<BlockAdapter>();

      return results;
    };
  }

  public DataFetcher<BlockAdapter> getBlockDataFetcher() {

    return dataFetchingEnvironment -> {
      BlockchainQuery blockchain =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getBlockchainQuery();
      UnsignedLong number = dataFetchingEnvironment.getArgument("number");
      Bytes32 hash = dataFetchingEnvironment.getArgument("hash");
      if ((number != null) && (hash != null)) {
        throw new CustomException(GraphQLRpcError.INVALID_PARAMS);
      }

      Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> result;
      if (number != null) {
        result = blockchain.blockByNumber(number.longValue());
      } else {
        result = blockchain.blockByHash(Hash.wrap(hash));
      }
      if (result.get() == null) {
        result = blockchain.latestBlock();
      }
      BlockAdapter block = new BlockAdapter(result.get());
      return block;
    };
  }

  public DataFetcher<TransactionAdapter> getTransactionDataFetcher() {
    return dataFetchingEnvironment -> {
      BlockchainQuery blockchain =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getBlockchainQuery();

      Hash hash = dataFetchingEnvironment.getArgument("hash");
      Optional<TransactionWithMetadata> result = blockchain.transactionByHash(hash);
      return new TransactionAdapter(result.get());
    };
  }
}
