 /**
 * MusicService Class
 *
 *
 *
 */
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
import android.support.v4.content.LocalBroadcastManager;



public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener
{
    public static final String SEEKBAR_RESULT = "com.example.ud4.muzicplayer.SEEKBAR_RES";
    public static final String SEEKBAR_MAX = "com.example.ud4.muzicplayer.SEEKBAR_MAX";
    private MediaPlayer player;
    private ArrayList<Song> songs;
    private final IBinder musicBind = new MusicBinder();

    private int songPos;
    private String songTitle="";
    private static final int NOTIFY_ID=1;
    //private MusicController controller;
    private ServiceCallback updaterCallback;
    private LocalBroadcastManager broadcaster;

    /** OnCreate  */
    //*******************************************************
    @Override
    public void onCreate()
    {
        super.onCreate();
        songPos = 0;
        initPlayer();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        broadcaster = LocalBroadcastManager.getInstance(this);

    }//end

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
    }//end

    /** OnDestroy */
    //*******************************************************
    @Override
    public void onDestroy()
    {
        if (player != null) 
            player.release();
        stopForeground(true);
    }//end

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

        //Send SeekBar info
        sendSeekBarMax();


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

        //SendSeekBarInfo
    }//end

    /** OnCompletion  */
    //*******************************************************
    @Override
	public void onCompletion(MediaPlayer mp)
    {
        //If playback has reached the end of a track
        if(player.getCurrentPosition() > 0)
        {
            mp.reset();

            //PlayNext
            songPos++;
            if(songPos >= songs.size()) 
                songPos=0;
            playSong();
        }
	}//end

    /** OnError  */
    //*******************************************************
    @Override
	public boolean onError(MediaPlayer mp, int what, int extra)
    {
		Log.v("MUSIC PLAYER", "Playback Error");
		mp.reset();
		return false;
	}//end

    /** OnBind  */
    //*******************************************************
    @Override
    public IBinder onBind(Intent arg0)
    {
        return musicBind;
    }//end

    /** OnUnbind */
    //*******************************************************
    @Override
    public boolean onUnbind(Intent arg0)
    {
        player.stop();
        player.release();

        return false;
    }//end



    /** InitPlayer  */
    //*******************************************************
    public void initPlayer()
    {
        if (player == null)
            player = new MediaPlayer();

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
    }//end

    /** SetSong  */
    //*******************************************************
    public void setSong(int songIndex)
    {
        songPos = songIndex;
    }//end

    /** GetSongPos  */
    //*******************************************************
    public int getSongPos()
    {
        return songPos;
    }//end

    /** MusicBinder  */
    //*******************************************************
    public class MusicBinder extends Binder
    {
        MusicService getService()
        {
            return MusicService.this;
        }
    }//end

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
    }//end

    //[>* Playnext   <]
    ///[>******************************************************
    //public void playNext()
    //{
    //}//end

    //[>* PlayPrev   <]
    ///[>******************************************************
    //public void playPrev()
    //{
        //songPos--;
        //if(songPos < 0)
            //songPos = songs.size() - 1;
		//playSong();
    //}//end

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
    //end-ControlActions


    //[>* SetController<]
    //******************************************************
    //public void setController(MusicController c)
    //{
        //controller = c;
    //}//end

    /** SetCallback   */
    //*******************************************************
    public void setCallback(ServiceCallback callbacks)
    {
        updaterCallback = callbacks;
    }//end

    /** SendSeekBarMax   */
    //*******************************************************
    private void sendSeekBarMax()
    {
        Intent intent = new Intent(SEEKBAR_RESULT);
        intent.putExtra(SEEKBAR_MAX, (int) player.getDuration());
        broadcaster.sendBroadcast(intent);
    }//end-Send
}


