package being.altiplano.mqdemo.rabbitmq;

import com.rabbitmq.client.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by gaoyuan on 05/04/2017.
 */
public class HelloWorldTest extends BaseRabbitTest {
    private final static String QUEUE_NAME = "hello";

    public void send(String message) throws Exception {
        try(Connection connection = super.newConnection()){
            Channel channel = connection.createChannel();

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes("UTF-8"));
            log(" [x] Sent '" + message + "'");

            channel.close();
        }
    }

    public void recv(final CountDownLatch latch) throws Exception {
        Connection connection = super.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        log(" [*] Waiting for messages. To exit press CTRL+C");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                log(" [x] Received '" + message + "'");
                latch.countDown();
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }

    @Test
    public void test() throws Exception {
        int times = 1_00;
        final CountDownLatch latch = new CountDownLatch(times);
        recv(latch);

        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0 ; i < times ; ++i){
            int finalI = i;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    String message = "Hello" + finalI;
                    try {
                        send(message);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }

        latch.await(3, TimeUnit.SECONDS);
        Assert.assertEquals(0,latch.getCount());

        try(Connection connection = newConnection()){
            Channel channel = connection.createChannel();
            channel.queueDelete(QUEUE_NAME);
        }
    }
}
