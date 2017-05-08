/*
 *  Copyright (c) 2015-2016 Apcera Inc. All rights reserved. This program and the accompanying
 *  materials are made available under the terms of the MIT License (MIT) which accompanies this
 *  distribution, and is available at http://opensource.org/licenses/MIT
 */

package io.nats.streaming;

import static io.nats.streaming.NatsStreaming.ERR_CONNECTION_REQ_TIMEOUT;
import static io.nats.streaming.UnitTestUtilities.runServer;
import static io.nats.streaming.UnitTestUtilities.testClusterName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Logger;
import io.nats.client.Nats;
import io.nats.streaming.examples.Publisher;
import io.nats.streaming.examples.Subscriber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;

@Category(IntegrationTest.class)
public class SubscriberTest {
    static final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    static final Logger logger = (Logger) LoggerFactory.getLogger(SubscriberTest.class);

    static final LogVerifier verifier = new LogVerifier();

    private static final String clusterId = UnitTestUtilities.testClusterName;

    ExecutorService service = Executors.newCachedThreadPool();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TestCasePrinterRule pr = new TestCasePrinterRule(System.out);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testSubscriberStringArray() throws Exception {
        List<String> argList = new ArrayList<String>();
        argList.addAll(Arrays.asList("-c", UnitTestUtilities.testClusterName));
        argList.add("foo");

        String[] args = new String[argList.size()];
        args = argList.toArray(args);

        new Subscriber(args);
    }

    @Test
    public void testParseArgsBadFlags() {
        List<String> argList = new ArrayList<String>();
        String[] flags = new String[] { "-s", "-c", "-id", "-q", "--seq", "--since", "--durable" };
        boolean exThrown = false;

        for (String flag : flags) {
            try {
                exThrown = false;
                argList.clear();
                argList.addAll(Arrays.asList(flag, "foo"));
                String[] args = new String[argList.size()];
                args = argList.toArray(args);
                new Subscriber(args);
            } catch (IllegalArgumentException e) {
                assertEquals(String.format("%s requires an argument", flag), e.getMessage());
                exThrown = true;
            } finally {
                assertTrue("Should have thrown exception", exThrown);
            }
        }
    }

    @Test
    public void testParseArgsNotEnoughArgs() {
        List<String> argList = new ArrayList<String>();
        boolean exThrown = false;

        try {
            exThrown = false;
            argList.clear();
            String[] args = new String[argList.size()];
            args = argList.toArray(args);
            new Subscriber(args);
        } catch (IllegalArgumentException e) {
            assertEquals("must supply at least a subject name", e.getMessage());
            exThrown = true;
        } finally {
            assertTrue("Should have thrown exception", exThrown);
        }
    }

    @Test(timeout = 5000)
    public void testMainSuccess() throws Exception {
        try (NatsStreamingServer srv = runServer(testClusterName)) {
            Publisher.main(new String[] { "-c", clusterId, "foo", "bar" });
            Subscriber.main(new String[] { "-c", clusterId, "--all", "--count", "1", "foo" });
        }
    }

    @Test
    public void testMainFailsNoServers() throws Exception {
        thrown.expect(IOException.class);
        thrown.expectMessage(Nats.ERR_NO_SERVERS);
        Subscriber.main(new String[] { "-s", "nats://enterprise:4242", "foobar" });
    }

    @Test
    public void testMainFailsTimeout() throws Exception {
        thrown.expect(IOException.class);
        thrown.expectMessage(ERR_CONNECTION_REQ_TIMEOUT);
        try (NatsStreamingServer srv = runServer(testClusterName)) {
            Subscriber.main(new String[] {"-c", "nonexistent-cluster", "foobar"});
        }
    }
}
