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

import static com.google.common.base.Preconditions.checkArgument;
import static tech.pegasys.pantheon.util.NetworkUtility.urlForSocketAddress;

import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.response.GraphQLRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.response.GraphQLRpcResponse;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.response.GraphQLRpcResponseType;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.response.GraphQLRpcSuccessResponse;
import tech.pegasys.pantheon.util.NetworkUtility;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.annotations.VisibleForTesting;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GraphQLRpcHttpService {

  private static final Logger LOG = LogManager.getLogger();

  private static final InetSocketAddress EMPTY_SOCKET_ADDRESS = new InetSocketAddress("0.0.0.0", 0);
  private static final String APPLICATION_JSON = "application/json";
  // private static final GraphQLRpcResponse NO_RESPONSE = new
  // GraphQLRpcNoResponse();
  private static final String EMPTY_RESPONSE = "";

  private final Vertx vertx;
  private final GraphQLRpcConfiguration config;
  // private final Map<String, JsonRpcMethod> jsonRpcMethods;
  private final Path dataDir;

  private HttpServer httpServer;

  private final GraphQL graphQL;

  // as GraphQL dataFetcher context data
  private final GraphQLDataFetcherContext dataFetcherContext;

  /**
   * Construct a GraphQLRpcHttpService handler
   *
   * @param vertx The vertx process that will be running this service
   * @param dataDir The data directory where requests can be buffered
   * @param config Configuration for the rpc methods being loaded
   * @param graphQL GraphQL engine
   * @param dataFetcherContext DataFetcherContext required by GraphQL to finish it's job
   */
  public GraphQLRpcHttpService(
      final Vertx vertx,
      final Path dataDir,
      final GraphQLRpcConfiguration config,
      final GraphQL graphQL,
      final GraphQLDataFetcherContext dataFetcherContext) {
    this.dataDir = dataDir;

    validateConfig(config);
    this.config = config;
    this.vertx = vertx;
    this.graphQL = graphQL;
    this.dataFetcherContext = dataFetcherContext;
  }

  private void validateConfig(final GraphQLRpcConfiguration config) {
    checkArgument(
        config.getPort() == 0 || NetworkUtility.isValidPort(config.getPort()),
        "Invalid port configuration.");
    checkArgument(config.getHost() != null, "Required host is not configured.");
  }

  public CompletableFuture<?> start() {
    LOG.info("Starting GraphQLRPC service on {}:{}", config.getHost(), config.getPort());
    // Create the HTTP server and a router object.
    httpServer =
        vertx.createHttpServer(
            new HttpServerOptions().setHost(config.getHost()).setPort(config.getPort()));

    // Handle graphql rpc requests
    final Router router = Router.router(vertx);

    router
        .route()
        .handler(
            BodyHandler.create()
                .setUploadsDirectory(dataDir.resolve("uploads").toString())
                .setDeleteUploadedFilesOnEnd(true));
    router.route("/").method(HttpMethod.GET).handler(this::handleEmptyRequest);
    router
        .route("/graphql")
        .method(HttpMethod.POST)
        .produces(APPLICATION_JSON)
        .handler(this::handleGraphQLRPCRequest);

    final CompletableFuture<?> resultFuture = new CompletableFuture<>();
    httpServer
        .requestHandler(router)
        .listen(
            res -> {
              if (!res.failed()) {
                resultFuture.complete(null);
                LOG.info(
                    "GraphQLRPC service started and listening on {}:{}",
                    config.getHost(),
                    httpServer.actualPort());
                return;
              }
              httpServer = null;
              final Throwable cause = res.cause();
              if (cause instanceof SocketException) {
                resultFuture.completeExceptionally(
                    new GraphQLRpcServiceException(
                        String.format(
                            "Failed to bind Ethereum GraphQL RPC listener to %s:%s: %s",
                            config.getHost(), config.getPort(), cause.getMessage())));
                return;
              }
              resultFuture.completeExceptionally(cause);
            });

    return resultFuture;
  }

  public CompletableFuture<?> stop() {
    if (httpServer == null) {
      return CompletableFuture.completedFuture(null);
    }

    final CompletableFuture<?> resultFuture = new CompletableFuture<>();
    httpServer.close(
        res -> {
          if (res.failed()) {
            resultFuture.completeExceptionally(res.cause());
          } else {
            httpServer = null;
            resultFuture.complete(null);
          }
        });
    return resultFuture;
  }

  public InetSocketAddress socketAddress() {
    if (httpServer == null) {
      return EMPTY_SOCKET_ADDRESS;
    }
    return new InetSocketAddress(config.getHost(), httpServer.actualPort());
  }

  @VisibleForTesting
  public String url() {
    if (httpServer == null) {
      return "";
    }
    return urlForSocketAddress("http", socketAddress());
  }

  private void handleGraphQLRPCRequest(final RoutingContext routingContext) {
    try {
      final String graphQuery = routingContext.getBodyAsString().trim();
      handleGraphQLSingleRequest(routingContext, graphQuery);

    } catch (final DecodeException ex) {
      handleGraphQLRpcError(routingContext, ex);
    }
  }

  // Facilitate remote health-checks in AWS, inter alia.
  private void handleEmptyRequest(final RoutingContext routingContext) {
    routingContext.response().setStatusCode(201).end();
  }

  private void handleGraphQLSingleRequest(
      final RoutingContext routingContext, final String request) {
    final HttpServerResponse response = routingContext.response();
    vertx.executeBlocking(
        future -> {
          final GraphQLRpcResponse graphQLRpcResponse = process(request);
          future.complete(graphQLRpcResponse);
        },
        false,
        (res) -> {
          if (res.failed()) {
            response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            return;
          }

          final GraphQLRpcResponse graphQLRpcResponse = (GraphQLRpcResponse) res.result();
          response.setStatusCode(status(graphQLRpcResponse).code());
          response.putHeader("Content-Type", APPLICATION_JSON);
          response.end(serialise(graphQLRpcResponse));
        });
  }

  private HttpResponseStatus status(final GraphQLRpcResponse response) {

    switch (response.getType()) {
      case UNAUTHORIZED:
        return HttpResponseStatus.UNAUTHORIZED;
      case ERROR:
        return HttpResponseStatus.BAD_REQUEST;
      case SUCCESS:
      case NONE:
      default:
        return HttpResponseStatus.OK;
    }
  }

  private String serialise(final GraphQLRpcResponse response) {

    if (response.getType() == GraphQLRpcResponseType.NONE) {
      return EMPTY_RESPONSE;
    }

    return Json.encodePrettily(response.getResult());
  }

  private GraphQLRpcResponse process(final String requestJson) {
    final ExecutionInput executionInput =
        ExecutionInput.newExecutionInput().query(requestJson).context(dataFetcherContext).build();
    final ExecutionResult result = graphQL.execute(executionInput);
    final Map<String, Object> toSpecificationResult = result.toSpecification();
    final List<GraphQLError> errors = result.getErrors();
    if (errors.size() == 0) {
      return new GraphQLRpcSuccessResponse(toSpecificationResult);
    } else {
      return new GraphQLRpcErrorResponse(toSpecificationResult);
    }
  }

  private void handleGraphQLRpcError(final RoutingContext routingContext, final Object errors) {
    routingContext
        .response()
        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
        .end(Json.encode(new GraphQLRpcErrorResponse(errors)));
  }
}
