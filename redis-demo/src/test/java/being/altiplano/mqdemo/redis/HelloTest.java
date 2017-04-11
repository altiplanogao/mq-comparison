package being.altiplano.mqdemo.redis;

import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by gaoyuan on 06/04/2017.
 */
public class HelloTest {

    @Test
    public void doTest() throws Exception {
//        final JedisPoolConfig POOL_CONFIG = new JedisPoolConfig();
//        final JedisPool JEDIS_POOL =
//                new JedisPool(POOL_CONFIG, "localhost", 6379, 0);

        final String channelName = "interest";
        final int msgCount = 10;
        final List<String> messagesReceived = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(msgCount);

//        final Jedis subscriberJedis = JEDIS_POOL.getResource();
//        final Jedis publisherJedis = JEDIS_POOL.getResource();
        final Jedis subscriberJedis = new Jedis("localhost", 6379);
        final Jedis publisherJedis = new Jedis("localhost", 6379);

        final JedisPubSub subscriber = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                super.onMessage(channel, message);
                messagesReceived.add(message);
                latch.countDown();
            }

            @Override
            public void onPMessage(String pattern, String channel, String message) {
                super.onPMessage(pattern, channel, message);
                messagesReceived.add(message);
                latch.countDown();
            }
        };
        new Thread(){
            @Override
            public void run() {
                super.run();
                subscriberJedis.subscribe(subscriber, channelName);
            }
        }.start();

        for (int i =0; i < msgCount ; ++i) {
            publisherJedis.publish(channelName, "" + i);
        }

        latch.await();
        for (int i = 0;i<msgCount;++i){
            Assert.assertEquals(""+i, messagesReceived.get(i));
        }

        subscriber.unsubscribe();
        subscriberJedis.close();
    }
}
