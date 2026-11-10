package kr.co.namulsan.smartvisit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/**
 * Matches a recently screened outgoing call to OFFHOOK -> IDLE.
 * OFFHOOK alone does not prove the callee answered; the user decides
 * whether to send after a call attempt ends.
 */
public final class PhoneStateReceiver extends BroadcastReceiver {
    private static final long MAX_PENDING_MS=60*60*1000L;
    @Override public void onReceive(Context c, Intent intent) {
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) return;
        String state=intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        // Always record state first: Telecom may deliver screening after OFFHOOK.
        Store.recordPhoneState(c,state);
        if (!Store.enabled(c)) return;
        long start=Store.pendingAt(c);
        if (start<=0 || System.currentTimeMillis()-start>MAX_PENDING_MS) return;
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            // Incoming activity must not reuse an older outgoing number.
            Store.clearPending(c);
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            Store.stateOffhook(c);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)
                && Store.wasOffhook(c) && !Store.ready(c)) {
            Store.setIdleAndReady(c);
            if(!CallMonitorService.showOnTop(c)) NotificationHelper.show(c);
        }
    }
}
