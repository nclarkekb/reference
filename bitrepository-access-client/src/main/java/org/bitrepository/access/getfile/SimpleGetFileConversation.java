/*
 * #%L
 * bitrepository-access-client
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2010 The State and University Library, The Royal Library and The State Archives, Denmark
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
package org.bitrepository.access.getfile;

import org.bitrepository.access.AccessException;
import org.bitrepository.bitrepositoryelements.ResponseInfo;
import org.bitrepository.bitrepositorymessages.GetFileComplete;
import org.bitrepository.bitrepositorymessages.GetFileRequest;
import org.bitrepository.bitrepositorymessages.GetFileResponse;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileResponse;
import org.bitrepository.common.utils.FileUtils;
import org.bitrepository.protocol.AbstractMessagebusBackedConversation;
import org.bitrepository.protocol.MessageBus;
import org.bitrepository.protocol.MessageSender;
import org.bitrepository.protocol.ProtocolComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A conversation for GetFile.
 *
 * Logic for behaving sanely in GetFile conversations.
 *
 * TODO move all the message generations into separate methods, or use the auto-generated constructors, which Mikis
 * has talked about.
 */
public class SimpleGetFileConversation extends AbstractMessagebusBackedConversation<File> {
    /** Protocol version used. */
    //TODO These constants should be defined elsewhere!
    public static final long PROTOCOL_VERSION = 1L;
    /** Protocol minimum version used. */
    //TODO These constants should be defined elsewhere!
    public static final long PROTOCOL_MIN_VERSION = 1L;
    /** Defines that the timer is a daemon thread. */
    private static final Boolean TIMER_IS_DAEMON = true;
    /** The amount of milliseconds to wait before executing the timertask when waiting for identify responses. */
    // TODO replace with configuration.
    private static final Long TIMER_TASK_DELAY = 100000L;

    /** The log for this class. */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** The directory where the retrieved files should be placed. */
    private final File fileDir;
    /** Expected number of pillars to respond when identifying pillars. */
    private final int expectedNumberOfPillars;
    /** The timeout when getting a file. */
    private long getFileTimeout;

    /**
     * The replies registered for each pillar to deliver this file. Maps between
     * pillar id and the replies received from the pillar.
     */
    private final Map<String, IdentifyPillarsForGetFileResponse> pillarTime
            = Collections.synchronizedMap(new HashMap<String, IdentifyPillarsForGetFileResponse>());

    /** The timer. Schedules conversation timeouts for this conversation. */
    private final Timer timer = new Timer(TIMER_IS_DAEMON);
    /** The timer task for timeout of identify in this conversation. */
    private final TimerTask identifyTimeoutTask = new IdentifyTimerTask();
    /** The timer task for timeout of getFile in this conversation. */
    private final TimerTask getFileTimeoutTask = new GetFileTimerTask();

    /** Whether the conversation has ended. */
    private boolean ended = false;
    /** The result of this conversation. */
    private File result;

    /**
     * Initialises the file directory, and the message bus used for sending messages.
     * The fileDir is retrieved from the configuration.
     *
     * @param messageBus The message bus used for sending messages.
     * @param expectedNumberOfPillars The number of pillars to wait for replies from, when identifying pillars.
     * @param getFileDefaultTimeout The timeout when getting a file. If the conversation identifies pillars, this value
     * is replaced by twice the time the pillar estimated.
     * @param fileDir The directory to store retrieved files in.
     */
    public SimpleGetFileConversation(MessageBus messageBus, int expectedNumberOfPillars, long getFileDefaultTimeout,
                                     String fileDir) {
        super(messageBus);

        this.expectedNumberOfPillars = expectedNumberOfPillars;
        this.getFileTimeout = getFileDefaultTimeout;

        // retrieve the directory for delivering the output files.
        // TODO: Should we really have files?
        this.fileDir = FileUtils.retrieveDirectory(fileDir);
    }

    @Override
    public boolean isEnded() {
        return ended;
    }

    @Override
    public File getResult() {
        return result;
    }

    /**
     * Sends the message as expected, but also sets up a timer task, that times out if we wait too long for enough
     * replies.
     *
     * @see MessageSender#sendMessage(String, IdentifyPillarsForGetFileRequest)
     *
     * @param destination Destination of message.
     * @param message Message to send.
     *
     * @return id of message sent.
     */
    @Override
    public String sendMessage(String destination, IdentifyPillarsForGetFileRequest message) {
        String id = super.sendMessage(destination, message);
        // add this as a task for the timer.
        timer.schedule(identifyTimeoutTask, TIMER_TASK_DELAY);
        return id;
    }

    /**
     * Sends the message as expected, but also sets up a timer task, that times out if we wait too long for enough
     * replies.
     *
     * @see MessageSender#sendMessage(String, GetFileRequest)
     *
     * @param destination Destination of message.
     * @param message Message to send.
     *
     * @return id of message sent.
     */
    @Override
    public String sendMessage(String destination, GetFileRequest message) {
        String id = super.sendMessage(destination, message);
        // add this as a task for the timer.
        timer.schedule(getFileTimeoutTask, getFileTimeout);
        return id;
    }

    /**
     * Handles a reply for identifying the fastest pillar to deliver a given file.
     * Removes the pillar responsible for the reply from the outstanding list for the file in the reply. If no more
     * pillars are outstanding, then the file is requested from fastest pillar.
     *
     * @param reply The IdentifyPillarsForGetFileReply to handle.
     */
    @Override
    public void onMessage(IdentifyPillarsForGetFileResponse reply) {
        // validate arguments
        if (reply == null) {
            throw new IllegalArgumentException("The IdentifyPillarsForGetFileReply reply may not be null.");
        }

        pillarTime.put(reply.getPillarID(), reply);
        if (pillarTime.size() == expectedNumberOfPillars) {
            // stop the timer task for this outstanding instance, and then get file from fastest pillar
            identifyTimeoutTask.cancel();
            // TODO: Race condition, what if timeout task already triggered this just before now
            getFileFromFastest();
        }

    }

    /**
     * Method for handling the GetFileResponse messages.
     * Currently logs the response, but does nothing else.
     *
     * @param msg The GetFileResponse message to be handled by this method.
     */
    @Override
    public void onMessage(GetFileResponse msg) {
        // TODO Something else?
        ResponseInfo info = msg.getResponseInfo();
        SimpleGetFileConversation.this.log
                .info("Received response for retrieval of file '" + msg.getFileID() + "' in SLA '" + msg.getSlaID()
                              + "' from pillar '" + msg.getPillarID() + "' with the following message: \n"
                              + info.getResponseCode() + " : " + info.getResponseText());
    }

    /**
     * Method for completing the get.
     * Downloads the file, and remembers the result. Then marks the conversation as ended.
     *
     * @param msg The GetFileCompleteMessage.
     */
    @Override
    public void onMessage(GetFileComplete msg) {
        if (msg == null) {
            throw new IllegalArgumentException("Cannot handle a null as GetFileComplete.");
        }

        getFileTimeoutTask.cancel();
        // TODO: Race condition, what if timeout task already triggered this just before now
        String id = msg.getFileID();
        try {
            // TODO: Should we really download the file, or is the interface the URL?
            SimpleGetFileConversation.this.log
                    .info("Downloading the file '" + id + "' from '" + msg.getFileAddress() + "'.");

            URL url = new URL(msg.getFileAddress());

            File outputFile = new File(fileDir, id);

            // Handle the scenario when a file already exists by deprecating the old one (move to '*.old')
            if (outputFile.exists()) {
                SimpleGetFileConversation.this.log.warn("The file '" + id + "' does already exist. Moving old one.");
                moveDeprecatedFile(outputFile);
            }

            FileOutputStream outStream = null;
            try {
                // download the file.
                outStream = new FileOutputStream(outputFile);
                ProtocolComponentFactory.getInstance().getFileExchange().downloadFromServer(outStream, url);
                outStream.flush();
            } finally {
                if (outStream != null) {
                    outStream.close();
                }
            }

            result = outputFile;
        } catch (Exception e) {
            throw new AccessException("Problems with retrieving the file '" + id + "'.", e);
        } finally {
            endConversation();
        }
    }

    /** Method for making the client send a request for the file to the fastest pillar. */
    private void getFileFromFastest() {
        IdentifyPillarsForGetFileResponse response = null;
        Long timeToDeliver = Long.MAX_VALUE;
        for (Map.Entry<String, IdentifyPillarsForGetFileResponse> entry : pillarTime.entrySet()) {
            if (entry.getValue().getTimeToDeliver().getTimeMeasureValue().longValue() < timeToDeliver) {
                response = entry.getValue();
                // TODO Unit is ignored! Assumed to be ms
                timeToDeliver = entry.getValue().getTimeToDeliver().getTimeMeasureValue().longValue();
            }
        }

        if (response == null) {
            endConversation();
            // TODO But reporting an actual time to deliver is optional ?!
            throw new AccessException("Cannot request file, since the no pillar with a "
                                              + "valid time has replied. Number of replies: '" + pillarTime.size()
                                              + "'");
        }

        // TODO Hardcoded doubling of timeout
        getFileTimeout = 2 * timeToDeliver;
        // make the client perform the request for the file from the fastest pillar.
        URL url;
        try {
            url = ProtocolComponentFactory.getInstance().getFileExchange().getURL(response.getFileID());
        } catch (MalformedURLException e) {
            throw new AccessException("Unable to create file from URL", e);
        }
        GetFileRequest msg = new GetFileRequest();
        msg.setSlaID(response.getSlaID());
        msg.setFileAddress(url.toExternalForm());
        msg.setFileID(response.getFileID());
        msg.setPillarID(response.getPillarID());
        msg.setReplyTo(response.getReplyTo());
        msg.setMinVersion(BigInteger.valueOf(PROTOCOL_MIN_VERSION));
        msg.setVersion(BigInteger.valueOf(PROTOCOL_VERSION));

        sendMessage(response.getReplyTo(), msg);
    }

    /**
     * Mark this conversation as ended, and notifies whoever waits for it to end.
     */
    private void endConversation() {
        ended = true;
        synchronized (this) {
            notifyAll();
        }
        mediator.endConversation(this);
    }

    /**
     * Method for deprecating a file by renaming it to '*.old' (where '*' is the current file name).
     * If an old version already exists, then rename it to '*.old.old', etc.
     * Should also work with a directory, though not be intended.
     *
     * TODO verify!
     *
     * @param current The current file to deprecate. Aka move to old.
     */
    private void moveDeprecatedFile(File current) {
        File newLocation = new File(current.getParent(), current.getName() + ".old");
        // Handle scenario, when a old copy already exists. Move the old one too! (to old.old)
        if (newLocation.exists()) {
            moveDeprecatedFile(newLocation);
        }
        current.renameTo(newLocation);
    }

    /**
     * The timer task class for the outstanding identify requests. When the time is reached the fastest pillar should
     * be called requested for the delivery of the file.
     */
    private class IdentifyTimerTask extends TimerTask {
        @Override
        public void run() {
            //TODO: Exception handler needed, this is a thread
            log.debug("Time has run out for identifying the fastest pillar.");
            getFileFromFastest();
        }
    }

    /**
     * The timer task class for the outstanding get file requests. When the time is reached the conversation should be
     * marked as ended.
     */
    private class GetFileTimerTask extends TimerTask {
        @Override
        public void run() {
            //TODO: Exception handler needed, this is a thread
            log.debug("Time has run out for getting file from pillar.");
            endConversation();
        }
    }
}