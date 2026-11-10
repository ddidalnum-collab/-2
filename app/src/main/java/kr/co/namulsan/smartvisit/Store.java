package kr.co.namulsan.smartvisit;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

final class Store {
    private static final String PREF = "smart_visit_private_v1"; // preserve profile when upgrading
    static SharedPreferences p(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
    static boolean enabled(Context c) { return p(c).getBoolean("enabled", false); }
    static String company(Context c) { return p(c).getString("company", "디지털남울산센터"); }
    static String name(Context c) { return p(c).getString("name", ""); }
    static int duplicateHours(Context c) { return p(c).getInt("duplicate_hours", 24); }
    static String pendingNumber(Context c) { return p(c).getString("pending_number", ""); }
    static long pendingAt(Context c) { return p(c).getLong("pending_at", 0); }
    static boolean ready(Context c) { return p(c).getBoolean("ready", false); }
    static boolean wasOffhook(Context c) { return p(c).getBoolean("was_offhook", false); }

    static void saveProfile(Context c, String company, String name, boolean enabled, int hours) {
        p(c).edit().putString("company",company.trim()).putString("name",name.trim())
             .putBoolean("enabled",enabled).putInt("duplicate_hours",hours).commit();
    }
    /** Number is supplied by Telecom call screening, never by a call this app placed. */
    static void stageDetectedOutgoing(Context c, String number) {
        // PHONE_STATE OFFHOOK may arrive before Telecom supplies the outgoing number.
        // Pair only a currently active, very recent OFFHOOK state; avoid older calls.
        boolean active= "OFFHOOK".equals(p(c).getString("last_state", ""))
                && System.currentTimeMillis()-p(c).getLong("last_state_at", 0) < 15000L;
        p(c).edit().putString("pending_number",number).putLong("pending_at",System.currentTimeMillis())
             .putBoolean("was_offhook",active).putBoolean("ready",false).commit();
    }
    static void recordPhoneState(Context c, String state) {
        p(c).edit().putString("last_state", state==null?"":state)
            .putLong("last_state_at",System.currentTimeMillis()).commit();
    }
    static void setReady(Context c, boolean ready) { p(c).edit().putBoolean("ready",ready).commit(); }
    static void stateOffhook(Context c) { p(c).edit().putBoolean("was_offhook",true).commit(); }
    static void clearPending(Context c) {
        p(c).edit().remove("pending_number").remove("pending_at")
            .putBoolean("was_offhook",false).putBoolean("ready",false).commit();
    }
    static void setIdleAndReady(Context c) {
        p(c).edit().putBoolean("was_offhook",false).putBoolean("ready",true).commit();
    }
    static String normalize(String phone) { return phone.replaceAll("[^0-9+]", ""); }
    static boolean validNumber(String phone) {
        String v=normalize(phone);
        return v.matches("\\+?[0-9]{8,15}");
    }
    static String masked(String number) {
        String n=normalize(number);
        if (n.length()<7) return "****";
        return n.substring(0,Math.min(3,n.length()-4)) + "-****-" + n.substring(n.length()-4);
    }
    static JSONArray history(Context c) {
        try { return new JSONArray(p(c).getString("history", "[]")); }
        catch(Exception ignored) { return new JSONArray(); }
    }
    static void append(Context c, String number, String status) {
        JSONArray old=history(c); JSONArray next=new JSONArray();
        JSONObject entry=new JSONObject();
        try {
            entry.put("when",System.currentTimeMillis());
            entry.put("number",normalize(number));
            entry.put("status",status);
        } catch(Exception ignored) {}
        next.put(entry);
        for(int i=0;i<Math.min(99,old.length());i++) next.put(old.optJSONObject(i));
        p(c).edit().putString("history",next.toString()).commit();
    }
    static boolean duplicate(Context c, String number) {
        String normalized=normalize(number); long cutoff=System.currentTimeMillis()-duplicateHours(c)*3600000L;
        JSONArray arr=history(c);
        for(int i=0;i<arr.length();i++) {
            JSONObject j=arr.optJSONObject(i);
            if(j!=null && ("사용자 발송 확인".equals(j.optString("status"))
                    ||"MMS 전송 요청".equals(j.optString("status"))
                    ||"통신사 MMS 전송 성공 응답".equals(j.optString("status"))) &&
                normalized.equals(j.optString("number")) && j.optLong("when")>=cutoff) return true;
        }
        return false;
    }
}
