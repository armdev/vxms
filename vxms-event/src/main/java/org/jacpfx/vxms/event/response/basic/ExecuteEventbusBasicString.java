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

package org.jacpfx.vxms.event.response.basic;

import static java.util.Optional.ofNullable;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.jacpfx.vxms.common.ExecutionStep;
import org.jacpfx.vxms.common.VxmsShared;
import org.jacpfx.vxms.common.throwable.ThrowableErrorConsumer;
import org.jacpfx.vxms.common.throwable.ThrowableFutureBiConsumer;
import org.jacpfx.vxms.common.throwable.ThrowableFutureConsumer;
import org.jacpfx.vxms.event.interfaces.basic.ExecuteEventbusStringCall;

/**
 * Created by Andy Moncsek on 12.01.16. This class is the end of the non blocking fluent API, all
 * data collected to execute the chain.
 */
public class ExecuteEventbusBasicString {

  protected final String methodId;
  protected final VxmsShared vxmsShared;
  protected final Throwable failure;
  protected final Consumer<Throwable> errorMethodHandler;
  protected final Message<Object> message;
  protected final List<ExecutionStep> chain;
  protected final ThrowableFutureConsumer<String> stringConsumer;
  protected final Consumer<Throwable> errorHandler;
  protected final ThrowableErrorConsumer<Throwable, String> onFailureRespond;
  protected final ExecuteEventbusStringCall excecuteEventBusAndReply;
  protected final DeliveryOptions deliveryOptions;
  protected final int retryCount;
  protected final long timeout;
  protected final long circuitBreakerTimeout;

  /**
   * The constructor to pass all needed members
   *
   * @param methodId the method identifier
   * @param vxmsShared the vxmsShared instance, containing the Vertx instance and other shared
   *     objects per instance
   * @param failure the failure thrown while task execution
   * @param errorMethodHandler the error handler
   * @param message the message to respond to
   * @param chain
   * @param stringConsumer the consumer, producing the byte response
   * @param excecuteEventBusAndReply handles the response execution after event-bus bridge reply
   * @param errorHandler the error handler
   * @param onFailureRespond the consumer that takes a Future with the alternate response value in
   *     case of failure
   * @param deliveryOptions the response deliver serverOptions
   * @param retryCount the amount of retries before failure execution is triggered
   * @param timeout the amount of time before the execution will be aborted
   * @param circuitBreakerTimeout the amount of time before the circuit breaker closed again
   */
  public ExecuteEventbusBasicString(
      String methodId,
      VxmsShared vxmsShared,
      Throwable failure,
      Consumer<Throwable> errorMethodHandler,
      Message<Object> message,
      List<ExecutionStep> chain,
      ThrowableFutureConsumer<String> stringConsumer,
      ExecuteEventbusStringCall excecuteEventBusAndReply,
      Consumer<Throwable> errorHandler,
      ThrowableErrorConsumer<Throwable, String> onFailureRespond,
      DeliveryOptions deliveryOptions,
      int retryCount,
      long timeout,
      long circuitBreakerTimeout) {
    this.methodId = methodId;
    this.vxmsShared = vxmsShared;
    this.failure = failure;
    this.errorMethodHandler = errorMethodHandler;
    this.message = message;
    this.chain = chain;
    this.stringConsumer = stringConsumer;
    this.excecuteEventBusAndReply = excecuteEventBusAndReply;
    this.errorHandler = errorHandler;
    this.onFailureRespond = onFailureRespond;
    this.deliveryOptions = deliveryOptions;
    this.retryCount = retryCount;
    this.timeout = timeout;
    this.circuitBreakerTimeout = circuitBreakerTimeout;
  }

  /**
   * Execute the reply chain with given deliveryOptions
   *
   * @param deliveryOptions, the event bus Delivery Options
   */
  public void execute(DeliveryOptions deliveryOptions) {
    Objects.requireNonNull(deliveryOptions);
    new ExecuteEventbusBasicString(
            methodId,
            vxmsShared,
            failure,
            errorMethodHandler,
            message,
            chain,
            stringConsumer,
            excecuteEventBusAndReply,
            errorHandler,
            onFailureRespond,
            deliveryOptions,
            retryCount,
            timeout,
            circuitBreakerTimeout)
        .execute();
  }

  /** Execute the reply chain */
  public void execute() {
    final Vertx vertx = vxmsShared.getVertx();
    vertx.runOnContext(
        action -> {
          ofNullable(excecuteEventBusAndReply)
              .ifPresent(
                  evFunction -> {
                    try {
                      evFunction.execute(
                          methodId,
                          vxmsShared,
                          errorMethodHandler,
                          message,
                          errorHandler,
                          onFailureRespond,
                          deliveryOptions,
                          retryCount,
                          timeout,
                          circuitBreakerTimeout);
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  });

          ofNullable(stringConsumer)
              .ifPresent(
                  userOperation -> {
                    int retry = retryCount;
                    ResponseExecution.createResponse(
                        methodId,
                        retry,
                        timeout,
                        circuitBreakerTimeout,
                        userOperation,
                        errorHandler,
                        onFailureRespond,
                        errorMethodHandler,
                        vxmsShared,
                        failure,
                        value -> {
                          if (value.succeeded()) {
                            respond(value.getResult());
                          } else {
                            fail(
                                value.getCause().getMessage(),
                                HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                          }
                        });
                  });
        });

    ofNullable(chain)
        .ifPresent(
            chainList -> {
              if (!chainList.isEmpty()) {
                final ExecutionStep executionStep = chainList.get(0);
                ofNullable(executionStep.getChainconsumer())
                    .ifPresent(
                        initialConsumer -> {
                          int retry = retryCount;
                          ResponseExecution.createResponse(
                              methodId,
                              retry,
                              timeout,
                              circuitBreakerTimeout,
                              initialConsumer,
                              errorHandler,
                              onFailureRespond,
                              errorMethodHandler,
                              vxmsShared,
                              failure,
                              value -> {
                                if (value.succeeded()) {
                                  final Object result = value.getResult();
                                  if (chainList.size() > 1 && !value.handledError()) {
                                    final ExecutionStep executionStepAndThan = chainList.get(1);
                                    ofNullable(executionStepAndThan.getStep())
                                        .ifPresent(
                                            step ->
                                                executeStep(
                                                    chainList,
                                                    retry,
                                                    result,
                                                    executionStepAndThan,
                                                    step));
                                  } else {
                                    respond(value.getResult());
                                  }
                                } else {
                                  fail(
                                      value.getCause().getMessage(),
                                      HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                                }
                              });
                        });
              }
            });
  }

  private void executeStep(
      List<ExecutionStep> chainList,
      int retry,
      Object result,
      ExecutionStep element,
      ThrowableFutureBiConsumer step) {
    StepExecution.createResponse(
        methodId,
        retry,
        timeout,
        circuitBreakerTimeout,
        step,
        result,
        errorHandler,
        onFailureRespond,
        errorMethodHandler,
        vxmsShared,
        failure,
        value -> {
          final int index = chainList.indexOf(element);
          final int size = chainList.size();
          if (value.succeeded()) {
            if (index == size - 1 ||  value.handledError()) {
              // handle last element
              respond(value.getResult());
            } else {
              // call recursive
              final ExecutionStep executionStepAndThan = chainList.get(index + 1);
              ofNullable(executionStepAndThan.getStep())
                  .ifPresent(
                      nextStep ->
                          executeStep(
                              chainList, retry, value.getResult(), executionStepAndThan, nextStep));
            }

          } else {
            fail(value.getCause().getMessage(), HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
          }
        });
  }

  protected void respond(String result) {
    if (result != null) {
      if (deliveryOptions != null) {
        message.reply(result, deliveryOptions);
      } else {
        message.reply(result);
      }
    }
  }

  protected void fail(String result, int statuscode) {
    if (result != null) {
      message.fail(statuscode, result);
    }
  }
}
