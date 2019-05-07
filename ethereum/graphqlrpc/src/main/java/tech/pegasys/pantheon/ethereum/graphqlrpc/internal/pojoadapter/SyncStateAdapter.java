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

import tech.pegasys.pantheon.ethereum.core.SyncStatus;

import java.util.Optional;

public class SyncStateAdapter {
  private final SyncStatus syncStatus;

  public SyncStateAdapter(final SyncStatus syncStatus) {
    this.syncStatus = syncStatus;
  }

  @SuppressWarnings("unused")
  public Optional<Long> getStartingBlock() {
    return Optional.of(syncStatus.getStartingBlock());
  }

  @SuppressWarnings("unused")
  public Optional<Long> getCurrentBlock() {
    return Optional.of(syncStatus.getCurrentBlock());
  }

  @SuppressWarnings("unused")
  public Optional<Long> getHighestBlock() {
    return Optional.of(syncStatus.getHighestBlock());
  }

  @SuppressWarnings("unused")
  public Optional<Long> getPulledStates() {
    // currently synchronizer has no this information
    return Optional.empty();
  }

  @SuppressWarnings("unused")
  public Optional<Long> getKnownStates() {
    // currently synchronizer has no this information
    return Optional.empty();
  }
}
