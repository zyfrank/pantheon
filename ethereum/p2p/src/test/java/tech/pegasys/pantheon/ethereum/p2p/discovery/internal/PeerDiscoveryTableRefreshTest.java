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
package tech.pegasys.pantheon.ethereum.p2p.discovery.internal;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import tech.pegasys.pantheon.crypto.SECP256K1;
import tech.pegasys.pantheon.crypto.SECP256K1.KeyPair;
import tech.pegasys.pantheon.ethereum.p2p.discovery.DiscoveryPeer;
import tech.pegasys.pantheon.ethereum.p2p.discovery.PeerDiscoveryStatus;
import tech.pegasys.pantheon.ethereum.p2p.discovery.PeerDiscoveryTestHelper;
import tech.pegasys.pantheon.ethereum.p2p.peers.PeerBlacklist;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.util.Subscribers;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PeerDiscoveryTableRefreshTest {
  private final PeerDiscoveryTestHelper helper = new PeerDiscoveryTestHelper();

  @Test
  public void tableRefreshSingleNode() {
    final List<SECP256K1.KeyPair> keypairs = PeerDiscoveryTestHelper.generateKeyPairs(2);
    final List<DiscoveryPeer> peers = helper.createDiscoveryPeers(keypairs);
    DiscoveryPeer localPeer = peers.get(0);
    KeyPair localKeyPair = keypairs.get(0);

    // Create and start the PeerDiscoveryController
    final OutboundMessageHandler outboundMessageHandler = mock(OutboundMessageHandler.class);
    final MockTimerUtil timer = new MockTimerUtil();
    final PeerDiscoveryController controller =
        spy(
            new PeerDiscoveryController(
                localKeyPair,
                localPeer,
                new PeerTable(localPeer.getId()),
                emptyList(),
                outboundMessageHandler,
                timer,
                new BlockingAsyncExecutor(),
                0,
                () -> true,
                new PeerBlacklist(),
                Optional.empty(),
                new Subscribers<>(),
                new Subscribers<>(),
                new NoOpMetricsSystem()));
    controller.start();

    // Send a PING, so as to add a Peer in the controller.
    final PingPacketData ping =
        PingPacketData.create(peers.get(1).getEndpoint(), peers.get(0).getEndpoint());
    final Packet pingPacket = Packet.create(PacketType.PING, ping, keypairs.get(1));
    controller.onMessage(pingPacket, peers.get(1));

    // Wait until the controller has added the newly found peer.
    assertThat(controller.getPeers()).hasSize(1);

    // Simulate a PONG message from peer 0.
    final PongPacketData pongPacketData =
        PongPacketData.create(localPeer.getEndpoint(), pingPacket.getHash());
    final Packet pongPacket = Packet.create(PacketType.PONG, pongPacketData, keypairs.get(0));

    final ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
    for (int i = 0; i < 5; i++) {

      controller.onMessage(pongPacket, peers.get(0));

      controller.getRecursivePeerRefreshState().cancel();
      timer.runPeriodicHandlers();
      controller.getPeers().forEach(p -> p.setStatus(PeerDiscoveryStatus.KNOWN));
      controller.onMessage(pingPacket, peers.get(1));
    }
    verify(outboundMessageHandler, atLeast(5)).send(eq(peers.get(1)), captor.capture());
    final List<Packet> capturedFindNeighborsPackets =
        captor.getAllValues().stream()
            .filter(p -> p.getType().equals(PacketType.FIND_NEIGHBORS))
            .collect(Collectors.toList());
    assertThat(capturedFindNeighborsPackets.size()).isEqualTo(5);

    // Collect targets from find neighbors packets
    final List<BytesValue> targets = new ArrayList<>();
    for (final Packet captured : capturedFindNeighborsPackets) {
      Optional<FindNeighborsPacketData> maybeData =
          captured.getPacketData(FindNeighborsPacketData.class);
      assertThat(maybeData).isPresent();
      final FindNeighborsPacketData neighborsData = maybeData.get();
      targets.add(neighborsData.getTarget());
    }

    assertThat(targets.size()).isEqualTo(5);

    // All targets are unique.
    assertThat(targets.size()).isEqualTo(new HashSet<>(targets).size());
  }
}
