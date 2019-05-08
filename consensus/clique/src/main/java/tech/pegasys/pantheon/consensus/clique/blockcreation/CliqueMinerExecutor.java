/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.pantheon.consensus.clique.blockcreation;

import tech.pegasys.pantheon.consensus.clique.CliqueContext;
import tech.pegasys.pantheon.consensus.clique.CliqueExtraData;
import tech.pegasys.pantheon.consensus.common.ConsensusHelpers;
import tech.pegasys.pantheon.consensus.common.EpochManager;
import tech.pegasys.pantheon.consensus.common.VoteTally;
import tech.pegasys.pantheon.crypto.SECP256K1.KeyPair;
import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.blockcreation.AbstractBlockScheduler;
import tech.pegasys.pantheon.ethereum.blockcreation.AbstractMinerExecutor;
import tech.pegasys.pantheon.ethereum.chain.MinedBlockObserver;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.MiningParameters;
import tech.pegasys.pantheon.ethereum.core.Util;
import tech.pegasys.pantheon.ethereum.eth.transactions.PendingTransactions;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.util.Subscribers;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

public class CliqueMinerExecutor extends AbstractMinerExecutor<CliqueContext, CliqueBlockMiner> {

  private final Address localAddress;
  private final KeyPair nodeKeys;
  private final EpochManager epochManager;

  public CliqueMinerExecutor(
      final ProtocolContext<CliqueContext> protocolContext,
      final ExecutorService executorService,
      final ProtocolSchedule<CliqueContext> protocolSchedule,
      final PendingTransactions pendingTransactions,
      final KeyPair nodeKeys,
      final MiningParameters miningParams,
      final AbstractBlockScheduler blockScheduler,
      final EpochManager epochManager) {
    super(
        protocolContext,
        executorService,
        protocolSchedule,
        pendingTransactions,
        miningParams,
        blockScheduler);
    this.nodeKeys = nodeKeys;
    this.localAddress = Util.publicKeyToAddress(nodeKeys.getPublicKey());
    this.epochManager = epochManager;
  }

  @Override
  public CliqueBlockMiner startAsyncMining(
      final Subscribers<MinedBlockObserver> observers, final BlockHeader parentHeader) {
    final CliqueBlockCreator blockCreator =
        new CliqueBlockCreator(
            localAddress, // TOOD(tmm): This can be removed (used for voting not coinbase).
            this::calculateExtraData,
            pendingTransactions,
            protocolContext,
            protocolSchedule,
            (gasLimit) -> gasLimit,
            nodeKeys,
            minTransactionGasPrice,
            parentHeader,
            epochManager);

    final CliqueBlockMiner currentRunningMiner =
        new CliqueBlockMiner(
            blockCreator,
            protocolSchedule,
            protocolContext,
            observers,
            blockScheduler,
            parentHeader,
            localAddress);
    executorService.execute(currentRunningMiner);
    return currentRunningMiner;
  }

  @VisibleForTesting
  BytesValue calculateExtraData(final BlockHeader parentHeader) {
    final List<Address> validators = Lists.newArrayList();

    final BytesValue vanityDataToInsert =
        ConsensusHelpers.zeroLeftPad(extraData, CliqueExtraData.EXTRA_VANITY_LENGTH);
    // Building ON TOP of canonical head, if the next block is epoch, include validators.
    if (epochManager.isEpochBlock(parentHeader.getNumber() + 1)) {
      final VoteTally voteTally =
          protocolContext
              .getConsensusState()
              .getVoteTallyCache()
              .getVoteTallyAfterBlock(parentHeader);
      validators.addAll(voteTally.getValidators());
    }

    final CliqueExtraData extraData = new CliqueExtraData(vanityDataToInsert, null, validators);

    return extraData.encode();
  }
}
