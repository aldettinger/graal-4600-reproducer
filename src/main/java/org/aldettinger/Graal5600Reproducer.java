package org.aldettinger;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultExchangeHolder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Graal5600Reproducer implements QuarkusApplication {

    @ConfigProperty(name = "camel.redis.test.server.authority")
    String authority;

    @Override
    public int run(String... args) throws Exception {
        Config config = new Config();
        config.useSingleServer().setAddress(String.format("redis://%s", authority));
        RedissonClient redisson = Redisson.create(config);

        RMap<String, DefaultExchangeHolder> cache = redisson.getMap("aggregation");

        //LOG.trace("Adding an Exchange with ID {} for key {} in a thread-safe manner.", exchange.getExchangeId(), key);
        RLock lock = redisson.getLock("aggregationLock");
        try {
            lock.lock();
            CamelContext context = new DefaultCamelContext();
            DefaultExchange exchange = new DefaultExchange(context);
            DefaultExchangeHolder newHolder = DefaultExchangeHolder.marshal(exchange, true, false);
            DefaultExchangeHolder oldHolder = cache.put("1", newHolder);
            if(oldHolder != null) {
                oldHolder.toString();
            }
        } finally {
            //LOG.trace("Added an Exchange with ID {} for key {} in a thread-safe manner.", exchange.getExchangeId(), key);
            lock.unlock();
        }

        cache.toString();
        redisson.toString();
        return 0;
    }

}
