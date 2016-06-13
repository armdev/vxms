package org.jacpfx.vertx.registry;

import java.util.function.Consumer;

/**
 * Created by Andy Moncsek on 30.05.16.
 */
public class DCRetry extends DCExecute{



    public DCRetry(DiscoveryClient client, String serviceName, Consumer<NodeResponse> consumer, Consumer<NodeResponse> onFailure,Consumer<NodeResponse> onError, int amount) {
        super(client,serviceName,consumer,onFailure,onError,amount,0);
    }


    public DCExecute delay(long ms){
        return new DCExecute(client,serviceName,consumer,onFailure,onError,amount,ms);
    }


}
