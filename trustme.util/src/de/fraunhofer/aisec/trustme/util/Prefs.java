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

package de.fraunhofer.aisec.trustme.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemProperties;

public class Prefs {

    private static final String TAG = "trustmelib.Prefs";

    private static final Intent SERVICE_POPUP = new Intent().setComponent(new ComponentName(
        "de.fraunhofer.aisec.trustme.service", "de.fraunhofer.aisec.trustme.service.PopupActivity"));

    /**
     * Check if we are privileged and are able to configure privileged services,
     * use this in the corresponding tiles for privileged services such as wifi or airplane mode.
     *
     * If we are not priveleged and switchDialog is set true, a POPUP Intent is generated for the TrustmeService.
     * This shows a corresponding message and offers to switch to privileged Container instance which is
     * able to configure the asked service.
     *
     * @param activity which should show the switch dialog; if NULL don't show the dialog.
     */
    public static boolean canManagePrivilegedServices(Activity activity) {
        boolean privileged = SystemProperties.getBoolean("ro.trustme.a0", false);
        if (!privileged && activity != null) {
            SERVICE_POPUP.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            activity.startActivity(SERVICE_POPUP);
        }
        return privileged;
    }

    /**
      * Check if we are configured (allowed) for telephony
      */
    public static boolean hasFeatureTelephony() {
        return (SystemProperties.getBoolean("ro.trustme.telephony", false));
    }

    /**
      * Provides a version string with or without preformated output
      *
      * @param preformated set true for formated output
      */
    public static String getTrustmeVersion(boolean preformated) {
        String trustmeVersion = SystemProperties.get("ro.trustme.version", null);
        if (trustmeVersion == null) {
            return "";
        }
        if (preformated) {
            return "(trust-me: " + trustmeVersion + ")";
        } else {
            return trustmeVersion;
        }
    }
}
