package kr.co.namulsan.smartvisit;

import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.PhoneAccount;

/**
 * Receives dialled number from Android Telecom when the user grants the
 * call-screening role. It does NOT initiate/redirect calls, screen/block
 * calls, or replace the default Phone application.
 *
 * Limits: only calls supplied by Telecom are visible, and a number that is
 * in Contacts may be withheld unless READ_CONTACTS is separately granted.
 */
public final class OutgoingCallScreeningService extends CallScreeningService {
    @Override public void onScreenCall(Call.Details details) {
        if (Build.VERSION.SDK_INT < 29 || details == null) return;
        int direction = details.getCallDirection();
        if (direction == Call.Details.DIRECTION_INCOMING) {
            // An ordinary incoming call must be allowed promptly. Never filter.
            respondToCall(details, new CallResponse.Builder().build());
            return;
        }
        if (direction != Call.Details.DIRECTION_OUTGOING || !Store.enabled(this)) return;
        Uri handle = details.getHandle();
        if (handle == null || !PhoneAccount.SCHEME_TEL.equals(handle.getScheme())) return;
        String number = Store.normalize(handle.getSchemeSpecificPart());
        if (Store.validNumber(number)) Store.stageDetectedOutgoing(this, number);
    }
}
