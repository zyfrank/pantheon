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

import tech.pegasys.pantheon.ethereum.core.Hash;

import java.util.Optional;

import com.google.common.primitives.UnsignedLong;
import graphql.schema.DataFetcher;

// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;

public class GraphQLDataFetchers {
  public GraphQLDataFetchers(final BlockchainQuery blockchain) {
    this.blockchain = blockchain;
  }

  // private static final Logger LOG = LogManager.getLogger();

  private BlockchainQuery blockchain;

  public DataFetcher<BlockAdapter> getBlockParentDataFetcher() {
    return dataFetchingEnvironment -> {
      BlockAdapter block = dataFetchingEnvironment.getSource();
      Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> result =
          blockchain.blockByHash(block.getParentHash());
      return new BlockAdapter(result.get());
    };
  }

  public DataFetcher<BlockAdapter> getBlockDataFetcher() {

    return dataFetchingEnvironment -> {
      UnsignedLong number = dataFetchingEnvironment.getArgument("number");
      Optional<BlockWithMetadata<TransactionWithMetadata, Hash>> result;

      if (number != null) {
        result = blockchain.blockByNumber(number.longValue());
      } else {

        String hash = dataFetchingEnvironment.getArgument("hash");
        if (hash != null) {
          result = blockchain.blockByHash(Hash.fromHexString(hash));
        } else {
          result = blockchain.latestBlock();
        }
      }

      BlockAdapter block = new BlockAdapter(result.get());
      return block;
    };
  }
}
