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
 * Created by gaoyuan on 06/04/2017.
 */
public class WorkQueuesTest extends BaseRabbitTest {
    private static final String TASK_QUEUE_NAME = "task_queue";

    public void send(String message) throws Exception {
        Connection connection = newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);

        channel.basicPublish("", TASK_QUEUE_NAME,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                message.getBytes("UTF-8"));
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();
    }

    public void recv(String receiver, final CountDownLatch latch) throws Exception {
        final Connection connection = newConnection();
        final Channel channel = connection.createChannel();

        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        channel.basicQos(1);

        final Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");

                log(receiver + " [x] Received '" + message + "'");
                try {
                    for (char ch : message.toCharArray()) {
                        if (ch == '.') {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException _ignored) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        };
        channel.basicConsume(TASK_QUEUE_NAME, false, consumer);
    }

    @Test
    public void test()throws Exception{
        int times = 1_00;
        final CountDownLatch latch = new CountDownLatch(times);

        recv("AAA", latch);
        recv("BBB", latch);

        ExecutorService executorService = Executors.newCachedThreadPool();
        for(int i = 0 ; i < times; ++i){
            int finalI = i;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    String message = "Hello#" + finalI;
                    try {
                        send(message);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());

        try (Connection connection = newConnection()){
            Channel channel = connection.createChannel();
            channel.queueDelete(TASK_QUEUE_NAME);
        }
    }
}
