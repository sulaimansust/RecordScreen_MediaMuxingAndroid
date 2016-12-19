package com.example.ipvision.testproject;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;
import android.support.v4.app.Fragment;


/**
 * Created by ip vision on 12/17/2016.
 */

public class VideoPlaybackFragment extends Fragment {

    VideoView videoView;
    View view;
    Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d("Fragment", "Created................");

        if (view == null) {

            view = inflater.inflate(R.layout.video_layout, container, false);
            videoView = (VideoView) view.findViewById(R.id.videoView);
            MediaController mediaController = new MediaController(context);
            mediaController.setAnchorView(videoView);
            Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/video.mp4");
            videoView.setMediaController(mediaController);
            videoView.setVideoURI(uri);
            videoView.requestFocus();
            videoView.start();
        }
        return view;
    }
}
