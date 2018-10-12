package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class LogstashJsonLayoutTest {
    public static final String LOCATION_INFO = "LocationInfo";
    private static Logger logger = LogManager.getLogger(LogstashJsonLayoutTest.class);

    @Test(enabled = false, dataProvider = "dp")
    public void f(Integer n, String s) {
    }

    @DataProvider
    public Object[][] dp() {
        return new Object[][]{
                new Object[]{1, "a"},
                new Object[]{2, "b"},
        };
    }

    @BeforeTest
    public void beforeTest() {
    }

    @AfterTest
    public void afterTest() {
    }

    String expectedBasicSimpleTestJSON = "{\"@version\":\"1\"," +
                   // "\"@timestamp\":\"2015-07-28T11:31:18.492-07:00\",\"timeMillis\":1438108278492," +
                    "\"thread\":\""+ Thread.currentThread().getName() +"\"," +
                    "\"level\":\"DEBUG\"," +
                    "\"loggerName\":\"org.apache.logging.log4j.core.layout.LogstashJsonLayoutTest\"," +
                    "\"message\":\"Test Message\"," +
                    "\"endOfBatch\":false," +
                    "\"loggerFqcn\":\"org.apache.logging.log4j.core.layout.LogstashJsonLayoutTest\","+
                    "\"contextMap\":{\"A\":\"B\"}}";

    @Test
    public void BasicSimpleTest() throws Exception {
        Message simpleMessage = new SimpleMessage("Test Message");
        Map<String,String> mdc = new HashMap<>();
        mdc.put("A","B");
        LogEvent event = new Log4jLogEvent.Builder()
                .setLoggerName(logger.getName())
                .setMarker(null)
                .setLoggerFqcn(this.getClass().getCanonicalName())
                .setLevel(Level.DEBUG)
                .setMessage(simpleMessage)
                .setThrown(null)
                .setContextMap(mdc)
                .setContextStack(null)
                .setThreadName(Thread.currentThread().getName())
                .setTimeMillis(System.currentTimeMillis())
                .build();

        LogstashJsonLayout layout = LogstashJsonLayout.newBuilder()
                .setLocationInfo(true)
                .setProperties(true)
                .setComplete(true)
                .setCompact(true)
                .setEventEol(true)
                .setCharset(Charset.defaultCharset())
                .setAdditionalFields(new KeyValuePair[]{new KeyValuePair("Foo", "Bar")})
                .setConfiguration(new XmlConfiguration(new LoggerContext("test"), new ConfigurationSource(new FileInputStream("log4j2.xml"))))
                .build();

        String actualJSON = layout.toSerializable(event);
        assertThat(actualJSON, sameJSONAs(expectedBasicSimpleTestJSON)
                .allowingExtraUnexpectedFields()
                .allowingAnyArrayOrdering());

    }

}
