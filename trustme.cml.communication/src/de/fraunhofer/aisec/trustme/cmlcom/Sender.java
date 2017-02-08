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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.protobuf.nano.MessageNano;

/**
 * This abstract class sends protobuf messages over an associated socket output
 * stream. Messages enqueued for sending will be processed in the order enqueued.
 */
public abstract class Sender implements Runnable {
    private OutputStream socketOutputStream;
    private DataOutputStream dataOutputStream;
    private final BlockingQueue<MessageNano> outgoingMessageQueue;
    private int socketSendBufferSize = 1024*1024;

    public Sender(OutputStream socketOutputStream) {
        super();
        setSocketOutputStream(socketOutputStream);
        this.outgoingMessageQueue = new ArrayBlockingQueue<MessageNano>(32);
    }

    /**
     * Enqueues a new message for sending.
     */
    public void sendMessage(MessageNano message) {
        try {
            outgoingMessageQueue.put(message);
        }
        catch (InterruptedException e) {}
    }

    /**
     * Sends a new message and returns only once the message has really been
     * sent. This method may be used in non-asynchronous scenarios.
     *
     * @see sendMessageSyncWithResponse
     */
    public synchronized void sendMessageSync(MessageNano message) {
        try {
            sendMessageInternal(message);
            dataOutputStream.flush();
        }
        catch (Exception e) {
            exceptionHandler(e);
        }
    }

    /**
     * Sends a new message and returns the message's response. This method
     * blocks until the response has been received and then returns it.
     * Note that this (convenience) method may be used instead of dealing
     * with Receiver.setMarker and Receiver.getMessageAfterMarker manually.
     */
    public synchronized byte[] sendMessageSyncWithResponse(MessageNano message, Receiver receiver) {
        receiver.setMarker();
        sendMessageSync(message);
        return receiver.getMessageAfterMarker();
    }

    /**
     * Sets the socket output stream this Sender is associated with.
     */
    public void setSocketOutputStream(OutputStream socketOutputStream) {
        this.socketOutputStream = socketOutputStream;
        this.dataOutputStream = new DataOutputStream(socketOutputStream);
    }

    /**
     * Returns the socket output stream this Sender is associated with.
     */
    public OutputStream getSocketOutputStream() {
        return socketOutputStream;
    }

    /**
     * Sets the socket send buffer size of the Sender.
     * The _maximum_ socket send buffer size is set by cmld to a reasonable
     * size (e.g. 1MB) in order to allow the sending of moderately
     * large messages (e.g. wallpaper data) over the unix domain socket
     * without any blocking. This method may be used to set a limit (which
     * should be less than or equal to the maximum socket send buffer size
     * set by cmld) for the Sender such that larger messages will not be
     * sent by the Sender as otherwise it might block for several seconds.
     */
    public void setSocketSendBufferSize(int size) {
        socketSendBufferSize = size;
    }

    /**
     * Returns the socket send buffer size of the Sender.
     */
    public int getSocketSendBufferSize() {
        return socketSendBufferSize;
    }

    /**
     * This method will be invoked whenever an exception occurs in the
     * Sender's run method.
     */
    protected abstract void exceptionHandler(Exception e);

    /**
     * Sender main.
     * Loops and waits until another thread notifies us of data to be sent to cmld.
     */
    public void run() {
        for (;;) {
            try {
                MessageNano message = outgoingMessageQueue.take();
                sendMessageInternal(message);
            }
            catch (Exception e) {
                exceptionHandler(e);
            }
        }
    }

    private synchronized void sendMessageInternal(MessageNano message) throws Exception {
        byte[] encodedMessage = MessageNano.toByteArray(message);

        if (encodedMessage.length + 4 > socketSendBufferSize) {
            throw new Exception("Trying to send a message to cmld which exceeds socket send buffer size"
                              + " (" + socketSendBufferSize + "). "
                              + " Not sending as we would likely block for several seconds.");
        }

        dataOutputStream.writeInt(encodedMessage.length); // length prefix
        dataOutputStream.write(encodedMessage, 0, encodedMessage.length); // payload
    }
}
