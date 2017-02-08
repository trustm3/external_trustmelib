/*
 * This file is part of trust|me
 * Copyright(c) 2013 - 2017 Fraunhofer AISEC
 * Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU General Public License,
 * version 2 (GPL 2), as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GPL 2 license for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses/>
 *
 * The full GNU General Public License is included in this distribution in
 * the file called "COPYING".
 *
 * Contact Information:
 * Fraunhofer AISEC <trustme@aisec.fraunhofer.de>
 */

package de.fraunhofer.aisec.trustme.cmlcom;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This abstract class receives messages (usually protobuf messages) over
 * an associated socket input stream. The abstract method handleMessage will
 * be invoked for each received message in the order received.
 */
public abstract class Receiver implements Runnable {
    protected InputStream socketInputStream;
    protected DataInputStream dataInputStream;
    private MessageAfterMarker messageAfterMarker = new MessageAfterMarker();

    public Receiver(InputStream socketInputStream) {
        super();
        setSocketInputStream(socketInputStream);
    }

    /**
     * Handles a received message. Will be invoked for each received message
     * in the order received. The received (encoded) message likely needs to
     * be converted to a specific protobuf message, for example, by using
     * something like: CService.CmldToServiceMessage.parseFrom(encodedMessage).
     */
    protected abstract void handleMessage(byte[] encodedMessage) throws Exception;

    /**
     * Sets a marker in time such that a subsequent call to getMessageAfterMarker()
     * will return the (chronologically) first message that has been received after
     * this (time) marker. The methods setMarker() and getMessageAfterMarker() may
     * be used as follows:
     *
     * <pre>
     * {@code
     * receiver.setMarker();
     * sender.sendMessageSync(message);
     * byte[] response = receiver.getMessageAfterMarker();
     * }
     * </pre>
     *
     * Alternatively, the convenience method sendMessageSyncWithResponse may be used
     * instead which uses setMarker() and getMessageAfterMarker() transparently for
     * the user:
     *
     * <pre>
     * {@code
     * byte[] response = sender.sendMessageSyncWithResponse(message, receiver);
     * }
     * </pre>
     *
     * @see getMessageAfterMarker
     */
    public void setMarker() {
        messageAfterMarker.reset();
    }

    /**
     * Returns the (chronologically) first message that was or will be received
     * after setMarker() has been called. In case no message has yet been received
     * after the call to setMarker(), this method will block until a message
     * arrives and then return it. This method may be used in scenarios where
     * the caller does not want to use event-based mechanisms (i.e. handleMessage)
     * but may prefer sequential processing instead. Note that handleMessage still
     * gets called for the message returned here.
     *
     * @see setMarker
     */
    public byte[] getMessageAfterMarker() {
        return messageAfterMarker.get();
    }

    /**
     * Sets the socket input stream this Receiver is associated with.
     */
    public void setSocketInputStream(InputStream socketInputStream) {
        this.socketInputStream = socketInputStream;
        this.dataInputStream = new DataInputStream(socketInputStream);
    }

    /**
     * Returns the socket input stream this Receiver is associated with.
     */
    public InputStream getSocketInputStream() {
        return socketInputStream;
    }

    /**
     * This method will be invoked whenever an exception occurs in the
     * Receiver's run method.
     */
    protected abstract void exceptionHandler(Exception e);

    /**
     * Receiver main
     */
    public void run() {
        for (;;) {
            try {
                byte[] message = recvMessage();
                messageAfterMarker.set(message);
                handleMessage(message);
            }
            catch (Exception e) {
                exceptionHandler(e);
            }
        }
    }

    /**
     * Reads a single message (prefixed with its length) received from cmld.
     */
    private byte[] recvMessage() throws IOException {
        int messageLength = dataInputStream.readInt();
        assert(messageLength >= 0);

        byte[] encodedMessage = new byte[messageLength];
        dataInputStream.readFully(encodedMessage);

        return encodedMessage;
    }
}

class MessageAfterMarker {
    private byte[] message;

    public synchronized byte[] get() {
        while (message == null) {
            try {
                wait();
            }
            catch (InterruptedException e) {}
        }
        return message;
    }

    public synchronized void set(byte[] message) {
        if (this.message == null)
            this.message = message;
        notifyAll();
    }

    public synchronized void reset() {
        message = null;
    }
}
