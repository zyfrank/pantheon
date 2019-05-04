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
import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.core.SyncStatus;
import tech.pegasys.pantheon.ethereum.core.Synchronizer;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.eth.EthProtocol;
import tech.pegasys.pantheon.ethereum.eth.transactions.TransactionPool;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.TransactionWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter.AccountAdapter;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter.NormalBlockAdapter;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter.SyncStateAdapter;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter.TransactionAdapter;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.response.GraphQLRpcError;
import tech.pegasys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason;
import tech.pegasys.pantheon.ethereum.mainnet.ValidationResult;
import tech.pegasys.pantheon.ethereum.p2p.wire.Capability;
import tech.pegasys.pantheon.ethereum.rlp.RLP;
import tech.pegasys.pantheon.ethereum.rlp.RLPException;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

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

  DataFetcher<Optional<Integer>> getProtocolVersionDataFetcher() {
    return dataFetchingEnvironment -> Optional.of(highestEthVersion);
  }

  DataFetcher<Optional<Bytes32>> getSendRawTransactionDataFetcher() {
    return dataFetchingEnvironment -> {
      try {
        final TransactionPool transactionPool =
            ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getTransactionPool();
        final BytesValue rawTran = dataFetchingEnvironment.getArgument("data");

        final Transaction transaction = Transaction.readFrom(RLP.input(rawTran));
        final ValidationResult<TransactionInvalidReason> validationResult =
            transactionPool.addLocalTransaction(transaction);
        if (validationResult.isValid()) {
          return Optional.of(transaction.hash());
        }
      } catch (final IllegalArgumentException | RLPException e) {
        throw new CustomException(GraphQLRpcError.INVALID_PARAMS);
      }
      throw new CustomException(GraphQLRpcError.INVALID_PARAMS);
    };
  }

  DataFetcher<Optional<SyncStateAdapter>> getSyncingDataFetcher() {
    return dataFetchingEnvironment -> {
      final Synchronizer synchronizer =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getSynchronizer();
      final Optional<SyncStatus> syncStatus = synchronizer.getSyncStatus();
      return syncStatus.map(SyncStateAdapter::new);
    };
  }

  DataFetcher<Optional<UInt256>> getGasPriceDataFetcher() {
    return dataFetchingEnvironment -> {
      final MiningCoordinator miningCoordinator =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getMiningCoordinator();

      return Optional.of(miningCoordinator.getMinTransactionGasPrice().asUInt256());
    };
  }

  DataFetcher<List<NormalBlockAdapter>> getRangeBlockDataFetcher() {

    return dataFetchingEnvironment -> {
      final long from = dataFetchingEnvironment.getArgument("from");
      final long to = dataFetchingEnvironment.getArgument("to");
      if (from > to) {
        throw new CustomException(GraphQLRpcError.INVALID_PARAMS);
      }

      final BlockchainQuery blockchain =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getBlockchainQuery();

      final List<NormalBlockAdapter> results = new ArrayList<>();
      for (long i = from; i <= to; i++) {
        final BlockWithMetadata<TransactionWithMetadata, Hash> block =
            blockchain.blockByNumber(i).get();
        if (block != null) {
          results.add(new NormalBlockAdapter(block));
        }
      }
      return results;
    };
  }

  public DataFetcher<Optional<NormalBlockAdapter>> getBlockDataFetcher() {

    return dataFetchingEnvironment -> {
      final BlockchainQuery blockchain =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getBlockchainQuery();
      final Long number = dataFetchingEnvironment.getArgument("number");
      final Bytes32 hash = dataFetchingEnvironment.getArgument("hash");
      if ((number != null) && (hash != null)) {
        throw new CustomException(GraphQLRpcError.INVALID_PARAMS);
      }

      Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> block = Optional.empty();
      if (number != null) {
        block = blockchain.blockByNumber(number);
      } else {
        if (hash != null) {
          block = blockchain.blockByHash(Hash.wrap(hash));
        }
      }
      if (!block.isPresent()) {
        block = blockchain.latestBlock();
      }
      return block.map(NormalBlockAdapter::new);
    };
  }

  DataFetcher<AccountAdapter> getAccountDataFetcher() {
    return dataFetchingEnvironment -> {
      final BlockchainQuery blockchain =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getBlockchainQuery();
      final Address addr = dataFetchingEnvironment.getArgument("address");
      final Long bn = dataFetchingEnvironment.getArgument("blockNumber");
      if (bn != null) {
        final Optional<MutableWorldState> ws = blockchain.getWorldState(bn);
        if (ws.isPresent()) {
          return new AccountAdapter(ws.get().get(addr));
        } else {
          // invalid blocknumber
          throw new CustomException(GraphQLRpcError.INVALID_PARAMS);
        }
      }
      // return account on latest block
      final long latestBn = blockchain.latestBlock().get().getHeader().getNumber();
      final Optional<MutableWorldState> ws = blockchain.getWorldState(latestBn);
      if (ws.isPresent()) {
        final Account acc = ws.get().get(addr);
        if (acc != null) {
          return new AccountAdapter(acc);
        } else {
          throw new CustomException(GraphQLRpcError.INTERNAL_ERROR);
        }
      }
      // invalid blocknumber
      throw new CustomException(GraphQLRpcError.INVALID_PARAMS);
    };
  }

  DataFetcher<Optional<TransactionAdapter>> getTransactionDataFetcher() {
    return dataFetchingEnvironment -> {
      final BlockchainQuery blockchain =
          ((GraphQLDataFetcherContext) dataFetchingEnvironment.getContext()).getBlockchainQuery();
      final Bytes32 hash = dataFetchingEnvironment.getArgument("hash");
      final Optional<TransactionWithMetadata> tran = blockchain.transactionByHash(Hash.wrap(hash));
      return tran.map(TransactionAdapter::new);
    };
  }
}