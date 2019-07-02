package com.gz.tang.smartqa;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.gz.tang.smartqa.utils.JsonParser;
import com.gz.tang.smartqa.utils.TTSUtils;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class MainActivity extends WearableActivity {

    private TextView mTextView;
    private ImageView pngImageView;
    private ImageView gifImageView;
    private ScrollView scrollView;

    private final static String TAG = MainActivity.class.getSimpleName();
    // 语音听写对象
    private SpeechRecognizer mRecognize;
    // 语音听写UI
//    private RecognizerDialog mRecognizeDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mRecognizeResults = new LinkedHashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 定义获取录音的动态权限
        soundPermissions();

        // 初始化识别无UI识别对象，使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mRecognize = SpeechRecognizer.createRecognizer(this, mInitListener);
        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请将assets下文件拷贝到项目中
//        mRecognizeDialog = new RecognizerDialog(this, mInitListener);

        TTSUtils.getInstance().init();

        mTextView = (TextView) findViewById(R.id.main_text_view);
        pngImageView = (ImageView) findViewById(R.id.mic_png_id);
        gifImageView = (ImageView) findViewById(R.id.mic_gif_id);
        scrollView = (ScrollView) findViewById(R.id.sv_show);

//        scroll2Bottom(scrollView, mTextView);

        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        Glide.with(this).load(R.drawable.mic2).into(gifImageView);

        pngImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 开始听写。如何判断一次听写结束：OnResult isLast=true 或者 onError
                gifImageView.setVisibility(View.VISIBLE);
                pngImageView.setVisibility(View.INVISIBLE);

                mRecognizeResults.clear();

                int ret = 0; // 函数调用返回值

                // 设置参数
                resetParam();
                ret = mRecognize.startListening(mRecognizeListener);
                if (ret != ErrorCode.SUCCESS) {
                    showTip("听写失败,错误码：" + ret);
                } else {
//                    showTip("请开始说话…");
                }

//                TTSUtils.getInstance().speak("您刚刚说的是：我很喜欢他！" );
            }
        });

        gifImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gifImageView.setVisibility(View.INVISIBLE);
                pngImageView.setVisibility(View.VISIBLE);


                mRecognize.stopListening();
//                showTip("停止听写");
//                mTextView.setText("您刚刚说的是：\n \n请点击说话！");
            }
        });


        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出时释放连接
        mRecognize.cancel();
        mRecognize.destroy();
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

    //参数设置
    public void resetParam() {
        // 清空参数
        mRecognize.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎。TYPE_LOCAL表示本地，TYPE_CLOUD表示云端，TYPE_MIX 表示混合
        mRecognize.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式
        mRecognize.setParameter(SpeechConstant.RESULT_TYPE, "json");

//        String lag = mSharedPreferences.getString("recognize_language_preference", "mandarin");
//        if (lag.equals("en_us")) {  // 设置语言
//            mRecognize.setParameter(SpeechConstant.LANGUAGE, "en_us");
//        } else {
//            mRecognize.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
//            // 设置语言区域
//            mRecognize.setParameter(SpeechConstant.ACCENT, lag);
//        }
        mRecognize.setParameter(SpeechConstant.LANGUAGE, "zh_cn");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mRecognize.setParameter(SpeechConstant.VAD_BOS, "2000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mRecognize.setParameter(SpeechConstant.VAD_EOS, "1000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
//        mRecognize.setParameter(SpeechConstant.ASR_PTT, "0");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mRecognize.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mRecognize.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/recognize.wav");
    }

    // 定义录音的动态权限
    private void soundPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    //初始化监听器
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
            }
        }
    };

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        String sn = null;
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        mRecognizeResults.put(sn, text);
        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mRecognizeResults.keySet()) {
            resultBuffer.append(mRecognizeResults.get(key));
        }
//        mTextView.setText(resultBuffer.toString());
        Log.d(TAG, "print result:" + resultBuffer.toString());
        if(resultBuffer.length() > 0 && mRecognizeResults.size() == 1) {
            mRecognize.stopListening();
            mTextView.append("\n" + resultBuffer.toString());
            scroll2Bottom(scrollView, mTextView);
            TTSUtils.getInstance().speak("您刚刚说的是：" + resultBuffer.toString());
        }
    }

    //听写UI监听器
//    private RecognizerDialogListener mRecognizeDialogListener = new RecognizerDialogListener() {
//        public void onResult(RecognizerResult results, boolean isLast) {
//            printResult(results);
//        }
//
//        //识别回调错误
//        public void onError(SpeechError error) {
//            showTip(error.getPlainDescription(true));
//        }
//    };

    //听写监听器
    private RecognizerListener mRecognizeListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
//            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
            showTip(error.getPlainDescription(true));
            gifImageView.setVisibility(View.INVISIBLE);
            pngImageView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
//            mRecognize.stopListening();
            gifImageView.setVisibility(View.INVISIBLE);
            pngImageView.setVisibility(View.VISIBLE);
//            showTip("结束说话");

            StringBuffer resultBuffer = new StringBuffer();
            for (String key : mRecognizeResults.keySet()) {
                resultBuffer.append(mRecognizeResults.get(key));
            }
            mRecognizeResults.clear();
            if(resultBuffer.length() > 0) {
                Log.d(TAG, "在End里面说话：" + resultBuffer.toString());
//                TTSUtils.getInstance().speak("您刚刚说的是：" + resultBuffer.toString());
            }
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            printResult(results);
//            mRecognize.startListening(mRecognizeListener);
            Log.i(TAG,"是不是Last:" + isLast);
            if (isLast) {
                // TODO 最后的结果
                StringBuffer resultBuffer = new StringBuffer();
                for (String key : mRecognizeResults.keySet()) {
                    resultBuffer.append(mRecognizeResults.get(key));
                }
                Log.d(TAG, "在result里面说：" + resultBuffer.toString());
//                mRecognize.stopListening();
//                gifImageView.setVisibility(View.INVISIBLE);
//                pngImageView.setVisibility(View.VISIBLE);
//                TTSUtils.getInstance().speak("您刚刚说的是：" + resultBuffer.toString() );
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
//            showTip("当前正在说话，音量大小：" + volume);
//            Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private void showTip(final String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }
}
