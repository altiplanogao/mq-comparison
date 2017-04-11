package being.altiplano.mqdemo.rabbitmq;

import com.rabbitmq.client.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gaoyuan on 06/04/2017.
 */
public class PublistSubscribeTest extends BaseRabbitTest {
    private static final String EXCHANGE_NAME = "logs";

    public void send(String message) throws Exception {
        try(final Connection connection = newConnection()) {
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);

            channel.basicPublish(EXCHANGE_NAME, "", null,
                    message.getBytes("UTF-8"));
            log(" [x] Sent '" + message + "'");

            channel.close();
        }
    }

    public void recv(String receiver, final CountDownLatch latch, final AtomicInteger counter) throws Exception {
        final Connection connection = newConnection();
        final Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        final Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");

                log(" [x] Received '" + message + "'");
                try {
                } finally {
                    counter.incrementAndGet();
                    latch.countDown();
                }
            }
        };
            channel.basicConsume(queueName, true, consumer);
    }

    @Test
    public void test() throws Exception {
        int times = 1_00;
        AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(times * 2);
        recv("AAA", latch,counter);
        recv("BBB", latch,counter);

        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0 ; i < times ; ++i) {
            int finalI = i;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    String message = "Hello" + finalI;
                    try{
                        send(message);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
        Assert.assertEquals(times * 2, counter.get());

        try (Connection connection = newConnection()){
            Channel channel = connection.createChannel();
            channel.exchangeDelete(EXCHANGE_NAME);
        }
    }
}
