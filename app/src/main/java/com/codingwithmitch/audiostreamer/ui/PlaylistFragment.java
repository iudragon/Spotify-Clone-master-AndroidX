package com.codingwithmitch.audiostreamer.ui;


import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.support.v4.media.MediaMetadataCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.codingwithmitch.audiostreamer.R;
import com.codingwithmitch.audiostreamer.adapters.PlaylistRecyclerAdapter;
import com.codingwithmitch.audiostreamer.models.Artist;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;

import static android.content.Context.DOWNLOAD_SERVICE;


public class PlaylistFragment extends Fragment implements
        PlaylistRecyclerAdapter.IMediaSelector {

    private static final String TAG = "PlaylistFragment";

    // UI Components
    private RecyclerView mRecyclerView;


    /* MUSIC DOWNLOAD */

    long queueId;
    DownloadManager dm;
    MediaPlayer mediaPlayer;


    /* MUSIC DOWNLOAD */


    // Vars
    private PlaylistRecyclerAdapter mAdapter;
    private ArrayList<MediaMetadataCompat> mMediaList = new ArrayList<>();
    private IMainActivity mIMainActivity;
    private String mSelectedCategory;
    private Artist mSelectArtist;
    private MediaMetadataCompat mSelectedMedia;

    Button downloadClick;
    Button viewClick;

    /* SWIPE REFRESH LAYOUT */

    private SwipeRefreshLayout mRefreshLayout;
    private int refresh_count = 0;

    public static PlaylistFragment newInstance(String category, Artist artist) {
        PlaylistFragment playlistFragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putString("category", category);
        args.putParcelable("artist", artist);
        playlistFragment.setArguments(args);
        return playlistFragment;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            mIMainActivity.setActionBarTitle(mSelectArtist.getTitle());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSelectedCategory = getArguments().getString("category");
            mSelectArtist = getArguments().getParcelable("artist");
        }
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initRecyclerView(view);
        mIMainActivity.setActionBarTitle(mSelectArtist.getTitle());

        if (savedInstanceState != null) {
            mAdapter.setSelectedIndex(savedInstanceState.getInt("selected_index"));
        }

        downloadClick = view.findViewById(R.id.downloadClick);
        viewClick = view.findViewById(R.id.viewClick);

        downloadClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                dm = (DownloadManager) getActivity().getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse("http://feeds.soundcloud.com/stream/550150548-iu-dragon-summer-upbeat.mp3"));

                queueId = dm.enqueue(request);
            }
        });

        viewClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent();
                i.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
                startActivity(i);

            }
        });

        /* SWIPE REFRESH LAYOUT */

        mRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                mMediaList.clear();

                retrieveMedia();

                mRefreshLayout.setRefreshing(false);

            }
        });
    }

    private void getSelectedMediaItem(String mediaId) {
        for (MediaMetadataCompat mediaItem : mMediaList) {
            if (mediaItem.getDescription().getMediaId().equals(mediaId)) {
                mSelectedMedia = mediaItem;
                mAdapter.setSelectedIndex(mAdapter.getIndexOfItem(mSelectedMedia));

                break;
            }
        }
    }

    private void retrieveMedia() {
        mIMainActivity.showPrgressBar();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Query query = firestore
                .collection(getString(R.string.collection_audio))
                .document(getString(R.string.document_categories))
                .collection(mSelectedCategory)
                .document(mSelectArtist.getArtist_id())
                .collection(getString(R.string.collection_content))
                .orderBy(getString(R.string.field_date_added), Query.Direction.ASCENDING);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        addToMediaList(document);
                    }
                } else {
                    Log.d(TAG, "onComplete: error getting documents: " + task.getException());
                }
                updateDataSet();
            }
        });
    }

    private void addToMediaList(QueryDocumentSnapshot document) {
        MediaMetadataCompat media = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, document.getString(getString(R.string.field_media_id)))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, document.getString(getString(R.string.field_artist)))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, document.getString(getString(R.string.field_title)))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, document.getString(getString(R.string.field_media_url)))
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, document.getString(getString(R.string.field_description)))
                .putString(MediaMetadataCompat.METADATA_KEY_DATE, document.getDate(getString(R.string.field_date_added)).toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, mSelectArtist.getImage())
                .build();


        mMediaList.add(media);
    }

    private void updateDataSet() {
        mIMainActivity.hideProgressBar();
        mAdapter.notifyDataSetChanged();
        if (mIMainActivity.getMyPreferenceManager().getLastPlayedArtist().equals(mSelectArtist.getArtist_id())) {
            getSelectedMediaItem(mIMainActivity.getMyPreferenceManager().getLastPlayedMedia());
        }
    }

    private void initRecyclerView(View view) {
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new PlaylistRecyclerAdapter(getActivity(), mMediaList, this);
        mRecyclerView.setAdapter(mAdapter);


        if (mMediaList.size() == 0) {
            retrieveMedia();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIMainActivity = (IMainActivity) getActivity();
    }

    @Override
    public void onMediaSelected(int position) {
        mIMainActivity.getMyApplicationInstance().setMediaItems(mMediaList);
        mSelectedMedia = mMediaList.get(position);

        mAdapter.setSelectedIndex(position);
        mIMainActivity.onMediaSelected(
                mSelectArtist.getArtist_id(), // playlist_id = artist_id
                mMediaList.get(position),
                position);
        Log.d(TAG, "onMediaSelected: MEX " + mSelectedMedia.getDescription().getMediaUri());
        saveLastPlayedSongProperties();
    }

    public void updateUI(MediaMetadataCompat mediaItem) {
        mAdapter.setSelectedIndex(mAdapter.getIndexOfItem(mediaItem));
        mSelectedMedia = mediaItem;
        Log.d(TAG, "onMediaSelected: MEXTHIS " + mSelectedMedia.getDescription().getMediaUri());
        saveLastPlayedSongProperties();
    }

    private void saveLastPlayedSongProperties() {
        // Save some properties for next time the app opens
        // NOTE: Normally you'd do this with a cache
        mIMainActivity.getMyPreferenceManager().savePlaylistId(mSelectArtist.getArtist_id()); // playlist id is same as artist id
        mIMainActivity.getMyPreferenceManager().saveLastPlayedArtist(mSelectArtist.getArtist_id());
        mIMainActivity.getMyPreferenceManager().saveLastPlayedCategory(mSelectedCategory);
        mIMainActivity.getMyPreferenceManager().saveLastPlayedArtistImage(mSelectArtist.getImage());
        mIMainActivity.getMyPreferenceManager().saveLastPlayedMedia(mSelectedMedia.getDescription().getMediaId());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_index", mAdapter.getSelectedIndex());
    }

    @Override
    public void onResume() {
        super.onResume();

        /* MUSIC */
// your URL here
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource("http://feeds.soundcloud.com/stream/550150548-iu-dragon-summer-upbeat.mp3");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mediaPlayer.prepare(); // might take long! (for buffering, etc)
        } catch (IOException e) {
            e.printStackTrace();
        }


        /* MUSIC */

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {

                    DownloadManager.Query req_query = new DownloadManager.Query();
                    req_query.setFilterById(queueId);

                    Cursor c = dm.query(req_query);
                    if (c.moveToFirst()) {

                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)){
                            Toast.makeText(context, "SUCCESS", Toast.LENGTH_SHORT).show();

                        }

                    }


                }

            }
        };

        getActivity().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
}
