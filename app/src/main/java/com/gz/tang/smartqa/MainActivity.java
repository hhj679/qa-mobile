package com.gz.tang.smartqa;

import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class MainActivity extends WearableActivity {

    private TextView mTextView;
    private ImageView pngImageView;
    private ImageView gifImageView;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.main_text_view);
        pngImageView = (ImageView) findViewById(R.id.mic_png_id);
        gifImageView = (ImageView) findViewById(R.id.mic_gif_id);
        scrollView = (ScrollView) findViewById(R.id.sv_show);

        scroll2Bottom(scrollView, mTextView);

        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        Glide.with(this).load(R.drawable.mic2).into(gifImageView);

        pngImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gifImageView.setVisibility(View.VISIBLE);
                pngImageView.setVisibility(View.INVISIBLE);

                mTextView.setText("请对着麦克风说话！");
            }
        });

        gifImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gifImageView.setVisibility(View.INVISIBLE);
                pngImageView.setVisibility(View.VISIBLE);

                mTextView.setText("您刚刚说的是：\n \n请点击说话！");
            }
        });

        // Enables Always-on
        setAmbientEnabled();
    }


    public static void scroll2Bottom(final ScrollView scroll, final View inner) {
        Handler handler = new Handler();
        handler.post(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                if (scroll == null || inner == null) {
                    return;
                }
                // 内层高度超过外层
                int offset = inner.getMeasuredHeight()
                        - scroll.getMeasuredHeight();
                if (offset < 0) {
                    offset = 0;
                }
                scroll.scrollTo(0, offset);
            }
        });
    }
}
