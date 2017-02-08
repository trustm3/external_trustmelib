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

import android.content.Context;
import android.content.res.AssetManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import android.graphics.Color;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.protobuf.nano.MessageNano;
import de.fraunhofer.aisec.trustme.Control;
import de.fraunhofer.aisec.trustme.Container;
import de.fraunhofer.aisec.trustme.Control.ControllerToDaemon;
import de.fraunhofer.aisec.trustme.Control.DaemonToController;
import de.fraunhofer.aisec.trustme.Container.ContainerStatus;
import de.fraunhofer.aisec.trustme.Container.ContainerConfig;

public class Communicator {
    private static final String SOCK_ADDR = "/dev/socket/cml-control";
    private static final String TAG = "Communicator";

    private LocalSocket socket;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;

    private Sender sender;
    private CReceiver receiver;

    public Communicator() {
        try {
            socket = new LocalSocket(LocalSocket.SOCKET_STREAM);
            Log.d(TAG, "Trying to connect to socket " + SOCK_ADDR);
            socket.connect(new LocalSocketAddress(SOCK_ADDR, LocalSocketAddress.Namespace.FILESYSTEM));
            Log.d(TAG, "Successfully connected to socket");

            // Set up input and output streams.
            socketInputStream = socket.getInputStream();
            socketOutputStream = socket.getOutputStream();
        }
        catch (IOException e) {
            Log.d(TAG,"Couldn't connect to socket " + SOCK_ADDR);
            e.printStackTrace();
            cleanup();
            return;
        }

        // Start sender thread.
        sender = new Sender(socketOutputStream) {
            @Override
            protected void exceptionHandler(Exception e) {
                Log.e(TAG, "The Sender's run loop threw an exception: " + e.getMessage());
                // We don't exit here and let the Sender proceed.
            }
        };
        new Thread(sender).start();

        // Start receiver thread.
        receiver = new CReceiver(socketInputStream);
        new Thread(receiver).start();
    }

    public void startContainer(String uuid, String key) throws IOException, PasswordException, LockedTillRebootException, SmartcardException {
        Log.d(TAG, "Entering startContainer");
        Control.ContainerStartParams startParams;
        ControllerToDaemon msg;

        startParams = new Control.ContainerStartParams();
        startParams.key = key;

        msg = new ControllerToDaemon();
        msg.command = Control.ControllerToDaemon.CONTAINER_START;
        msg.containerUuids = new String[1];
        msg.containerUuids[0] = uuid;
        msg.containerStartParams = startParams;

        DaemonToController co_msg = DaemonToController.parseFrom(sender.sendMessageSyncWithResponse(msg, receiver));

        if (co_msg.response == Control.DaemonToController.CONTAINER_START_PASSWD_WRONG) {
            throw new PasswordException("Wrong container password");
        }
        else if (co_msg.response == Control.DaemonToController.CONTAINER_START_LOCKED_TILL_REBOOT) {
            throw new LockedTillRebootException("Too many wrong password attempts; token locked till next reboot");
        }
        else if (co_msg.response == Control.DaemonToController.CONTAINER_START_LOCK_FAILED ||
                 co_msg.response == Control.DaemonToController.CONTAINER_START_UNLOCK_FAILED) {
            throw new SmartcardException("Lock or unlock operation failed");
        }
    }

    public void stopContainer(String uuid) throws IOException {
        Log.d(TAG, "Entering stopContainer");
        ControllerToDaemon msg;

        msg = new ControllerToDaemon();
        msg.command = Control.ControllerToDaemon.CONTAINER_STOP;
        msg.containerUuids = new String[1];
        msg.containerUuids[0] = uuid;

        sender.sendMessage(msg);
    }

    public void switchTo(String uuid) {
        Log.d(TAG, "Entering switchTo");
        ControllerToDaemon msg;

        msg = new ControllerToDaemon();
        msg.command = Control.ControllerToDaemon.CONTAINER_SWITCH;
        msg.containerUuids = new String[1];
        msg.containerUuids[0] = uuid;

        sender.sendMessage(msg);
    }

    public int getContainerState(String uuid) throws IOException {
        Log.d(TAG,"Entering getContainerState");
        ControllerToDaemon msg;

        msg = new ControllerToDaemon();
        msg.command = Control.ControllerToDaemon.GET_CONTAINER_STATUS;
        msg.containerUuids = new String[1];
        msg.containerUuids[0] = uuid;

        DaemonToController co_msg = DaemonToController.parseFrom(sender.sendMessageSyncWithResponse(msg, receiver));

        ContainerStatus status = co_msg.containerStatus[0];
        return status.state;
    }

    public ContainerConfig getContainerConfig(String uuid) throws IOException {
        Log.d(TAG,"Entering getContainerConfig");
        ControllerToDaemon msg;

        msg = new ControllerToDaemon();
        msg.command = Control.ControllerToDaemon.GET_CONTAINER_CONFIG;
        msg.containerUuids = new String[1];
        msg.containerUuids[0] = uuid;

        DaemonToController co_msg = DaemonToController.parseFrom(sender.sendMessageSyncWithResponse(msg, receiver));

        ContainerConfig config = co_msg.containerConfigs[0];
        return config;
    }

    public ArrayList<ContainerItem> getContainers() throws IOException {
        Log.d(TAG, "Entering getContainers");
        ArrayList<ContainerItem> containers;
        ControllerToDaemon msg;

        containers = new ArrayList<ContainerItem>();

        msg = new ControllerToDaemon();
        msg.command = Control.ControllerToDaemon.GET_CONTAINER_STATUS;

        DaemonToController co_msg = DaemonToController.parseFrom(sender.sendMessageSyncWithResponse(msg, receiver));

        for (int i = 0; i < co_msg.containerStatus.length; ++i) {
            ContainerStatus cStatus = co_msg.containerStatus[i];

            //TODO hard coded stuff. reconsider. ignore a0.
            if (cStatus.name.equals("a0"))
                continue;

            ContainerItem contItem =
                new ContainerItem(cStatus.uuid,
                                    cStatus.name,
                                    (cStatus.state != Container.STOPPED));
            containers.add(contItem);
        }
        return containers;
    }

    /* Currently not used.
    public ArrayList<ContainerItem> getFakeContainers(){
        ArrayList<ContainerItem> containers = new ArrayList<ContainerItem>();

        for (int i = 0; i < 8; i++){
            ContainerItem contItem = 
                new ContainerItem("a1b2c3","a"+(i+1),false);
            contItem.setColor(Color.rgb(17, 19, 189));
            containers.add(contItem);
        }
        return containers;
    }
    */

    public void cleanup() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class CReceiver extends Receiver {
    private static final String TAG = "Communicator";

    public CReceiver(InputStream socketInputStream) {
        super(socketInputStream);
    }

    @Override
    protected void handleMessage(byte[] encodedMessage) throws IOException {
        // empty since we use non-async processing()
    }

    @Override
    protected void exceptionHandler(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        Log.e(TAG, "The Receiver's run loop threw an exception: " + exceptionAsString);
        // We don't exit here and let the Receiver proceed, except we got EOF.
        if (e instanceof EOFException)
            System.exit(-1);
    }
}
