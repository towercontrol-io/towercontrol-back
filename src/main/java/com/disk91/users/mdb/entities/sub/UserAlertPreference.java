package com.disk91.users.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;

public class UserAlertPreference implements CloneableObject<UserAlertPreference> {

        // accept email alert
        private boolean emailAlert;

        // accept sms alert
        private boolean smsAlert;

        // accept push alert
        private boolean pushAlert;

        // === GETTER / SETTER ===

        public boolean isEmailAlert() {
            return emailAlert;
        }

        public void setEmailAlert(boolean emailAlert) {
            this.emailAlert = emailAlert;
        }

        public boolean isSmsAlert() {
            return smsAlert;
        }

        public void setSmsAlert(boolean smsAlert) {
            this.smsAlert = smsAlert;
        }

        public boolean isPushAlert() {
            return pushAlert;
        }

        public void setPushAlert(boolean pushAlert) {
            this.pushAlert = pushAlert;
        }

        // === INT ===

        public static UserAlertPreference of() {
            UserAlertPreference pref = new UserAlertPreference();
            pref.setEmailAlert(true);
            pref.setSmsAlert(false);
            pref.setPushAlert(false);
            return pref;
        }

        // === CLONE ===

        @Override
        public UserAlertPreference clone() {
            UserAlertPreference u = new UserAlertPreference();
            u.setEmailAlert(this.emailAlert);
            u.setSmsAlert(this.smsAlert);
            u.setPushAlert(this.pushAlert);
            return u;
        }
}
