package being.altiplano.mqdemo.rabbitmq;

import com.rabbitmq.client.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gaoyuan on 06/04/2017.
 */
public class TopicTest extends BaseRabbitTest {
    private static final String EXCHANGE_NAME = "topic_logs";

    public void send(String routingKey, String message) throws Exception {
        try (Connection connection = newConnection()) {
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");

            channel.close();
        }
    }

    private class Subscriber {
        private final CountDownLatch latch;
        private final AtomicInteger counter;
        private final List<String> bindingKys = new ArrayList<>();

        public Subscriber(CountDownLatch latch, AtomicInteger counter, String... bindingKys) {
            this.counter = counter;
            this.latch = latch;
            for (String s : bindingKys) {
                this.bindingKys.add(s);
            }
        }

        public void run(String name) throws IOException, TimeoutException {
            Connection connection = newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
            String queueName = channel.queueDeclare().getQueue();

            for (String bk : bindingKys) {
                channel.queueBind(queueName, EXCHANGE_NAME, bk);
            }

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    log(" [x] Received '" + envelope.getRoutingKey() + "':'" + message + "'");
                    try {

                    } finally {
                        counter.incrementAndGet();
                        latch.countDown();
                    }
                }
            };
            channel.basicConsume(queueName, true, consumer);
        }
    }

    public void recv(String receiver, final CountDownLatch latch, final AtomicInteger counter, String... severities) throws Exception {
        Subscriber subscriber = new Subscriber(latch, counter, severities);
        subscriber.run(receiver);
    }

    public static final String[] SPEEDS = {"lazy", "quick"};
    public static final String[] COLOURS = {"orange", "pink", "brown"};
    public static final String[] SPECIES = {"rabbit", "fox", "elephant", "duck"};

    @Test
    public void test() throws Exception {
        final int bySpeed = COLOURS.length * SPECIES.length;
        final int byColor = SPEEDS.length * SPECIES.length;
        final int bySpecies = COLOURS.length * SPEEDS.length;

        final AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch c1Latch = new CountDownLatch(byColor);
        final CountDownLatch c2Latch = new CountDownLatch(bySpeed + bySpecies - COLOURS.length + 1);

        recv("C1", c1Latch, counter, "*.orange.*");
        recv("C2", c2Latch, counter, "*.*.rabbit", "lazy.#");

        AtomicInteger id = new AtomicInteger(0);
        ExecutorService executorService = Executors.newCachedThreadPool();

        for (String speed : SPEEDS) {
            for (String color : COLOURS) {
                for (String spec : SPECIES) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            final String xx = speed + "." + color + "." + spec;
                            try {
                                send(xx, "#" + id.incrementAndGet() + "." + xx);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }

        String[] specials = {"orange", "quick.orange.male.rabbit", "lazy.orange.male.rabbit"};
        for (String spec : specials) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    final String xx = spec;
                    try {
                        send(xx, "Hello#" + id.incrementAndGet() + "." + xx);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        c1Latch.await(5, TimeUnit.SECONDS);
        c2Latch.await(5, TimeUnit.SECONDS);

        Assert.assertEquals(0, c1Latch.getCount());
        Assert.assertEquals(0, c2Latch.getCount());
        Assert.assertEquals(bySpeed + byColor + bySpecies - COLOURS.length + 1, counter.get());

        try (Connection connection = newConnection()) {
            Channel channel = connection.createChannel();
            channel.exchangeDelete(EXCHANGE_NAME);
        }
    }
}