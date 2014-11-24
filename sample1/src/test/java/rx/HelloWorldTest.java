package rx;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HelloWorldTest {

    @Test
    public void testSayHello() {
        assertEquals("hello world", HelloWorld.msg);
    }
}
