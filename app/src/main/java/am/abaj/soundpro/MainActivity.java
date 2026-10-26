package am.abaj.soundpro;

import android.app.Activity;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import java.util.Locale;

public final class MainActivity extends Activity {
    private LinearLayout layout, bands;
    private TextView status;
    private Equalizer equalizer;
    private Button toggle;
    private boolean enabled;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24,24,24,24);
        layout.setBackgroundColor(Color.rgb(12,22,36));
        scroll.addView(layout);
        setContentView(scroll);
        label("ABAJ SOUND PRO", 26);
        label("Диагностика Android аудио · v0.1.4", 16);
        label(Build.MANUFACTURER + " / " + Build.MODEL + " · Android " + Build.VERSION.RELEASE, 16);
        status=label("Эффекты выключены",16);
        Button check=button("Проверить аудиосистему");
        check.setOnClickListener(v -> diagnose());
        toggle=button("Включить пробный эквалайзер");
        toggle.setOnClickListener(v -> { if(enabled) stop(); else start(); });
        Button reset=button("Сбросить в 0 dB");
        reset.setOnClickListener(v -> reset());
        bands=new LinearLayout(this);
        bands.setOrientation(LinearLayout.VERTICAL);
        layout.addView(bands);
        label("Включение Android-эффекта не гарантирует обработку радио, Bluetooth и системных предупреждений. Проверять только на стоянке.", 14);
    }
    private TextView label(String text,int size) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size);
        view.setTextColor(Color.rgb(235,244,255));
        view.setPadding(0,10,0,10);
        layout.addView(view);
        return view;
    }
    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text); b.setAllCaps(false);
        layout.addView(b); return b;
    }
    private void diagnose() {
        try {
            AudioEffect.Descriptor[] all=AudioEffect.queryEffects();
            StringBuilder s=new StringBuilder("Android API ").append(Build.VERSION.SDK_INT);
            s.append("\nНайдено эффектов: ").append(all==null?0:all.length);
            if(all!=null) for(AudioEffect.Descriptor item:all)
                if(item!=null) s.append("\n").append(item.name);
            s.append("\nНаличие эффекта не подтверждает глобальную обработку.");
            status.setText(s.toString());
        } catch(RuntimeException | LinkageError e) {
            status.setText("Диагностика недоступна: "+e.getClass().getSimpleName());
        }
    }
    private void start() {
        stop();
        try {
            equalizer=new Equalizer(0,0);
            if(!equalizer.hasControl() || equalizer.setEnabled(true)!=AudioEffect.SUCCESS)
                throw new IllegalStateException("Нет доступа к эквалайзеру");
            short count=equalizer.getNumberOfBands();
            short[] range=equalizer.getBandLevelRange();
            if(count<=0 || count>32 || range==null || range.length<2 || range[1]<=range[0])
                throw new IllegalStateException("Некорректные параметры драйвера");
            for(short i=0;i<count;i++) {
                final short band=i;
                int hz=equalizer.getCenterFreq(i)/1000;
                TextView title=new TextView(this);
                title.setText(String.format(Locale.US,"%d Hz",hz));
                title.setTextColor(Color.WHITE);
                bands.addView(title);
                SeekBar slider=new SeekBar(this);
                slider.setMax(range[1]-range[0]);
                slider.setProgress(Math.max(0,Math.min(slider.getMax(),equalizer.getBandLevel(i)-range[0])));
                slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                    @Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser) {
                        if(fromUser && equalizer!=null) try {
                            equalizer.setBandLevel(band,(short)(value+range[0]));
                        } catch(RuntimeException ignored) {}
                    }
                    @Override public void onStartTrackingTouch(SeekBar bar) {}
                    @Override public void onStopTrackingTouch(SeekBar bar) {}
                });
                bands.addView(slider);
            }
            enabled=true;
            toggle.setText("Выключить эффекты");
            status.setText("Эквалайзер включён в Android. Влияние на источники не подтверждено.");
        } catch(RuntimeException | LinkageError e) {
            stop();
            status.setText("Эффект недоступен: "+e.getClass().getSimpleName());
        }
    }
    private void reset() {
        if(!enabled || equalizer==null) return;
        try {
            short[] range=equalizer.getBandLevelRange();
            short neutral=(short)Math.max(range[0],Math.min(0,range[1]));
            short count=equalizer.getNumberOfBands();
            for(short i=0;i<count;i++) {
                equalizer.setBandLevel(i,neutral);
                SeekBar slider=(SeekBar)bands.getChildAt(i*2+1);
                slider.setProgress(neutral-range[0]);
            }
            status.setText("Полосы сброшены в 0 dB.");
        } catch(RuntimeException e) { status.setText("Ошибка сброса"); }
    }
    private void stop() {
        enabled=false;
        if(equalizer!=null) {
            try { equalizer.setEnabled(false); } catch(RuntimeException ignored) {}
            try { equalizer.release(); } catch(RuntimeException ignored) {}
            equalizer=null;
        }
        if(bands!=null) bands.removeAllViews();
        if(toggle!=null) toggle.setText("Включить пробный эквалайзер");
        if(status!=null) status.setText("Эффекты выключены");
    }
    @Override protected void onDestroy() { stop(); super.onDestroy(); }
}
