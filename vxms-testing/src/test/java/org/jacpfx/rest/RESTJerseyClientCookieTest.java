/*
 * Copyright [2017] [Andy Moncsek]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jacpfx.rest;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Cookie;
import io.vertx.test.core.VertxTestBase;
import io.vertx.test.fakecluster.FakeClusterManager;
import java.util.concurrent.CountDownLatch;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.jacpfx.vxms.common.ServiceEndpoint;
import org.jacpfx.vxms.rest.response.RestHandler;
import org.jacpfx.vxms.services.VxmsEndpoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

// import io.vertx.core.http.HttpClient;

/** Created by Andy Moncsek on 23.04.15. */
public class RESTJerseyClientCookieTest extends VertxTestBase {

  public static final String SERVICE_REST_GET = "/wsService";
  public static final int PORT2 = 8888;
  private static String HOST;

  static {
    // try {
    HOST = "127.0.0.1";
    // } catch (UnknownHostException e) {
    //     e.printStackTrace();
    // }
  }

  private io.vertx.core.http.HttpClient client;

  protected int getNumNodes() {
    return 1;
  }

  protected Vertx getVertx() {
    return vertices[0];
  }

  @Override
  protected ClusterManager getClusterManager() {
    return new FakeClusterManager();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    startNodes(getNumNodes());
  }

  @Before
  public void startVerticles() throws InterruptedException {

    CountDownLatch latch2 = new CountDownLatch(1);
    DeploymentOptions options = new DeploymentOptions().setInstances(1);
    options.setConfig(new JsonObject().put("clustered", false).put("host", HOST));
    // Deploy the module - the System property `vertx.modulename` will contain the name of the
    // module so you
    // don'failure have to hardecode it in your tests

    getVertx()
        .deployVerticle(
            new WsServiceTwo(),
            options,
            asyncResult -> {
              // Deployment is asynchronous and this this handler will be called when it's complete
              // (or failed)
              System.out.println("start service: " + asyncResult.succeeded());
              if (asyncResult.failed()) {
                asyncResult.cause().printStackTrace();
              }
              assertTrue(asyncResult.succeeded());
              assertNotNull("deploymentID should not be null", asyncResult.result());
              // If deployed correctly then start the tests!
              //   latch2.countDown();

              latch2.countDown();
            });

    client = getVertx().createHttpClient(new HttpClientOptions());
    awaitLatch(latch2);
  }

  @Test
  public void cookieTest() throws InterruptedException {
    System.out.println("start cookie test");
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    CountDownLatch latch = new CountDownLatch(1);
    HttpClientOptions options = new HttpClientOptions();
    options.setDefaultPort(PORT2);
    options.setDefaultHost(HOST);
    io.vertx.core.http.HttpClient client = vertx.createHttpClient(options);
    HttpClientRequest request =
        client.get(
            "/wsService/stringGETResponseSyncAsync",
            resp -> {
              System.out.println("response from vertx client");

              final HttpClientRequest httpClientRequest =
                  client.get(
                      "http://" + HOST + ":" + PORT2 + "/wsService/stringGETResponseSyncAsync",
                      myresp -> {
                        myresp.bodyHandler(
                            body -> {
                              String response = body.toString();
                              System.out.println("Response entity '" + response + "' received.");
                              vertx.runOnContext(
                                  context -> {
                                    Assert.assertEquals("xyz", response);
                                    latch.countDown();
                                  });
                            });
                      });
              httpClientRequest.headers().add("Cookie", "c1=xyz");
              httpClientRequest.end();
            });
    request.end();

    System.out.println("wait cookie test");
    latch.await();
    testComplete();
  }

  public io.vertx.core.http.HttpClient getClient() {
    return client;
  }

  @ServiceEndpoint(name = SERVICE_REST_GET, contextRoot = SERVICE_REST_GET, port = PORT2)
  public class WsServiceTwo extends VxmsEndpoint {

    ///// ------------- sync blocking ----------------

    @Path("/stringGETResponseSyncAsync")
    @GET
    public void rsstringGETResponseSyncAsync(RestHandler reply) {
      System.out.println("stringResponse: " + reply);
      Cookie someCookie = reply.request().cookie("c1");
      if (someCookie == null) {
        reply.response().end();
      } else {
        String cookieValue = someCookie.getValue();
        reply
            .response()
            .stringResponse(
                (future) -> {
                  future.complete(cookieValue);
                })
            .execute();
      }
    }
  }
}
