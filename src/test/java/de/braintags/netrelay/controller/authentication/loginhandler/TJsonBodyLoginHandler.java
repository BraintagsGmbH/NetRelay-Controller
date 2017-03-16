package de.braintags.netrelay.controller.authentication.loginhandler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;

import de.braintags.netrelay.unit.NetRelayBaseConnectorTest;
import de.braintags.netrelay.util.MockHttpServerRequest;
import de.braintags.netrelay.util.MockHttpServerResponse;
import de.braintags.netrelay.util.MockRoutingContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.unit.TestContext;

/**
 * Unit test for {@link JsonBodyLoginHandler}
 * 
 * @author sschmitt
 * 
 */
public class TJsonBodyLoginHandler extends NetRelayBaseConnectorTest {

  @Test(expected = IllegalStateException.class)
  public void testJsonBodyHandler_noInit(TestContext context) throws URISyntaxException {
    MockRoutingContext routingContext = new MockRoutingContext(vertx, new URI("http://localhost:8080/"), false);
    JsonBodyLoginHandler handler = new JsonBodyLoginHandler();
    handler.handle(routingContext);
  }

  @Test
  public void testJsonBodyHandler_noPost(TestContext context) throws URISyntaxException {
    MockRoutingContext routingContext = new MockRoutingContext(vertx, new URI("http://localhost:8080/"), false);
    JsonBodyLoginHandler handler = new JsonBodyLoginHandler();
    handler.init(new MockAuthProvider(null), new Properties());
    handler.handle(routingContext);
    assertThat(routingContext.isFailed(), is(true));
    assertThat(routingContext.statusCode(), is(405));
  }

  @Test
  public void testJsonBodyHandler_validBody(TestContext context) throws URISyntaxException {
    MockHttpServerRequest request = new MockHttpServerRequest(new URI("http://localhost:8080/"), HttpMethod.POST,
        new MockHttpServerResponse());
    MockRoutingContext routingContext = new MockRoutingContext(vertx, request, true);

    JsonObject requestBody = new JsonObject().put("testString", "value").put("testInt", 1).put("testList",
        Arrays.asList("v1", "v2"));
    Buffer buffer = Buffer.buffer(requestBody.toString());
    routingContext.setBody(buffer);

    JsonBodyLoginHandler handler = new JsonBodyLoginHandler();
    handler.init(new MockAuthProvider(requestBody), new Properties());
    handler.handle(routingContext);
    assertThat(routingContext.isFailed(), is(false));
  }

  private final class MockAuthProvider implements AuthProvider {

    private JsonObject expected;

    public MockAuthProvider(JsonObject expected) {
      this.expected = expected;
    }

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
      if (expected.equals(authInfo))
        resultHandler.handle(Future.succeededFuture());
      else
        resultHandler.handle(Future.failedFuture("JSON objects not equal"));
    }
  }
}
