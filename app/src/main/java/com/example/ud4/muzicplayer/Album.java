package com.example.ud4.muzicplayer;

import android.net.Uri;
import android.content.ContentUris;


public class Album implements TitleInterface
{
    public long mId;
    public String mTitle;
    public String mArtist;

    //Constructor
    public Album(long id, String title, String artist)
    {
        mId = id;
        mTitle = title;
        mArtist = artist;
    }
    
    //Methods
    public long getId()
    {
        return mId;
    }

    @Override
    public String getTitle()
    {
        return mTitle;
    }

    public String getArtist()
    {
        return mArtist;
    }

    public Uri getAlbumArtUri()
    {
        //Return URI of album art using album ID
        Uri uri = Uri.parse("content://media/external/audio/albumart");
        return ContentUris.withAppendedId(uri, mId);
    }
}//end-class
