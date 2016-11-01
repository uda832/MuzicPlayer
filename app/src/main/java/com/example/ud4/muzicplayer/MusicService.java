package com.example.ud4.muzicplayer;

import java.util.Random;
import java.util.ArrayList;
import android.app.Service;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.os.IBinder;
import android.util.Log;



public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener
{
    private MediaPlayer player;
    private ArrayList<Song> songs;
    private final IBinder musicBind = new MusicBinder();

    private int songPos;
    private String songTitle="";
    private static final int NOTIFY_ID=1;
    //private MusicController controller;
    private ServiceCallback updaterCallback;

    /** OnCreate  */
    //*******************************************************
    @Override
    public void onCreate()
    {
        super.onCreate();
        songPos = 0;
        player = new MediaPlayer();
        initPlayer();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        
    }

    /** OnAudioFocusChange */
    //*******************************************************
    @Override
    public void onAudioFocusChange(int focusChange)
    {
        switch (focusChange)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (player == null) 
                    initPlayer();
                else if (!player.isPlaying()) 
                    player.start();
                player.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                player.release();
                player = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (player.isPlaying()) 
                    player.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (player.isPlaying()) 
                    player.setVolume(0.1f, 0.1f);
                break;
        }
    }

    /** OnDestroy */
    //*******************************************************
    @Override
    public void onDestroy()
    {
        if (player != null) 
            player.release();
        stopForeground(true);
    }

    /** OnPrepared  */
    //*******************************************************
    @Override
    public void onPrepared(MediaPlayer mp)
    {
        //start playback
        mp.start();
        
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Notification
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(pendInt)
		.setSmallIcon(R.drawable.play)
		.setTicker(songTitle)
		.setOngoing(true)
		.setContentTitle("Playing")
		.setContentText(songTitle);
		Notification not = builder.build();
		startForeground(NOTIFY_ID, not);

        //Update controllerBar
        if (updaterCallback != null)
            updaterCallback.updateControllerBar();

        //controller.show(0);
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
            playNext();
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

    /** GetSongPos  */
    //*******************************************************
    public int getSongPos()
    {
        return songPos;
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
        songTitle = toPlay.getTitle();

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

    /** Playnext   */
    //*******************************************************
    public void playNext()
    {
        songPos++;
        if(songPos >= songs.size()) 
            songPos=0;
		playSong();
    }

    /** PlayPrev   */
    //*******************************************************
    public void playPrev()
    {
        songPos--;
        if(songPos < 0)
            songPos = songs.size() - 1;
		playSong();
    }

    /** ControlActions   */
    //*******************************************************
    public int getCurrentPos()
    {
        return player.getCurrentPosition();
    }
    public int getDur()
    {
        return player.getDuration();
    }
    public boolean isPlaying()
    {
        return player.isPlaying();
    }
    public void pausePlayer()
    {
        player.pause();
    }
    public void seek(int posn)
    {
        player.seekTo(posn);
    }
    public void go()
    {
        player.start();
    }


    //[>* SetController<]
    ///[>******************************************************
    //public void setController(MusicController c)
    //{
        //controller = c;
    //}

    /** SetCallback   */
    //*******************************************************
    public void setCallback(ServiceCallback callbacks)
    {
        updaterCallback = callbacks;
    }


}


