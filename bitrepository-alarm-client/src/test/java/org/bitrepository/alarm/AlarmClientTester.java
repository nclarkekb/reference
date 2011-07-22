/*
 * #%L
 * bitrepository-access-client
 * *
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2010 - 2011 The State and University Library, The Royal Library and The State Archives, Denmark
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.bitrepository.alarm;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Date;

import org.apache.kahadb.util.ByteArrayOutputStream;
import org.bitrepository.alarm.handler.AlarmLoggingHandler;
import org.bitrepository.alarm.handler.MailingAlarmHandler;
import org.bitrepository.alarm_client.alarmclientconfiguration.AlarmConfiguration;
import org.bitrepository.alarm_client.alarmclientconfiguration.AlarmConfiguration.MailingConfiguration;
import org.bitrepository.bitrepositoryelements.AlarmDescription;
import org.bitrepository.bitrepositorymessages.Alarm;
import org.bitrepository.bitrepositorymessages.IdentifyContributorsForGetStatusRequest;
import org.bitrepository.protocol.ExampleMessageFactory;
import org.bitrepository.protocol.LocalActiveMQBroker;
import org.bitrepository.protocol.activemq.ActiveMQMessageBus;
import org.bitrepository.protocol.bus.MessageBusConfigurationFactory;
import org.bitrepository.protocol.configuration.MessageBusConfigurations;
import org.bitrepository.protocol.messagebus.MessageBus;
import org.jaccept.structure.ExtendedTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the AlarmClient and the AlarmHandlers.
 */
public class AlarmClientTester extends ExtendedTestCase {
    private static final Long DEFAULT_WAIT_TIME = 500L;

    @Test( groups={"regressiontest"})
    public void defaultTest() throws Exception {
        addDescription("Tests the instantiation of a AlarmClient by using a local broker and a TestAlarmHandler.");
        addStep("Defining constants for the test.", "Should not be able to fail here.");
        String QUEUE = "ALARM-" + new Date().getTime();
        String CORRELATION_ID = "Alarm_Correlation_Identifier_X";
        String BITREPOSITORY_COLLECTION_ID = "ALARM_COLLECTION";

        addStep("Make configuration for the messagebus.", "Both should be created.");
        MessageBusConfigurations confs = MessageBusConfigurationFactory.createEmbeddedMessageBusConfiguration();
        LocalActiveMQBroker broker = new LocalActiveMQBroker(confs.getPrimaryMessageBusConfiguration());

        try {
            addStep("Start the broker.", "Connections should be established.");
            broker.start();

            addStep("Initialise the variables for the AlarmClient, e.g. The Messagebus, the AlarmHandler and "
	     + "the AlarmConfiguration", "Should be allowed.");
            MessageBus bus = new ActiveMQMessageBus(confs);
            TestAlarmHandler handler = new TestAlarmHandler();
            AlarmConfiguration aconf = new AlarmConfiguration();
            aconf.setHandlerClass(handler.getClass().getName());
            aconf.setQueue(QUEUE);

            addStep("Instantiating the AlarmClient based on these variables.", "Should connect to the messagebus.");
            AlarmClient aclient = new AlarmClient(bus, handler, aconf);

            addStep("Wait for setup", "We wait!");
            synchronized (this) {
	 try {
	     wait(DEFAULT_WAIT_TIME);
	 } catch (InterruptedException e) {
	     e.printStackTrace();
	 }
            }
            
            addStep("Verify that nothing has been received by the handler.", "Latest alarm should be null.");
            Assert.assertNull(handler.getLatestAlarm(), "No alarms sent yet, thus latest alarm should be null.");
            Assert.assertNull(handler.getLatestAlarmMessage(), "No alarms sent yet, thus latest alarm should be null.");
            
            addStep("Create and send a message for the AlarmClient to handle", "");
            Alarm alarmMsg = ExampleMessageFactory.createMessage(Alarm.class);
            alarmMsg.setTo(QUEUE);
            bus.sendMessage(alarmMsg);

            addStep("Wait for mediator to handle message", "We wait!");
            synchronized (this) {
	 try {
	     wait(DEFAULT_WAIT_TIME);
	 } catch (InterruptedException e) {
	     e.printStackTrace();
	 }
            }

            addStep("Verify that the handler has received the sent alarm message.", 
	     "The Alarm message should be received and stored.");
            Assert.assertNotNull(handler.getLatestAlarm(), "The handler should have received a Alarm message");
            Assert.assertNotNull(handler.getLatestAlarmMessage(), "The handler should have received a Alarm message");
            Assert.assertTrue(handler.getLatestAlarmMessage().contains(alarmMsg.getBitRepositoryCollectionID()));
            Assert.assertTrue(handler.getLatestAlarmMessage().contains(alarmMsg.getCorrelationID()));
            Assert.assertTrue(handler.getLatestAlarmMessage().contains(QUEUE));
            Assert.assertEquals(alarmMsg, handler.getLatestAlarm());
        } finally {
            if(broker != null) {
	 broker.stop();
	 broker = null;
            }
        }
    }

    // TODO requires the logging properties to send the output to 'STDOUT' instead of an actual log file.
    @Test( groups={"other-test"})
    public void TestLoggingHandler() throws Exception {
        addDescription("Tests the AlarmLoggingHandler handling of alarms and other objects.");
        addStep("Initalise the output stream receiver.", "Should be OK.");
        PrintStream defaultOut = System.out;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));

            addStep("Initalise handler, AlarmMessage and constants", "Should not be problematic.");
            AlarmHandler handler = new AlarmLoggingHandler();
            Alarm alarmMsg = ExampleMessageFactory.createMessage(Alarm.class);

            String ALARM_MESSAGE = "REGRESSION-TEST";
            String ALARM_CODE = "123456";
            String EXCEPTION_MESSAGE = "Can you handle this??";
            String IDENTIFY_ID = "Identification_identifier";

            addStep("Insert description of ALARM_CODE and ALARM_MESSAGE in message send to handler", 
            "Should sent to log.");
            AlarmDescription desc = new AlarmDescription();
            desc.setAlarmCode(new BigInteger(ALARM_CODE));
            desc.setAlarmText(ALARM_MESSAGE);
            alarmMsg.setAlarmDescription(desc);
            handler.notify(alarmMsg);

            addStep("Tests whether it has been written to the LOG", "Should be found in the outputstream");
            String logwrittenOutput = new String(out.toByteArray());
            Assert.assertTrue(logwrittenOutput.contains(ALARM_MESSAGE), 
	     "The message should contain '" + ALARM_MESSAGE + "' but was: '" + logwrittenOutput);
            Assert.assertTrue(logwrittenOutput.contains(ALARM_CODE), 
	     "The message should contain '" + ALARM_CODE + "' but was: '" + logwrittenOutput);
            Assert.assertTrue(logwrittenOutput.contains(alarmMsg.getClass().getName()), 
	     "The message should contain '" + alarmMsg.getClass().getName() + "' but was: '" + logwrittenOutput);
            defaultOut.print(logwrittenOutput);
            out.flush();

            addStep("Tests the handling of other objects, in this case an NullPointerException", 
            "Should be written to the log.");
            handler.notify(new NullPointerException(EXCEPTION_MESSAGE));
            logwrittenOutput = new String(out.toByteArray());
            Assert.assertTrue(logwrittenOutput.contains(EXCEPTION_MESSAGE), 
	     "The message should contain '" + EXCEPTION_MESSAGE + "' but was: '" + logwrittenOutput);
            defaultOut.print(logwrittenOutput);
            out.flush();

            addStep("Tests the handling of other messages, in this case an IdentifyConstributorsForGetStatusRequest", 
            "Should be written to the log.");
            IdentifyContributorsForGetStatusRequest identifyMsg = ExampleMessageFactory.createMessage(
	     IdentifyContributorsForGetStatusRequest.class);
            identifyMsg.setBitRepositoryCollectionID(IDENTIFY_ID);
            handler.notify(identifyMsg);
            logwrittenOutput = new String(out.toByteArray());
            Assert.assertTrue(logwrittenOutput.contains(IDENTIFY_ID), 
	     "The message should contain '" + IDENTIFY_ID + "' but was: '" + logwrittenOutput);
            Assert.assertTrue(logwrittenOutput.contains(identifyMsg.getClass().getName()), 
	     "The message should contain '" + identifyMsg.getClass().getName() + "' but was: '" 
	     + logwrittenOutput);

            defaultOut.print(logwrittenOutput);
            out.flush();
        } finally {
            System.setOut(defaultOut);
        }
    }

    // TODO insert your own mail address to test the mailing handler.
    @Test( groups={"other-test"})
    public void mailingAlarmHandler() throws Exception {
        addDescription("Testing the MailingAlarmHandler");
        addStep("Initialising the variables for the test.", "Should be OK");
        AlarmConfiguration aconf = new AlarmConfiguration();
        MailingConfiguration conf = new MailingConfiguration();
        conf.setMailReceiver("error@sbforge.org");
        conf.setMailSender("error@sbforge.org");
        conf.setMailServer("sbforge.org");
        aconf.setMailingConfiguration(conf);
        AlarmHandler handler = new MailingAlarmHandler(aconf);
        Alarm msg = ExampleMessageFactory.createMessage(Alarm.class);

        String ALARM_MESSAGE = "REGRESSION-TEST";
        String ALARM_CODE = "123456";
        String EXCEPTION_MESSAGE = "Can you handle this??";

        addStep("Insert description of ALARM_CODE and ALARM_MESSAGE in message send to handler", 
        "Should sent to log.");
        AlarmDescription desc = new AlarmDescription();
        desc.setAlarmCode(new BigInteger(ALARM_CODE));
        desc.setAlarmText(ALARM_MESSAGE);
        msg.setAlarmDescription(desc);
        handler.notify(msg);

        addStep("Tests the handling of other objects, in this case an exception", "Should be written to the log.");
        handler.notify(new NullPointerException(EXCEPTION_MESSAGE));
    }
}
