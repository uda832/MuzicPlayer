package com.example.ud4.muzicplayer;


public class Song
{
    private long mId;
    private String mTitle;
    private String mArtist;
    private String mAlbum;

    //Constructor
    public Song(long id, String title, String artist, String album)
    {
        mId = id;
        mTitle = title;
        mArtist = artist;
        mAlbum = album;
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

    public String getAlbum()
    {
        return mAlbum;
    }
}//end-class
