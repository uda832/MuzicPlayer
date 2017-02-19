package com.example.ud4.muzicplayer;

import android.net.Uri;
import android.content.ContentUris;


public class Song implements TitleInterface
{
    private long mId;
    private String mTitle;
    private String mArtist;
    private long mAlbumId;
    private int mTrackNum;
    private long mDuration;

    //Constructor
    public Song(long id, String title, String artist, long albumId, int trackNum, long duration)
    {
        mId = id;
        mTitle = title;
        mArtist = artist;
        mAlbumId = albumId;
        mTrackNum = trackNum;
        mDuration = duration;
    }
    
    //Methods
    public long getId() { return mId; }

    @Override
    public String getTitle() { return mTitle; }

    public String getArtist() { return mArtist; }

    public long getAlbumId() { return mAlbumId; }

    public int getTrackNum() { return mTrackNum; }

    public long getDuration() { return mDuration; }

    public Uri getAlbumArtUri()
    {
        //Return URI of album art using album ID
        Uri uri = Uri.parse("content://media/external/audio/albumart");
        return ContentUris.withAppendedId(uri, mAlbumId);
    }
}//end-class
