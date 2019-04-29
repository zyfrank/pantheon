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
package tech.pegasys.pantheon.ethereum.graphqlrpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.pegasys.pantheon.ethereum.core.InMemoryStorageProvider.createInMemoryWorldStateArchive;

import tech.pegasys.pantheon.ethereum.blockcreation.EthHashMiningCoordinator;
import tech.pegasys.pantheon.ethereum.chain.Blockchain;
import tech.pegasys.pantheon.ethereum.chain.GenesisState;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.Synchronizer;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.eth.EthProtocol;
import tech.pegasys.pantheon.ethereum.eth.transactions.TransactionPool;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.TransactionWithMetadata;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockHashFunction;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.p2p.api.P2PNetwork;
import tech.pegasys.pantheon.ethereum.p2p.wire.Capability;
import tech.pegasys.pantheon.ethereum.util.RawBlockIterator;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateArchive;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GraphQLRpcHttpServiceTest {

  @ClassRule public static final TemporaryFolder folder = new TemporaryFolder();

  private static final Vertx vertx = Vertx.vertx();

  protected static GraphQLRpcHttpService service;
  protected static OkHttpClient client;
  protected static String baseUrl;
  protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  protected static final String CLIENT_VERSION = "TestClientVersion/0.1.0";
  protected static final int CHAIN_ID = 123;
  protected static P2PNetwork peerDiscoveryMock;
  protected static BlockchainQuery blockchainQueries;
  protected static Synchronizer synchronizer;
  protected static GraphQL graphQL;
  protected static GraphQLDataFetchers dataFetchers;
  protected static GraphQLDataFetcherContext dataFetcherContext;
  protected static EthHashMiningCoordinator miningCoordinatorMock;
  // protected static MutableBlockchain blockchain;
  protected static Blockchain blockchain;
  protected static WorldStateArchive stateArchive;
  protected static Block GENESIS_BLOCK;
  protected static GenesisState GENESIS_CONFIG;
  protected static List<Block> BLOCKS;
  protected static ProtocolSchedule<Void> PROTOCOL_SCHEDULE;

  // protected static final Collection<RpcApi> JSON_RPC_APIS =
  // Arrays.asList(RpcApis.ETH, RpcApis.NET, RpcApis.WEB3,
  // RpcApis.ADMIN);
  protected final GraphQLRpcTestHelper testHelper = new GraphQLRpcTestHelper();

  @BeforeClass
  public static void initServerAndClient() throws Exception {
    peerDiscoveryMock = mock(P2PNetwork.class);
    blockchainQueries = mock(BlockchainQuery.class);
    synchronizer = mock(Synchronizer.class);
    graphQL = mock(GraphQL.class);
    stateArchive = createInMemoryWorldStateArchive();
    // GENESIS_CONFIG.writeStateTo(stateArchive.getMutable());

    // blockchain = createInMemoryBlockchain(GENESIS_BLOCK);
    blockchain = mock(Blockchain.class);
    miningCoordinatorMock = mock(EthHashMiningCoordinator.class);
    // dataFetcherContext = new GraphQLDataFetcherContext(blockchain, stateArchive,
    // PROTOCOL_SCHEDULE, mock(TransactionPool.class), miningCoordinatorMock,
    // synchronizer);
    dataFetcherContext = mock(GraphQLDataFetcherContext.class);
    when(dataFetcherContext.getBlockchainQuery()).thenReturn(blockchainQueries);
    when(dataFetcherContext.getMiningCoordinator()).thenReturn(miningCoordinatorMock);
    // when(dataFetcherContext.getProtocolSchedule()).thenReturn(PROTOCOL_SCHEDULE);
    when(dataFetcherContext.getTransactionPool()).thenReturn(mock(TransactionPool.class));
    when(dataFetcherContext.getSynchronizer()).thenReturn(synchronizer);

    final Set<Capability> supportedCapabilities = new HashSet<>();
    supportedCapabilities.add(EthProtocol.ETH62);
    supportedCapabilities.add(EthProtocol.ETH63);
    dataFetchers = new GraphQLDataFetchers(supportedCapabilities);
    graphQL = GraphQLProvider.buildGraphQL(dataFetchers);
    service = createGraphQLRpcHttpService();
    service.start().join();
    // Build an OkHttp client.
    client = new OkHttpClient();
    baseUrl = service.url();
  }

  private static GraphQLRpcHttpService createGraphQLRpcHttpService(
      final GraphQLRpcConfiguration config) throws Exception {
    return new GraphQLRpcHttpService(
        vertx,
        folder.newFolder().toPath(),
        config,
        graphQL,
        dataFetcherContext,
        new NoOpMetricsSystem());
  }

  private static GraphQLRpcHttpService createGraphQLRpcHttpService() throws Exception {
    return new GraphQLRpcHttpService(
        vertx,
        folder.newFolder().toPath(),
        createGraphQLRpcConfig(),
        graphQL,
        dataFetcherContext,
        new NoOpMetricsSystem());
  }

  private static GraphQLRpcConfiguration createGraphQLRpcConfig() {
    final GraphQLRpcConfiguration config = GraphQLRpcConfiguration.createDefault();
    config.setPort(0);
    return config;
  }

  @BeforeClass
  public static void setupConstants() throws Exception {
    PROTOCOL_SCHEDULE = MainnetProtocolSchedule.create();

    final URL blocksUrl =
        GraphQLRpcHttpServiceTest.class
            .getClassLoader()
            .getResource(
                "tech/pegasys/pantheon/ethereum/graphqlrpc/graphQLRpcTestBlockchain.blocks");

    final URL genesisJsonUrl =
        GraphQLRpcHttpServiceTest.class
            .getClassLoader()
            .getResource("tech/pegasys/pantheon/ethereum/graphqlrpc/graphQLRpcTestGenesis.json");

    assertThat(blocksUrl).isNotNull();
    assertThat(genesisJsonUrl).isNotNull();

    BLOCKS = new ArrayList<>();
    try (final RawBlockIterator iterator =
        new RawBlockIterator(
            Paths.get(blocksUrl.toURI()),
            rlp -> BlockHeader.readFrom(rlp, MainnetBlockHashFunction::createHash))) {
      while (iterator.hasNext()) {
        BLOCKS.add(iterator.next());
      }
    }

    final String genesisJson = Resources.toString(genesisJsonUrl, Charsets.UTF_8);

    GENESIS_BLOCK = BLOCKS.get(0);
    GENESIS_CONFIG = GenesisState.fromJson(genesisJson, PROTOCOL_SCHEDULE);
  }

  /** Tears down the HTTP server. */
  @AfterClass
  public static void shutdownServer() {
    service.stop().join();
  }

  @Test
  public void invalidCallToStart() {
    service
        .start()
        .whenComplete(
            (unused, exception) -> {
              assertThat(exception).isInstanceOf(IllegalStateException.class);
            });
  }

  @Test
  public void http404() throws Exception {
    try (final Response resp = client.newCall(buildGetRequest("/foo")).execute()) {
      assertThat(resp.code()).isEqualTo(404);
    }
  }

  @Test
  public void handleEmptyRequest() throws Exception {
    try (final Response resp = client.newCall(buildGetRequest("")).execute()) {
      assertThat(resp.code()).isEqualTo(201);
    }
  }

  @Test
  public void handleInvalidQuerySchema() throws Exception {
    RequestBody body = RequestBody.create(JSON, "{gasPrice1}");

    try (final Response resp = client.newCall(buildPostRequest(body)).execute()) {
      // assertThat(resp.code()).isEqualTo(200); // Check general format of result
      final JsonObject json = new JsonObject(resp.body().string());
      testHelper.assertValidGraphQLRpcError(json); // Check result final
    }
  }

  @Test
  public void getGasprice() throws Exception {
    RequestBody body = RequestBody.create(JSON, "{gasPrice}");
    Wei price = Wei.of(16);
    when(miningCoordinatorMock.getMinTransactionGasPrice()).thenReturn(price);

    try (final Response resp = client.newCall(buildPostRequest(body)).execute()) {
      assertThat(resp.code()).isEqualTo(200); // Check general format of result
      final JsonObject json = new JsonObject(resp.body().string());
      testHelper.assertValidGraphQLRpcResult(json);
      String result = json.getJsonObject("data").getString("gasPrice");
      assertThat(result).isEqualTo("0x10");
    }
  }

  @Test
  public void getSocketAddressWhenActive() {
    final InetSocketAddress socketAddress = service.socketAddress();
    assertThat("127.0.0.1").isEqualTo(socketAddress.getAddress().getHostAddress());
    assertThat(socketAddress.getPort() > 0).isTrue();
  }

  @Test
  public void getSocketAddressWhenStoppedIsEmpty() throws Exception {
    final GraphQLRpcHttpService service = createGraphQLRpcHttpService();

    final InetSocketAddress socketAddress = service.socketAddress();
    assertThat("0.0.0.0").isEqualTo(socketAddress.getAddress().getHostAddress());
    assertThat(0).isEqualTo(socketAddress.getPort());
    assertThat("").isEqualTo(service.url());
  }

  @Test
  public void getSocketAddressWhenBindingToAllInterfaces() throws Exception {
    final GraphQLRpcConfiguration config = createGraphQLRpcConfig();
    config.setHost("0.0.0.0");
    final GraphQLRpcHttpService service = createGraphQLRpcHttpService(config);
    service.start().join();

    try {
      final InetSocketAddress socketAddress = service.socketAddress();
      assertThat("0.0.0.0").isEqualTo(socketAddress.getAddress().getHostAddress());
      assertThat(socketAddress.getPort() > 0).isTrue();
      assertThat(!service.url().contains("0.0.0.0")).isTrue();
    } finally {
      service.stop().join();
    }
  }

  @Test
  public void responseContainsJsonContentTypeHeader() throws Exception {

    final RequestBody body = RequestBody.create(JSON, "{gasPrice}");

    try (final Response resp = client.newCall(buildPostRequest(body)).execute()) {
      assertThat(resp.header("Content-Type")).isEqualTo("application/json");
    }
  }

  @Test
  public void ethGetUncleCountByBlockHash() throws Exception {
    final int uncleCount = 4;
    final Hash blockHash = Hash.hash(BytesValue.of(1));
    @SuppressWarnings("unchecked")
    final BlockWithMetadata<TransactionWithMetadata, Hash> block = mock(BlockWithMetadata.class);
    @SuppressWarnings("unchecked")
    final List<Hash> list = mock(List.class);

    when(blockchainQueries.blockByHash(eq(blockHash))).thenReturn(Optional.of(block));
    when(block.getOmmers()).thenReturn(list);
    when(list.size()).thenReturn(uncleCount);

    final String query = "{block(hash:\"" + blockHash.toString() + "\") {ommerCount}}";

    final RequestBody body = RequestBody.create(JSON, query);
    try (final Response resp = client.newCall(buildPostRequest(body)).execute()) {
      assertThat(resp.code()).isEqualTo(200);
      final String jsonStr = resp.body().string();
      System.out.println(jsonStr);
      final JsonObject json = new JsonObject(jsonStr);
      testHelper.assertValidGraphQLRpcResult(json);
      int result = json.getJsonObject("data").getJsonObject("block").getInteger("ommerCount");
      assertThat(result).isEqualTo(uncleCount);
    }
  }

  @Test
  public void ethGetUncleCountByBlockNumber() throws Exception {
    final int uncleCount = 5;
    @SuppressWarnings("unchecked")
    final BlockWithMetadata<TransactionWithMetadata, Hash> block = mock(BlockWithMetadata.class);
    @SuppressWarnings("unchecked")
    final List<Hash> list = mock(List.class);
    when(blockchainQueries.blockByNumber(anyLong())).thenReturn(Optional.of(block));
    when(block.getOmmers()).thenReturn(list);
    when(list.size()).thenReturn(uncleCount);

    final String query = "{block(number:\"3\") {ommerCount}}";

    final RequestBody body = RequestBody.create(JSON, query);
    try (final Response resp = client.newCall(buildPostRequest(body)).execute()) {
      assertThat(resp.code()).isEqualTo(200);
      final String jsonStr = resp.body().string();
      System.out.println(jsonStr);
      final JsonObject json = new JsonObject(jsonStr);
      testHelper.assertValidGraphQLRpcResult(json);
      int result = json.getJsonObject("data").getJsonObject("block").getInteger("ommerCount");
      assertThat(result).isEqualTo(uncleCount);
    }
  }

  @Test
  public void ethGetUncleCountByBlockLatest() throws Exception {
    final int uncleCount = 5;
    @SuppressWarnings("unchecked")
    final BlockWithMetadata<TransactionWithMetadata, Hash> block = mock(BlockWithMetadata.class);
    @SuppressWarnings("unchecked")
    final List<Hash> list = mock(List.class);
    when(blockchainQueries.latestBlock()).thenReturn(Optional.of(block));
    when(block.getOmmers()).thenReturn(list);
    when(list.size()).thenReturn(uncleCount);

    final String query = "{block {ommerCount}}";

    final RequestBody body = RequestBody.create(JSON, query);
    try (final Response resp = client.newCall(buildPostRequest(body)).execute()) {
      assertThat(resp.code()).isEqualTo(200);
      final String jsonStr = resp.body().string();
      System.out.println(jsonStr);
      final JsonObject json = new JsonObject(jsonStr);
      testHelper.assertValidGraphQLRpcResult(json);
      int result = json.getJsonObject("data").getJsonObject("block").getInteger("ommerCount");
      assertThat(result).isEqualTo(uncleCount);
    }
  }

  private Request buildPostRequest(final RequestBody body) {
    return new Request.Builder().post(body).url(baseUrl).build();
  }

  private Request buildGetRequest(final String path) {
    return new Request.Builder().get().url(baseUrl + path).build();
  }
}