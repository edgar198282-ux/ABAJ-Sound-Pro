package am.abaj.soundpro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG=0xff080f1d, PANEL=0xff142136, BLUE=0xff40c9fa, WHITE=0xfff2f7ff, MUTED=0xff94a6c0;
    private LinearLayout root,bands;
    private TextView status, device, mode;
    private Equalizer equalizer;
    private Button toggle;
    private boolean enabled;

    private int dp(float value){return (int)(getResources().getDisplayMetrics().density*value+.5f);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private LinearLayout column(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private void add(LinearLayout parent,View child,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(top);parent.addView(child,p);
    }
    private LinearLayout card(){
        LinearLayout l=column();l.setPadding(dp(18),dp(17),dp(18),dp(17));
        l.setBackground(shape(PANEL,20));add(root,l,14);return l;
    }
    private Button button(String value,int color){
        Button b=new Button(this);
        b.setAllCaps(false);b.setText(value);b.setTextColor(WHITE);
        b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(shape(color,14));b.setMinHeight(dp(52));
        return b;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        root=column();root.setPadding(dp(18),dp(25),dp(18),dp(28));
        scroll.addView(root);setContentView(scroll);

        TextView eyebrow=text("ABAJ  /  AUDIO LAB",12,BLUE,true);add(root,eyebrow,0);
        TextView title=text("Sound Pro",34,WHITE,true);add(root,title,3);
        TextView subtitle=text("Диагностика и настройка звука",14,MUTED,false);add(root,subtitle,3);

        LinearLayout info=card();
        add(info,text("УСТРОЙСТВО",11,MUTED,true),0);
        device=text(Build.MANUFACTURER+"  "+Build.MODEL,19,WHITE,true);add(info,device,8);
        add(info,text("Android "+Build.VERSION.RELEASE+"  •  Версия 0.1.5",12,MUTED,false),4);

        LinearLayout control=card();
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView section=text("ОБРАБОТКА ЗВУКА",12,MUTED,true);
        row.addView(section,new LinearLayout.LayoutParams(0,-2,1));
        mode=text("ВЫКЛ",12,MUTED,true);row.addView(mode);
        add(control,row,0);
        status=text("Эффекты отключены",15,WHITE,false);add(control,status,13);
        toggle=button("Включить эквалайзер",0xff197cae);add(control,toggle,16);
        toggle.setOnClickListener(v->{if(enabled)stop();else start();});

        LinearLayout tools=card();
        add(tools,text("ДИАГНОСТИКА",12,MUTED,true),0);
        add(tools,text("Проверка доступных эффектов Android",14,WHITE,false),8);
        Button scan=button("Проверить аудиосистему",0xff263e5d);add(tools,scan,12);
        scan.setOnClickListener(v->diagnose());
        Button reset=button("Сбросить настройки  ·  0 dB",0xff263e5d);add(tools,reset,10);
        reset.setOnClickListener(v->reset());

        LinearLayout eq=card();
        add(eq,text("ЭКВАЛАЙЗЕР",12,MUTED,true),0);
        add(eq,text("Полосы появятся после включения",13,MUTED,false),6);
        bands=column();add(eq,bands,12);

        TextView note=text("Работает только с аудиоисточниками, к которым Android предоставляет доступ. Радио, Bluetooth и сигналы безопасности могут обходить обработку. Проверяйте звук на стоянке.",12,MUTED,false);
        note.setLineSpacing(dp(3),1);add(root,note,20);
    }
    private void diagnose(){
        try{
            AudioEffect.Descriptor[] all=AudioEffect.queryEffects();
            StringBuilder s=new StringBuilder("Доступно эффектов: ").append(all==null?0:all.length);
            if(all!=null)for(AudioEffect.Descriptor item:all)if(item!=null)s.append("\n• ").append(item.name);
            s.append("\nГлобальная обработка не гарантируется.");
            status.setText(s.toString());
        }catch(RuntimeException | LinkageError e){
            status.setText("Диагностика недоступна: "+e.getClass().getSimpleName());
        }
    }
    private void start(){
        stop();
        try{
            equalizer=new Equalizer(0,0);
            if(!equalizer.hasControl() || equalizer.setEnabled(true)!=AudioEffect.SUCCESS)
                throw new IllegalStateException("Нет доступа к эквалайзеру");
            short count=equalizer.getNumberOfBands();
            short[] range=equalizer.getBandLevelRange();
            if(count<=0||count>32||range==null||range.length<2||range[1]<=range[0])
                throw new IllegalStateException("Некорректные параметры драйвера");
            for(short i=0;i<count;i++){
                final short band=i;
                int hz=equalizer.getCenterFreq(i)/1000;
                LinearLayout line=column();
                TextView freq=text(hz>=1000?String.format(Locale.US,"%.1f kHz",hz/1000f):hz+" Hz",13,WHITE,true);
                add(line,freq,12);
                SeekBar bar=new SeekBar(this);
                bar.setMax(range[1]-range[0]);
                bar.setProgress(Math.max(0,Math.min(bar.getMax(),equalizer.getBandLevel(i)-range[0])));
                if(Build.VERSION.SDK_INT>=21)bar.setProgressTintList(android.content.res.ColorStateList.valueOf(BLUE));
                bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                    public void onProgressChanged(SeekBar b,int val,boolean user){
                        if(user&&equalizer!=null)try{equalizer.setBandLevel(band,(short)(val+range[0]));}catch(RuntimeException ignored){}
                    }
                    public void onStartTrackingTouch(SeekBar b){}
                    public void onStopTrackingTouch(SeekBar b){}
                });
                add(line,bar,3);add(bands,line,0);
            }
            enabled=true;mode.setText("ВКЛ");mode.setTextColor(BLUE);
            toggle.setText("Выключить эквалайзер");
            status.setText("Android-эффект включён. Проверяйте звучание.");
        }catch(RuntimeException | LinkageError e){
            stop();status.setText("Недоступно: "+e.getClass().getSimpleName());
        }
    }
    private void reset(){
        if(!enabled||equalizer==null){status.setText("Сначала включите эквалайзер");return;}
        try{
            short[] range=equalizer.getBandLevelRange();
            short neutral=(short)Math.max(range[0],Math.min(0,range[1]));
            short count=equalizer.getNumberOfBands();
            for(short i=0;i<count;i++){
                equalizer.setBandLevel(i,neutral);
                LinearLayout line=(LinearLayout)bands.getChildAt(i);
                SeekBar bar=(SeekBar)line.getChildAt(1);
                bar.setProgress(neutral-range[0]);
            }
            status.setText("Настройки сброшены до 0 dB");
        }catch(RuntimeException e){status.setText("Ошибка сброса");}
    }
    private void stop(){
        enabled=false;
        if(equalizer!=null){
            try{equalizer.setEnabled(false);}catch(RuntimeException ignored){}
            try{equalizer.release();}catch(RuntimeException ignored){}
            equalizer=null;
        }
        if(bands!=null)bands.removeAllViews();
        if(toggle!=null)toggle.setText("Включить эквалайзер");
        if(mode!=null){mode.setText("ВЫКЛ");mode.setTextColor(MUTED);}
        if(status!=null)status.setText("Эффекты отключены");
    }
    @Override protected void onDestroy(){stop();super.onDestroy();}
}
