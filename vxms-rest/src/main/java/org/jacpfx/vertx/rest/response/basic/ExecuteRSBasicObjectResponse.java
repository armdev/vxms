package org.jacpfx.vertx.rest.response.basic;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.jacpfx.common.ThrowableErrorConsumer;
import org.jacpfx.common.ThrowableFutureConsumer;
import org.jacpfx.common.ThrowableSupplier;
import org.jacpfx.common.encoder.Encoder;
import org.jacpfx.vertx.rest.interfaces.ExecuteEventBusObjectCall;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Andy Moncsek on 12.01.16.
 */
public class ExecuteRSBasicObjectResponse extends ExecuteRSBasicObject {


    public ExecuteRSBasicObjectResponse(Vertx vertx, Throwable t, Consumer<Throwable> errorMethodHandler, RoutingContext context, Map<String, String> headers, ThrowableFutureConsumer<Serializable> objectConsumer, ExecuteEventBusObjectCall excecuteEventBusAndReply, Encoder encoder,
                                        Consumer<Throwable> errorHandler, ThrowableErrorConsumer<Throwable, Serializable> onFailureRespond, int httpStatusCode, int retryCount, long timeout) {
        super(vertx, t, errorMethodHandler, context, headers, objectConsumer, excecuteEventBusAndReply, encoder, errorHandler, onFailureRespond, httpStatusCode, retryCount, timeout);
    }


    /**
     * defines an action for errors in byte responses, you can handle the error and return an alternate createResponse value
     *
     * @param onFailureRespond the handler (function) to execute on error
     * @return the createResponse chain
     */
    public ExecuteRSBasicObject onFailureRespond(ThrowableErrorConsumer<Throwable, Serializable> onFailureRespond, Encoder encoder) {
        return new ExecuteRSBasicObject(vertx, t, errorMethodHandler, context, headers, objectConsumer, excecuteEventBusAndReply, encoder, errorHandler, onFailureRespond, httpStatusCode, retryCount, timeout);
    }
    /**
     * intermediate error handler which will be called on each error (at least 1 time, in case on N retries... up to N times)
     * @param errorHandler the handler to be executed on each error
     * @return the response chain
     */
    public ExecuteRSBasicObjectResponse onError(Consumer<Throwable> errorHandler) {
        return new ExecuteRSBasicObjectResponse(vertx, t, errorMethodHandler, context, headers, objectConsumer, excecuteEventBusAndReply, encoder, errorHandler, onFailureRespond, httpStatusCode, retryCount, timeout);
    }

    /**
     * Defines how long a method can be executed before aborted.
     * @param timeout the amount of timeout in ms
     * @return the response chain
     */
    public ExecuteRSBasicObjectResponse timeout(long timeout) {
        return new ExecuteRSBasicObjectResponse(vertx, t, errorMethodHandler, context, headers, objectConsumer, excecuteEventBusAndReply, encoder, errorHandler, onFailureRespond, httpStatusCode, retryCount, timeout);
    }

    /**
     * retry execution N times before
     * @param retryCount the amount of retries
     * @return the response chain
     */
    public ExecuteRSBasicObjectResponse retry(int retryCount) {
        return new ExecuteRSBasicObjectResponse(vertx, t, errorMethodHandler, context, headers, objectConsumer, excecuteEventBusAndReply, encoder, errorHandler, onFailureRespond, httpStatusCode, retryCount, timeout);
    }
    /**
     * put HTTP header to response
     * @param key the header name
     * @param value the header value
     * @return the response chain
     */
    public ExecuteRSBasicObjectResponse putHeader(String key, String value) {
        Map<String, String> headerMap = new HashMap<>(headers);
        headerMap.put(key, value);
        return new ExecuteRSBasicObjectResponse(vertx, t, errorMethodHandler, context, headerMap, objectConsumer, excecuteEventBusAndReply, encoder, errorHandler, onFailureRespond, httpStatusCode, retryCount, timeout);
    }
}
