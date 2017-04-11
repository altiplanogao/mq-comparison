package being.altiplano.mqdemo.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * Created by gaoyuan on 05/04/2017.
 */
//http://activemq.apache.org/cross-language-clients.html
public class HelloJms {
    public static void sendToQueue() throws Exception {
        final ConnectionFactory connFactory = new ActiveMQConnectionFactory();
        final Connection conn = connFactory.createConnection();
        final Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination destination = session.createQueue("SampleQueue");

        final MessageProducer producer = session.createProducer(destination);
        final Message msg = session.createTextMessage("Simple Message");
        producer.send(msg);
        conn.close();
    }

    public static void receiveFromQueue() throws Exception {
        final ConnectionFactory connFactory = new ActiveMQConnectionFactory();
        final Connection conn = connFactory.createConnection();
        final Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination destination = session.createQueue("SampleQueue");

        final MessageConsumer consumer = session.createConsumer(destination);
        conn.start();
        final Message msg = consumer.receive();
        System.out.println(msg);
        conn.close();
    }

    public static void listenFromQueue() throws Exception {
        class Listener implements MessageListener {
            public void onMessage(Message message) {
                System.out.println(message);
            }
        }

        final ConnectionFactory connFactory = new ActiveMQConnectionFactory();
        final Connection conn = connFactory.createConnection();
        final Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination destination = session.createQueue("SampleQueue");

        final MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(new Listener());
//        conn.start();
//        final Message msg = consumer.receive();
//        System.out.println(msg);
//        conn.close();
    }



    public static void sendViaTopic() throws Exception {
        final ConnectionFactory connFactory = new ActiveMQConnectionFactory();
        final Connection conn = connFactory.createConnection();
        final Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination destination = session.createTopic("SampleTopic");

        final MessageProducer producer = session.createProducer(destination);
        final Message msg = session.createTextMessage("Simple Message");
        producer.send(msg);
        conn.close();
    }

    public static void receiveViaTopic() throws Exception {
        final ConnectionFactory connFactory = new ActiveMQConnectionFactory();
        final Connection conn = connFactory.createConnection();
        final Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination destination = session.createTopic("SampleTopic");

        final MessageConsumer consumer = session.createConsumer(destination);
        conn.start();
        final Message msg = consumer.receive();
        System.out.println(msg);
        conn.close();
    }

    public static void receiveViaTopicW() throws Exception {
        final ConnectionFactory connFactory = new ActiveMQConnectionFactory();
        final Connection conn = connFactory.createConnection();
        conn.setClientID("SampleClient");
        final Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

        final Topic destination = session.createTopic("SampleTopic");
        final MessageConsumer consumer = session.createDurableSubscriber(destination, "SampleSubscription");
        conn.start();
        final Message msg = consumer.receive();
        System.out.println(msg);
        conn.close();
    }
}
