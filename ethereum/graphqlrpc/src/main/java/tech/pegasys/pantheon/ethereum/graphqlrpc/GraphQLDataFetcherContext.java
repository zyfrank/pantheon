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
import tech.pegasys.pantheon.ethereum.chain.Blockchain;
import tech.pegasys.pantheon.ethereum.core.Synchronizer;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateArchive;

public class GraphQLDataFetcherContext {

  private BlockchainQuery blockchain;
  private MiningCoordinator miningCoordinator;
  private Synchronizer synchronizer;

  public GraphQLDataFetcherContext(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final MiningCoordinator miningCoordinator,
      final Synchronizer synchronizer) {
    this.blockchain = new BlockchainQuery(blockchain, worldStateArchive);
    this.miningCoordinator = miningCoordinator;
    this.synchronizer = synchronizer;
  }

  public BlockchainQuery getBlockchainQuery() {
    return blockchain;
  }

  public MiningCoordinator getMiningCoordinator() {
    return miningCoordinator;
  }

  public Synchronizer getSynchronizer() {
    return synchronizer;
  }
}
