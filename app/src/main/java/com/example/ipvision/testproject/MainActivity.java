package com.example.ipvision.testproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.nfc.Tag;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    //    private ProgressBar liveRecordingProgressBar;
//    private Button liveRecordingBtn;
    private String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static int DISPLAY_WIDTH;
    private static int DISPLAY_HEIGHT;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSIONS = 10;
    private int progressBarStatus;
    private CountDownTimer mCountDownTimer;
    private boolean isProgressBarFinished;
    private HoldToLoadLayout holdToLoadLayout;


    //audiorecord

    protected RecordScreen recordScreen = new RecordScreen();
    AudioManager iAudioManager;

    public static final int SAMPLE_RATE = 8000;
    private AudioRecord mRecorder;
    protected File rawFile;
    protected File waveFile;
    protected File audioFile;
    protected File videoFile;
    protected File outputFile;
    private short[] mBuffer;
    private boolean mIsRecording = false;

    //end audiorecord

    static {

        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        iAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiInit();



    }

    private void initiateHoldToLayout() {

        if (holdToLoadLayout != null) {
            holdToLoadLayout.setStrokeWidth(10);
            holdToLoadLayout.setStrokeAlpha(255);
            holdToLoadLayout.setPlayReverseAnimation(false);
            holdToLoadLayout.setStopWhenFilled(false);
            holdToLoadLayout.setColorAnimator(Color.YELLOW, Color.RED);
            holdToLoadLayout.setStartAngle(HoldToLoadLayout.DEFAULT_START_ANGLE);
            holdToLoadLayout.setDuration(10500);
            holdToLoadLayout.setMainActivity(MainActivity.this);
            holdToLoadLayout.setFillListener(new HoldToLoadLayout.FillListener() {

                @Override
                public void onFull() {
                    Log.d(TAG, "onFull");
                    destroyMedia();
                    addPreviewFragment();
                }

                @Override
                public void onEmpty() {
                    Log.d(TAG, "onEmpty");
                }

                @Override
                public void onAngleChanged(float angle) {
                    Log.d("test_log", "onAngleChange");
                }
            });

        }


    }

    private void uiInit() {

        holdToLoadLayout = (HoldToLoadLayout) findViewById(R.id.holdToLoadLayout);
        initiateHoldToLayout();


        initAudioRecorder();

        final


        //display metrices
                DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        //end display metrice

        //current display size
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        DISPLAY_WIDTH = size.x;
        DISPLAY_HEIGHT = size.y;
        //end current display size







    }

    protected void destroyMedia(){
        Log.d("mymethod","destroyMedia()");

        destroyMediaProjection();

        waveFile = recordScreen.getFile("wav");
        audioFile = recordScreen.getFile("mp4");

        try {
            recordScreen.rawToWave(rawFile, waveFile);

        } catch (IOException e) {

            Toast.makeText(MainActivity.this, e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(MainActivity.this,
                "Recorded to " + waveFile.getName(),
                Toast.LENGTH_SHORT).show();

        // destroyCountDowntimer();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                recordScreen.wavToMP4(waveFile, audioFile);
            }
        });
        t.start();

        try {

            t.join();
            if (!t.isAlive()) {
                Toast toast = Toast.makeText(MainActivity.this, "Processing Completed", Toast.LENGTH_SHORT);
                toast.show();

                final File outputFile = recordScreen.getFile("mp4");

                new Thread(new Runnable() {
                    @Override
                    public void run() {


                        final File outputFile = recordScreen.getFile("mp4");
                        //muxAudioAndVideo(videoFile.toString(), audioFile.toString(), outputFile.toString());
                        muxing(videoFile.toString(), audioFile.toString(), outputFile.toString());

                    }
                }).start();

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String AUDIOID = "AUDIOID";


    private void muxing(String audio, String video, String output) {

        String outputFile = "";

        try {

            File file = new File(output);
            file.createNewFile();
            outputFile = file.getAbsolutePath();

            MediaExtractor videoExtractor = new MediaExtractor();
            // final AssetFileDescriptor afdd = getAssets().openFd("sample2.mp4");
            videoExtractor.setDataSource(video);

            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audio);

            Log.d(TAG, "Video Extractor Track Count " + videoExtractor.getTrackCount());
            Log.d(TAG, "Audio Extractor Track Count " + audioExtractor.getTrackCount());

            MediaMuxer muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            videoExtractor.selectTrack(0);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(0);
            int videoTrack = muxer.addTrack(videoFormat);

            audioExtractor.selectTrack(0);
            MediaFormat audioFormat = audioExtractor.getTrackFormat(0);
            int audioTrack = muxer.addTrack(audioFormat);

            Log.d(TAG, "Video Format " + videoFormat.toString());
            Log.d(TAG, "Audio Format " + audioFormat.toString());

            boolean sawEOS = false;
            int frameCount = 0;
            int offset = 100;
            int sampleSize = 256 * 1024;
            ByteBuffer videoBuf = ByteBuffer.allocate(sampleSize);
            ByteBuffer audioBuf = ByteBuffer.allocate(sampleSize);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();


            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            muxer.start();

            while (!sawEOS) {
                videoBufferInfo.offset = offset;
                audioBufferInfo.offset = offset;

                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset);
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset);

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    Log.d(TAG, "saw input EOS.");
                    sawEOS = true;
                    videoBufferInfo.size = 0;
                    audioBufferInfo.size = 0;
                } else {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                    videoBufferInfo.flags = videoExtractor.getSampleFlags();
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo);
                    videoExtractor.advance();

                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                    audioBufferInfo.flags = audioExtractor.getSampleFlags();
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo);
                    audioExtractor.advance();

                    frameCount++;

                    Log.d(TAG, "Frame (" + frameCount + ") Video PresentationTimeUs:" + videoBufferInfo.presentationTimeUs + " Flags:" + videoBufferInfo.flags + " Size(KB) " + videoBufferInfo.size / 1024);
                    Log.d(TAG, "Frame (" + frameCount + ") Audio PresentationTimeUs:" + audioBufferInfo.presentationTimeUs + " Flags:" + audioBufferInfo.flags + " Size(KB) " + audioBufferInfo.size / 1024);

                }
            }

            muxer.stop();
            muxer.release();


        } catch (IOException e) {
            Log.d(TAG, "Mixer Error 1 " + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Mixer Error 2 " + e.getMessage());
        }

    }

    public void save(Movie movie, String output) {
        Log.e(TAG, output);
        Container mp4file = new DefaultMp4Builder().build(movie);
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/output.mp4");

            FileChannel fc = new FileOutputStream(file).getChannel();

            mp4file.writeContainer(fc);

            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    final int FRAM_SIZE_FACTOR = 160;

    private void initAudioRecorder() {

        int frame_size = 5 * FRAM_SIZE_FACTOR;
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[frame_size];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }


//    private void configureProgressBar(View v) {
//
//
//        liveRecordingProgressBar = (ProgressBar) findViewById(R.id.liveRecordingProgressBar);
//        liveRecordingProgressBar.setMax(300);
//        liveRecordingProgressBar.setScaleY(0.3f);
//        liveRecordingProgressBar.setProgress(progressBarStatus);
//        liveRecordingProgressBar.setVisibility(View.VISIBLE);
//
//        mCountDownTimer = new CountDownTimer(30000, 100) {
//
//            @Override
//            public void onTick(long millisUntilFinished) {
//
//                // Log.v("Log_tag", "Tick of Progress " + progressBarStatus + " ---- " + millisUntilFinished);
//                progressBarStatus++;
//                liveRecordingProgressBar.setProgress(progressBarStatus);
//            }
//
//            @Override
//            public void onFinish() {
//                Log.d(TAG, "Finished Progressbar");
//                isProgressBarFinished = true;
//                progressBarStatus++;
//                liveRecordingProgressBar.setProgress(progressBarStatus);
//                destroyMediaProjection();
//                liveRecordingProgressBar.setVisibility(View.GONE);
//                addPreviewFragment();
//            }
//        };
//        mCountDownTimer.start();
//    }

    private void addPreviewFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new VideoPlaybackFragment()).commit();

    }


    private void destroyCountDowntimer() {

        if (null != mCountDownTimer) {
            mCountDownTimer.cancel();
        }

    }

    private boolean mediaRecorderStarted;
    ParcelFileDescriptor[] pipe;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        try {


            if (requestCode != REQUEST_CODE) {
                Log.e(TAG, "Unknown request code: " + requestCode);
                return;
            }
            if (resultCode != RESULT_OK) {
                Toast.makeText(this,
                        "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
                return;
            }

            iAudioManager.setSpeakerphoneOn(true);
            recordScreen.changeRecordingStatus(true);

            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            mVirtualDisplay = createVirtualDisplay();
            mMediaRecorder.start();
            mRecorder.startRecording();
            recordScreen.startBufferedWrite(rawFile, mRecorder, mBuffer);

            mediaRecorderStarted = true;

            startReceivinMediaData();

            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(MainActivity.this, "Recording Started", duration);
            Log.v(TAG, "Start Recording");
            toast.show();

        } catch (Exception e) {

        }
    }

    private void startReceivinMediaData() {

        if (mediaRecorderStarted) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Reading media data....." + pipe[0].getFileDescriptor());
                }
            }).start();
        }
    }


    protected void initMediaRecorder(File videoFile) {
        try {

            pipe = ParcelFileDescriptor.createPipe();


            mMediaRecorder = new MediaRecorder();
            mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoEncodingBitRate(1000 * 1000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setOutputFile(videoFile.toString());
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT - 300);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null /*Callbacks*/, null/*Handler*/);
    }


    @Override
    public void onDestroy() {

        super.onDestroy();
        destroyMediaProjection();
    }

    protected void destroyMediaProjection() {

        try {

            if (mVirtualDisplay != null) {

                mVirtualDisplay.release();
            }
            if (mMediaRecorder != null) {


                recordScreen.changeRecordingStatus(false);
                iAudioManager.setSpeakerphoneOn(false);
                mRecorder.stop();
                mMediaRecorder.reset();

            }
            if (mMediaProjection != null) {

                mMediaProjection.stop();

            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        Log.i(TAG, "MediaProjection Stopped");
    }
}
