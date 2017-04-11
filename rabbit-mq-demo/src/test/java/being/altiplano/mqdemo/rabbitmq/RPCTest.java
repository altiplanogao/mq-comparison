package being.altiplano.mqdemo.rabbitmq;

import com.rabbitmq.client.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

/**
 * Created by gaoyuan on 06/04/2017.
 */
public class RPCTest extends BaseRabbitTest {
    private static final String RPC_QUEUE_NAME = "rpc_queue";

    private String call(String message) throws Exception {
        try (Connection connection = newConnection()) {
            Channel channel = connection.createChannel();

            String replyQueueName = channel.queueDeclare().getQueue();
            String corrId = UUID.randomUUID().toString();

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .correlationId(corrId)
                    .replyTo(replyQueueName)
                    .build();

            channel.basicPublish("", RPC_QUEUE_NAME, props, message.getBytes("UTF-8"));
            final BlockingQueue<String> response = new ArrayBlockingQueue<String>(1);

            channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    if (properties.getCorrelationId().equals(corrId)) {
                        response.offer(new String(body, "UTF-8"));
                    }
                }
            });
            String result = response.take();
            channel.close();
            return result;
        }
    }

    private static long fib(long n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }

    public void service() throws Exception {

        Connection connection = null;

        connection = newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);

        channel.basicQos(1);

        System.out.println(" [x] Awaiting RPC requests");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(properties.getCorrelationId())
                        .build();

                String response = "";

                try {
                    String message = new String(body, "UTF-8");
                    long n = Long.parseLong(message);

                    System.out.println(" [.] fib(" + message + ")");
                    response += fib(n);
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e.toString());
                } finally {
                    channel.basicPublish("", properties.getReplyTo(), replyProps, response.getBytes("UTF-8"));

                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        };

        channel.basicConsume(RPC_QUEUE_NAME, false, consumer);

    }

    @Test
    public void test() throws Exception {
        int times = 1_00;
        service();

        int counts = 30;
        long[] fs = new long[counts];

        fs[0] = 0;
        fs[1] = 1;
        for (int i = 2; i < counts; ++i) {
            fs[i] = fs[i - 1] + fs[i - 2];
        }

        for (int i = 0; i < counts; ++i) {
            long a = Long.parseLong(call("" + i));
            Assert.assertEquals(fs[i], a);
        }

        try (Connection connection = newConnection()) {
            Channel channel = connection.createChannel();
            channel.queueDelete(RPC_QUEUE_NAME);
        }
    }
}