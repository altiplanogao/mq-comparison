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
public class RoutingTest extends BaseRabbitTest {

    private static final String EXCHANGE_NAME = "direct_logs";

    private static final String SEVERITY_ERROR = "error";
    private static final String SEVERITY_INFO = "info";
    private static final String SEVERITY_WARNING = "warning";

    public void send(String severity, String message) throws Exception {
        try(Connection connection = newConnection()) {
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);

            channel.basicPublish(EXCHANGE_NAME, severity, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + severity + "':'" + message + "'");

            channel.close();
        }
    }

    private class Subscriber {
        private final CountDownLatch latch;
        private final AtomicInteger counter;
        private final List<String> severities = new ArrayList<>();

        public Subscriber(CountDownLatch latch,AtomicInteger counter, String... severities) {
            this.counter = counter;
            this.latch = latch;
            for(String s : severities){
                this.severities.add(s);
            }
        }

        public void run(String name) throws IOException, TimeoutException {
            Connection connection = newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
            String queueName = channel.queueDeclare().getQueue();

            for(String severity : severities){
                channel.queueBind(queueName, EXCHANGE_NAME, severity);
            }

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    log(" [x] Received '" + envelope.getRoutingKey() + "':'" + message + "'");
                    try{

                    }finally {
                        counter.incrementAndGet();
                        latch.countDown();
                    }
                }
            };
            channel.basicConsume(queueName, true, consumer);
        }
    }

    public void recv(String receiver, final CountDownLatch latch, AtomicInteger counter, String... severities) throws Exception {
        Subscriber subscriber = new Subscriber(latch, counter, severities);
        subscriber.run(receiver);
    }

    @Test
    public void test() throws Exception{
        final int errors = 10;
        final int infos = 10;
        final int warnings = 10;

        AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(errors * 2 + infos +warnings);

        recv("C1", latch, counter, SEVERITY_ERROR);
        recv("C2", latch, counter, SEVERITY_ERROR, SEVERITY_INFO, SEVERITY_WARNING);

        ExecutorService executorService = Executors.newCachedThreadPool();
        for(int i = 0; i < errors + infos + warnings; ++i) {
            int finalI = i;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    String message = "Hello" + finalI;
                    try {
                        final String sev;
                        switch (finalI % 3){
                            case 2: sev = SEVERITY_ERROR; break;
                            case 1: sev = SEVERITY_INFO; break;
                            case 0: sev = SEVERITY_WARNING; break;
                            default: sev = "";
                        }
                        send(sev, message);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
        Assert.assertEquals(errors*2 + infos + warnings, counter.get());

        try(Connection connection = newConnection()){
            Channel channel = connection.createChannel();
            channel.exchangeDelete(EXCHANGE_NAME);
        }
    }
}
