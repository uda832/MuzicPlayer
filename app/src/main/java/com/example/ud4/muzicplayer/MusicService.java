package com.example.ud4.muzicplayer;

import android.app.Service;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.util.Log;
import android.content.Intent;
import android.os.IBinder;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener
{
    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int songPos;
    private final IBinder musicBind = new MusicBinder();


    /** OnCreate  */
    //*******************************************************
    @Override
    public void onCreate()
    {
        super.onCreate();
        songPos = 0;
        player = new MediaPlayer();
        initPlayer();
    }

    /** OnPrepared  */
    //*******************************************************
    @Override
    public void onPrepared(MediaPlayer mp)
    {
        //start playback
        mp.start();
    }

    /** OnCompletion  */
    //*******************************************************
    @Override
	public void onCompletion(MediaPlayer mp)
    {
        //If playback has reached the end of a track
        if(player.getCurrentPosition() > 0)
        {
            mp.reset();
            //playNext();
        }
	}

    /** OnError  */
    //*******************************************************
    @Override
	public boolean onError(MediaPlayer mp, int what, int extra)
    {
		Log.v("MUSIC PLAYER", "Playback Error");
		mp.reset();
		return false;
	}

    /** OnBind  */
    //*******************************************************
    @Override
    public IBinder onBind(Intent arg0)
    {
        return musicBind;
    }

    /** OnUnbind */
    //*******************************************************
    @Override
    public boolean onUnbind(Intent arg0)
    {
        player.stop();
        player.release();

        return false;
    }

    /** InitPlayer  */
    //*******************************************************
    public void initPlayer()
    {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }//end-init

    /** SetList  */
    //*******************************************************
    public void setList(ArrayList<Song> theSongs)
    {
        songs = theSongs;
    }

    /** SetSong  */
    //*******************************************************
    public void setSong(int songIndex)
    {
        songPos = songIndex;
    }

    /** MusicBinder  */
    //*******************************************************
    public class MusicBinder extends Binder
    {
        MusicService getService()
        {
            return MusicService.this;
        }
    }

    /** PlaySong   */
    //*******************************************************
    public void playSong()
    {
        player.reset();

        Song toPlay = songs.get(songPos);
        long toPlayId = toPlay.getId();
        Uri trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, toPlayId);

        try
        {
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        
        player.prepareAsync();
    }

}


