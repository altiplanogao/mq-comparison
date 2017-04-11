package being.altiplano.mqdemo.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Rule;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by gaoyuan on 05/04/2017.
 */
public class BaseRabbitTest {
    @Rule
    public Timeout timeout = new Timeout(1_000);

    protected Connection newConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);

        Connection connection = factory.newConnection();
        return connection;
    }

    protected void log(String msg){
        System.out.println(this.getClass().getSimpleName() + " " + msg);
    }
}
