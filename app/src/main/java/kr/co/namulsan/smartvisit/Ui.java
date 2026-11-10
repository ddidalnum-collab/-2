package kr.co.namulsan.smartvisit;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Ui {
    static final int BLUE=Color.rgb(17,57,168), NAVY=Color.rgb(21,43,95);
    static int px(Context c,float dp) { return (int)(dp*c.getResources().getDisplayMetrics().density+0.5f); }
    static GradientDrawable bg(int color,int r,Context c) {
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(px(c,r)); return g;
    }
    static LinearLayout column(Context c) {
        LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(px(c,18),px(c,18),px(c,18),px(c,18));return l;
    }
    static ScrollView scroll(Context c, LinearLayout content) {
        ScrollView s=new ScrollView(c);s.setFillViewport(true);s.addView(content);return s;
    }
    static TextView text(Context c,String value,int sp,boolean bold,int color) {
        TextView t=new TextView(c);t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL); t.setLineSpacing(px(c,2),1f);
        return t;
    }
    static EditText edit(Context c,String value,String hint) {
        EditText e=new EditText(c); e.setText(value);e.setHint(hint);e.setSingleLine(true);
        e.setTextSize(17);e.setPadding(px(c,13),px(c,11),px(c,13),px(c,11));
        e.setBackground(bg(Color.WHITE,12,c));return e;
    }
    static Button button(Context c,String title,boolean primary) {
        Button b=new Button(c);b.setText(title);b.setAllCaps(false);b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(primary?Color.WHITE:NAVY);
        b.setBackground(bg(primary?BLUE:Color.rgb(232,239,252),12,c));
        b.setPadding(px(c,12),px(c,7),px(c,12),px(c,7));return b;
    }
    static void add(LinearLayout parent,View v,int top) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=px(parent.getContext(),top);parent.addView(v,p);
    }
    static void label(LinearLayout parent,String value,int top) {
        add(parent,text(parent.getContext(),value,14,true,NAVY),top);
    }
}
