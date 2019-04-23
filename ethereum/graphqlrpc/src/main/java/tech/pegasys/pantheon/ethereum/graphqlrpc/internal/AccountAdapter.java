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
package tech.pegasys.pantheon.ethereum.graphqlrpc.internal;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import com.google.common.primitives.UnsignedLong;
import graphql.schema.DataFetchingEnvironment;

public class AccountAdapter {
  private Account account;

  public AccountAdapter(final Account account) {
    this.account = account;
  }

  public Address getAddress() {
    return account.getAddress();
  }

  public UInt256 getBalance() {
    return account.getBalance().asUInt256();
  }

  public UnsignedLong getTransactionCount() {
    return UnsignedLong.valueOf(account.getNonce());
  }

  public BytesValue getCode() {
    return account.getCode();
  }

  public Bytes32 getStorage(final DataFetchingEnvironment environment) {
    Bytes32 slot = environment.getArgument("slot");
    return account.getStorageValue(slot.asUInt256()).getBytes();
  }
}
