package recorder.view;

import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by KingFish on 2017/12/20.
 */

public class AudioManager {
    private MediaRecorder mMediaRecorder;
    //文件路径
    private String mDir;
    private String mCurrentFilePath;

    public boolean isPrepared;

    /**
     * 单例生成
     */
    private static AudioManager mInstance;

    public static AudioManager getInstance(String dir) {
        if (mInstance == null) {
            synchronized (AudioManager.class) {
                if (mInstance == null)
                    mInstance = new AudioManager(dir);
            }
        }
        return mInstance;
    }

    private AudioManager(String dir){
        mDir = dir;
    }


    /**
     * 接口：回调准备完毕
     */
    public interface AudioStateListener {
        void wellPrepared();
    }

    public AudioStateListener mListener;

    public void setOnAudioStateListener(AudioStateListener Listener) {
        mListener = Listener;
    }


    public void prepareAudio() {
        //准备
        try {
            isPrepared = false;

            //生成语音文件路径
            File dir = new File(mDir);
            if (!dir.exists())
                dir.mkdirs();
            String fileName = generateFileName();
            File file = new File(dir, fileName);
            mCurrentFilePath = file.getAbsolutePath();

            mMediaRecorder = new MediaRecorder();
            //设置输出文件
            mMediaRecorder.setOutputFile(file.getAbsolutePath());
            //设置MediaRecorder的音频源——麦克风
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //设置音频格式
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            //设置音频编码为amr
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            mMediaRecorder.prepare();
            mMediaRecorder.start();
            //准备完毕
            isPrepared = true;
            if (mListener != null)
                mListener.wellPrepared();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 随机生成文件名称
     * @return
     */
    private String generateFileName() {
        return UUID.randomUUID().toString()+".amr";
    }


    public int getVoiceLevel(int maxLevel) {
        if (isPrepared) {
            try {
                //mMediaRecorder.getMaxAmplitude() 1-32767
                return maxLevel*mMediaRecorder.getMaxAmplitude() / 32768 + 1;
            } catch (Exception e) {
            }
        }
        return  1;
    }

    public void release() {
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
    }

    public void cancel() {
        release();
        if (mCurrentFilePath != null){
            File file = new File(mCurrentFilePath);
            file.delete();
            mCurrentFilePath = null;
        }
    }

    public String getmCurrentFilePath() {
        return  mCurrentFilePath;
    }
}
