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

import java.io.Serializable;

import android.content.Context;
import android.graphics.Color;

import android.util.Log;

import java.io.IOException;

import de.fraunhofer.aisec.trustme.Container.ContainerConfig;

public class ContainerItem implements Serializable {
    private String label;
    private String name;
    private String uuid;
    private int color;
    private byte[] screenshot;

    private boolean phone_ability;
    private boolean internet_ability;
    private boolean screenshot_enabled;
    private boolean encryption;
    private float partition_size;
    private boolean new_mail_notification;
    private boolean new_phone_notification;

    private boolean isRunning;

    public ContainerItem(String name, String color, byte[] screenshot, float partition_size, boolean encryption) {
        this.name = name;
        setColorFromString(color);
        this.screenshot = screenshot;
        this.partition_size = partition_size;
        this.encryption = encryption;
    }

    public ContainerItem() {}

    public ContainerItem(String name) {
        this.name = name;
    }

    public ContainerItem(String uuid, String name, boolean isRunning) {
        this.uuid = uuid;
        this.name = name;
        this.isRunning = isRunning;
    }

    // Sets led-color, encryption and phonecapability from config
    public void setItemFromConfig(Communicator communicator, Context context) {

        try {
            ContainerConfig conf = communicator.getContainerConfig(uuid);
            Log.d("ContainerItem","Got color: " + conf.color);
            setColorFromConf(conf.color);
        } catch (IOException exp) {
            // TODO replace this stub with real config data
            if (this.name.equals("a1")) {
                setColorFromString("red");
                this.phone_ability = true;
            } else if (this.name.equals("a2")) {
                setColorFromString("blue");
                this.phone_ability = false;
            }
        }

        this.phone_ability = true;
        this.encryption = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public byte[] getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(byte[] screenshot) {
        this.screenshot = screenshot;
    }

    public boolean isNew_phone_notification() {
        return new_phone_notification;
    }

    public void setNew_phone_notification(boolean new_phone_notification) {
        this.new_phone_notification = new_phone_notification;
    }

    public boolean isNew_mail_notification() {
        return new_mail_notification;
    }

    public void setNew_mail_notification(boolean new_mail_notification) {
        this.new_mail_notification = new_mail_notification;
    }

    public float getPartition_size() {
        return partition_size;
    }

    public void setPartition_size(float partition_size) {
        this.partition_size = partition_size;
    }


    public boolean isEncryption() {
        return encryption;
    }

    public void setEncryption(boolean encryption) {
        this.encryption = encryption;
    }

    public boolean isScreenshot_enabled() {
        return screenshot_enabled;
    }

    public void setScreenshot_enabled(boolean screenshot_enabled) {
        this.screenshot_enabled = screenshot_enabled;
    }

    public boolean isPhone_ability() {
        return phone_ability;
    }

    public void setPhone_ability(boolean phone_ability) {
        this.phone_ability = phone_ability;
    }

    public boolean isInternet_ability() {
        return internet_ability;
    }

    public void setInternet_ability(boolean internet_ability) {
        this.internet_ability = internet_ability;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    private void setColorFromConf(int color) {
        Log.d("ContainerItem","Color in hex: 0x" + Integer.toHexString(color));
        // Values transmitted are RGBA, so I have to shift the bits accordingly.
        int red = (color >> 24) & 0xFF;
        int green = (color >> 16) & 0xFF;
        int blue = (color >> 8) & 0xFF;
        this.color = Color.rgb(red,green,blue);
    }

    public void setColorFromString(String stringColor) {
        if (stringColor.equals("blue")) {
            this.color = Color.rgb(17, 19, 189);
            //this.color = -16776961;
        }
        if (stringColor.equals("red")) {
            this.color = Color.rgb(161, 14, 14);
            //this.color = -65536;
        }
        if (stringColor.equals("green")) {
            this.color = Color.rgb(50, 140, 56);
            //this.color = -16711936;
        }
        if (stringColor.equals("none")) {
            this.color = Color.rgb(0, 0, 0);
            //this.color = 0;
        }
    }

    public String getStringColor() {
        String stringColor = "";
        if (this.color == -16776961) {
            stringColor = "blue";
        }
        if (this.color == -65536) {
            stringColor = "red";
        }
        if (this.color == -16711936) {
            stringColor = "green";
        }
        if (this.color == 0) {
            stringColor = "none";
        }
        return stringColor;
    }
}

