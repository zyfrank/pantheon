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
package tech.pegasys.pantheon.ethereum.blockcreation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.chain.MinedBlockObserver;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockBody;
import tech.pegasys.pantheon.ethereum.core.BlockHeaderTestFixture;
import tech.pegasys.pantheon.ethereum.core.BlockImporter;
import tech.pegasys.pantheon.ethereum.mainnet.HeaderValidationMode;
import tech.pegasys.pantheon.ethereum.mainnet.MutableProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpec;
import tech.pegasys.pantheon.util.Subscribers;

import java.math.BigInteger;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.junit.Test;

public class BlockMinerTest {

  @Test
  @SuppressWarnings("unchecked")
  public void blockCreatedIsAddedToBlockChain() throws InterruptedException {
    final BlockHeaderTestFixture headerBuilder = new BlockHeaderTestFixture();

    final Block blockToCreate =
        new Block(
            headerBuilder.buildHeader(), new BlockBody(Lists.newArrayList(), Lists.newArrayList()));

    final ProtocolContext<Void> protocolContext = new ProtocolContext<>(null, null, null);

    final EthHashBlockCreator blockCreator = mock(EthHashBlockCreator.class);
    when(blockCreator.createBlock(anyLong())).thenReturn(blockToCreate);

    final BlockImporter<Void> blockImporter = mock(BlockImporter.class);
    final ProtocolSpec<Void> protocolSpec = mock(ProtocolSpec.class);

    final ProtocolSchedule<Void> protocolSchedule = singleSpecSchedule(protocolSpec);

    when(protocolSpec.getBlockImporter()).thenReturn(blockImporter);
    when(blockImporter.importBlock(any(), any(), any())).thenReturn(true);

    final MinedBlockObserver observer = mock(MinedBlockObserver.class);
    final DefaultBlockScheduler scheduler = mock(DefaultBlockScheduler.class);
    when(scheduler.waitUntilNextBlockCanBeMined(any())).thenReturn(5L);
    final BlockMiner<Void, EthHashBlockCreator> miner =
        new EthHashBlockMiner(
            blockCreator,
            protocolSchedule,
            protocolContext,
            subscribersContaining(observer),
            scheduler,
            headerBuilder.buildHeader()); // parent header is arbitrary for the test.

    miner.run();
    verify(blockImporter).importBlock(protocolContext, blockToCreate, HeaderValidationMode.FULL);
    verify(observer, times(1)).blockMined(blockToCreate);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void failureToImportDoesNotTriggerObservers() throws InterruptedException {
    final BlockHeaderTestFixture headerBuilder = new BlockHeaderTestFixture();

    final Block blockToCreate =
        new Block(
            headerBuilder.buildHeader(), new BlockBody(Lists.newArrayList(), Lists.newArrayList()));

    final ProtocolContext<Void> protocolContext = new ProtocolContext<>(null, null, null);

    final EthHashBlockCreator blockCreator = mock(EthHashBlockCreator.class);
    when(blockCreator.createBlock(anyLong())).thenReturn(blockToCreate);

    final BlockImporter<Void> blockImporter = mock(BlockImporter.class);
    final ProtocolSpec<Void> protocolSpec = mock(ProtocolSpec.class);
    final ProtocolSchedule<Void> protocolSchedule = singleSpecSchedule(protocolSpec);

    when(protocolSpec.getBlockImporter()).thenReturn(blockImporter);
    when(blockImporter.importBlock(any(), any(), any())).thenReturn(false, false, true);

    final MinedBlockObserver observer = mock(MinedBlockObserver.class);
    final DefaultBlockScheduler scheduler = mock(DefaultBlockScheduler.class);
    when(scheduler.waitUntilNextBlockCanBeMined(any())).thenReturn(5L);
    final BlockMiner<Void, EthHashBlockCreator> miner =
        new EthHashBlockMiner(
            blockCreator,
            protocolSchedule,
            protocolContext,
            subscribersContaining(observer),
            scheduler,
            headerBuilder.buildHeader()); // parent header is arbitrary for the test.

    miner.run();
    verify(blockImporter, times(3))
        .importBlock(protocolContext, blockToCreate, HeaderValidationMode.FULL);
    verify(observer, times(1)).blockMined(blockToCreate);
  }

  private static Subscribers<MinedBlockObserver> subscribersContaining(
      final MinedBlockObserver... observers) {
    final Subscribers<MinedBlockObserver> result = new Subscribers<>();
    for (final MinedBlockObserver obs : observers) {
      result.subscribe(obs);
    }
    return result;
  }

  private ProtocolSchedule<Void> singleSpecSchedule(final ProtocolSpec<Void> protocolSpec) {
    final MutableProtocolSchedule<Void> protocolSchedule =
        new MutableProtocolSchedule<>(Optional.of(BigInteger.valueOf(1234)));
    protocolSchedule.putMilestone(0, protocolSpec);
    return protocolSchedule;
  }
}
