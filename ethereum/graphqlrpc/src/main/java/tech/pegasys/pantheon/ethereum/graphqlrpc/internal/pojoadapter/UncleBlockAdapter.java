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
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import graphql.schema.DataFetchingEnvironment;

public class UncleBlockAdapter extends BlockAdapterBase {

  public UncleBlockAdapter(final BlockHeader uncleHeader) {
    super(uncleHeader);
  }

  public Optional<Integer> getTransactionCount() {
    return Optional.of(0);
  }

  public Optional<UInt256> getTotalDifficulty() {
    return Optional.of(UInt256.of(0));
  }

  public Optional<Integer> getOmmerCount() {
    return Optional.empty();
  }

  public List<NormalBlockAdapter> getOmmers(final DataFetchingEnvironment environment) {
    return new ArrayList<NormalBlockAdapter>();
  }

  public Optional<NormalBlockAdapter> getOmmerAt(final DataFetchingEnvironment environment) {
    return Optional.empty();
  }

  public List<TransactionAdapter> getTransactions() {
    return new ArrayList<TransactionAdapter>();
  }

  public Optional<TransactionAdapter> getTransactionAt(final DataFetchingEnvironment environment) {
    return Optional.empty();
  }
}
