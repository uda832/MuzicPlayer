package com.example.ud4.muzicplayer;

import android.net.Uri;
import android.content.ContentUris;

public class Song
{
    private long mId;
    private String mTitle;
    private String mArtist;
    private long mAlbumId;

    //Constructor
    public Song(long id, String title, String artist, long albumId)
    {
        mId = id;
        mTitle = title;
        mArtist = artist;
        mAlbumId = albumId;
    }
    
    //Methods
    public long getId()
    {
        return mId;
    }

    public String getTitle()
    {
        return mTitle;
    }

    public String getArtist()
    {
        return mArtist;
    }

    public long getAlbumId()
    {
        return mAlbumId;
    }

    public Uri getAlbumArtUri()
    {
        //Return URI of album art using album ID
        Uri uri = Uri.parse("content://media/external/audio/albumart");
        return ContentUris.withAppendedId(uri, mAlbumId);
    }
}//end-class
