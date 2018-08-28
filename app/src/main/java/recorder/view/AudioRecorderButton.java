package recorder.view;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.mwh.wechatrecorder.R;

/**
 * Created by KingFish on 2017/12/19.
 */

public class AudioRecorderButton extends android.support.v7.widget.AppCompatButton implements AudioManager.AudioStateListener {

    private static final int STATE_NORMAL = 1; //正常状态
    private static final int STATE_RECORDING = 2; //录音状态
    private static final int STATE_WANT_TO_CANCEL = 3; //准备取消录音状态

    private static final int DISTANCE_Y_CANCEL = 50;

    private int mCurState = STATE_NORMAL; //当前状态

    private boolean isRecording = false; //已经开始录音

    private  DialogManager mDialogManager;

    private  AudioManager mAudioManager;


    private float mTime; //录音时间

    private boolean mReady;//触发longclik

    public AudioRecorderButton (Context context) {
        this(context, null);
    }
    public AudioRecorderButton (Context context, AttributeSet attrs) {
        super(context, attrs);

        mDialogManager = new DialogManager(getContext());
        //完善
        String dir = Environment.getExternalStorageDirectory()+"/recorder_audios";
        mAudioManager = AudioManager.getInstance(dir);
        mAudioManager.setOnAudioStateListener(this);

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            //长按开始录音
            public boolean onLongClick(View view) {
                mReady = true;
                mAudioManager.prepareAudio(); //调用wellPrepared()
                return false;
            }
        });
    }
    @Override
    public void wellPrepared() {
        mHandler.sendEmptyMessage(MSG_AUDIO_PREPARED);
    }

    /**
     * 录音完成后的回调接口
     */
    public interface AudioFinishRecorderListener {
        void onFinish(float second, String filePath);
    }

    private AudioFinishRecorderListener mListener;

    public void setAudioFinishRecorderListener(AudioFinishRecorderListener listener) {
        mListener = listener;
    }

    /**
     * 获取音量大小的Runnable
     */
    private Runnable mGetVoiceLevelRunnable = new Runnable() {
        @Override
        public void run() {
            while(isRecording) {
                try {
                    Thread.sleep(100);
                    mTime += 0.1f;
                    mHandler.sendEmptyMessage(MSG_VOICE_CHANGED);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private static final int MSG_AUDIO_PREPARED = 0X110; //录音准备完毕，开始录音
    private static final int MSG_VOICE_CHANGED = 0X111; //音量改变
    private static final int MSG_DIALOG_DISMISS = 0X112; //dismiss掉dialog

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_AUDIO_PREPARED:
                    //TODO 真正显示在audio end prepare之后
                    mDialogManager.showRecordingDialog();
                    isRecording = true;
                    new Thread(mGetVoiceLevelRunnable).start();
                    break;
                case MSG_VOICE_CHANGED:
                    mDialogManager.updateVoiceLevel(mAudioManager.getVoiceLevel(7));
                    break;
                case MSG_DIALOG_DISMISS:
                    mDialogManager.dismissDialog();
                    break;
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //按下按钮，改变状态为录音状态
                changeState(STATE_RECORDING);
                break;

            case MotionEvent.ACTION_MOVE:
                //移动
                if (isRecording) {
                    //根据x,y的坐标，判断是否取消
                    if (wantToCancel(x,y))
                        changeState(STATE_WANT_TO_CANCEL);
                    else
                        changeState(STATE_RECORDING);
                }
                break;

            case MotionEvent.ACTION_UP:
                //抬起
                if (!mReady) {
                    //没有触发longclick
                    reset();
                    return super.onTouchEvent(event);
                }
                if (!isRecording || mTime < 0.6f) {
                    //prepare没有完成或者录音时间太短
                    mDialogManager.tooShort();
                    mAudioManager.cancel();
                    //延迟显示1.3秒
                    mHandler.sendEmptyMessageDelayed(MSG_DIALOG_DISMISS, 1300);
                } else if (mCurState == STATE_RECORDING) {
                    //正常录制结束
                    mDialogManager.dismissDialog();
                    mAudioManager.release();

                    if (mListener != null) {
                        mListener.onFinish(mTime, mAudioManager.getmCurrentFilePath());
                    }
                    // callbackToAct
                }
                else if (mCurState == STATE_WANT_TO_CANCEL) {
                    mDialogManager.dismissDialog();
                    //cancel
                }
                reset();
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 恢复状态及标志位
     */
    private void reset() {
        isRecording = false;
        mReady = false;
        mTime = 0;
        changeState(STATE_NORMAL);
    }

    private boolean wantToCancel(int x, int y) {
        if (x < 0 || x > getWidth())
            return  true;
        if (y < - DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL)
            return  true;
        return  false;
    }

    private void changeState(int state) {
        //改变dialog文本、背景色
        if (mCurState != state) {
            mCurState = state;
            switch (state) {
                case STATE_NORMAL:
                    setBackgroundResource(R.drawable.btn_recorder_normal);
                    setText(R.string.str_recorder_normal);
                    break;

                case STATE_RECORDING:
                    setBackgroundResource(R.drawable.btn_recording);
                    setText(R.string.str_recorder_recording);
                    if (isRecording) {
                        mDialogManager.recording();
                    }
                    break;

                case STATE_WANT_TO_CANCEL:
                    setBackgroundResource(R.drawable.btn_recording);
                    setText(R.string.str_recorder_want_cancle);
                    mDialogManager.wantToCancel();
                    break;
            }
        }
    }

}
