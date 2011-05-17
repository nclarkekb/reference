package org.bitrepository.protocol;

import org.bitrepository.bitrepositorymessages.GetChecksumsComplete;
import org.bitrepository.bitrepositorymessages.GetChecksumsRequest;
import org.bitrepository.bitrepositorymessages.GetChecksumsResponse;
import org.bitrepository.bitrepositorymessages.GetFileComplete;
import org.bitrepository.bitrepositorymessages.GetFileIDsComplete;
import org.bitrepository.bitrepositorymessages.GetFileIDsRequest;
import org.bitrepository.bitrepositorymessages.GetFileIDsResponse;
import org.bitrepository.bitrepositorymessages.GetFileRequest;
import org.bitrepository.bitrepositorymessages.GetFileResponse;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetChecksumsRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetChecksumsResponse;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileIDsRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileIDsResponse;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileResponse;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForPutFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForPutFileResponse;
import org.bitrepository.bitrepositorymessages.PutFileComplete;
import org.bitrepository.bitrepositorymessages.PutFileRequest;
import org.bitrepository.bitrepositorymessages.PutFileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract super class for conversations. This super class will handle sending all messages with the correct
 * conversation id, and simply log messages received. Overriding implementations should override the behaviour for
 * receiving specific messages.
 *
 * @param <T> The result of this conversation.
 */
public abstract class AbstractMessagebusBackedConversation<T> implements Conversation<T> {
    /** The message bus used for sending messages. */
    private final MessageBus messagebus;
    /** The conversation ID. Null until the first message is sent. */
    private String conversationID;
    /** The conversation mediator that handles this conversation. */
    protected ConversationMediator mediator;
    /** The logger for this class. */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Initialise a conversation on the given messagebus.
     *
     * @param messagebus The message bus used for exchanging messages.
     */
    public AbstractMessagebusBackedConversation(MessageBus messagebus) {
        this.messagebus = messagebus;
    }


    @Override
    public void setMediator(ConversationMediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public String getConversationID() {
        return conversationID;
    }

    @Override
    public T waitFor() {
        synchronized (this) {
            while (!isEnded()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        return getResult();
    }

    @Override
    public T waitFor(long timeout) throws ConversationTimedOutException {
        long time = System.currentTimeMillis();
        synchronized (this) {
            while (!isEnded() && time + timeout < System.currentTimeMillis()) {
                try {
                    wait(timeout);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        if (!isEnded()) {
            throw new ConversationTimedOutException("Conversation timed out");
        }
        return getResult();
    }

    @Override
    public String sendMessage(String destinationId, GetChecksumsComplete content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, GetChecksumsRequest content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, GetChecksumsResponse content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, GetFileComplete content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, GetFileIDsComplete content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, GetFileIDsRequest content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, GetFileIDsResponse content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, GetFileRequest content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, GetFileResponse content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, IdentifyPillarsForGetChecksumsResponse content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, IdentifyPillarsForGetChecksumsRequest content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, IdentifyPillarsForGetFileIDsRequest content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, IdentifyPillarsForGetFileIDsResponse content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, IdentifyPillarsForGetFileRequest content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, IdentifyPillarsForGetFileResponse content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, IdentifyPillarsForPutFileResponse content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, IdentifyPillarsForPutFileRequest content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, PutFileComplete content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, PutFileRequest content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public String sendMessage(String destinationId, PutFileResponse content) {
        content.setCorrelationID(getConversationID());
        String id = messagebus.sendMessage(destinationId, content);
        if (conversationID == null) {
            conversationID = id;
        }
        return id;
    }

    @Override
    public void onMessage(GetChecksumsComplete message) {
       log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(GetChecksumsRequest message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(GetChecksumsResponse message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(GetFileComplete message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(GetFileIDsComplete message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(GetFileIDsRequest message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(GetFileIDsResponse message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(GetFileRequest message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(GetFileResponse message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(IdentifyPillarsForGetChecksumsResponse message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(IdentifyPillarsForGetChecksumsRequest message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(IdentifyPillarsForGetFileIDsResponse message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(IdentifyPillarsForGetFileIDsRequest message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(IdentifyPillarsForGetFileResponse message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(IdentifyPillarsForGetFileRequest message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(IdentifyPillarsForPutFileResponse message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(IdentifyPillarsForPutFileRequest message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(PutFileComplete message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(PutFileRequest message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }

    @Override
    public void onMessage(PutFileResponse message) {
        log.debug("Received message " + message.getCorrelationID() + " but did not know how to handle it.");
    }
}