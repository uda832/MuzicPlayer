package com.example.ud4.muzicplayer;

import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.content.ContextCompat;
import android.Manifest;
import com.example.ud4.muzicplayer.MusicService.MusicBinder;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;

import android.os.Bundle;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ServiceConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.media.AudioManager;
import android.net.Uri;
import android.content.Context;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;

public class MainActivity extends AppCompatActivity implements MediaPlayerControl
{
    private static final int PERMISSIONS_EXTERNAL = 0;
    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ArrayList<Song> songsList;

    private MusicController controller;
    private MusicService musicService;
    private Intent playIntent;
    private boolean bindFlag;

    private boolean paused = false;
    private boolean playbackPaused = false;
    private NoisyAudioStreamReceiver noisyReceiver;
    
    LinearLayout cbRoot;
    ImageView cbArtwork;
    TextView cbTitle;
    TextView cbArtist;
    CheckBox cbPlayPauseButton;


    /** OnCreate  */
    //*******************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songsList = new ArrayList<Song>();

        //Get Permission to access EXTERNAL STORAGE
        getPermission();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Adapter for each Fragment
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        //Hook ViewPager with the adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
      
        //setController();
        initControllerBar();
        noisyReceiver = new NoisyAudioStreamReceiver();
    }//end-onCreate

    /** OnStart  */
    //*******************************************************
    @Override
    protected void onStart()
    {
        super.onStart();
        if(playIntent==null)
        {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    /** OnStop  */
    //*******************************************************
    @Override
    protected void onStop()
    {
        controller.hide();
        unregisterReceiver(noisyReceiver);
        super.onStop();
    }

    /** OnDestroy  */
    //*******************************************************
    @Override
    protected void onDestroy()
    {
        stopService(playIntent);
        unbindService(musicConnection);
        musicService = null;
        super.onDestroy();
    }

    /** OnResume  */
    //*******************************************************
    @Override
    protected void onResume()
    {
        super.onResume();
        if (paused)
        {
            setController();
            paused = false;
        }

		if(musicService!=null && bindFlag)
            controller.show(0);
        //Handle NOISY events
        IntentFilter noiseFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(noisyReceiver, noiseFilter);
    }

    /** OnPause  */
    //*******************************************************
    @Override
    protected void onPause()
    {
        super.onPause();
        paused = true;
    }
    
    /** MediaPlayerControl Methods  */
    //*******************************************************
    @Override
	public boolean canPause() { return true; }

	@Override
	public boolean canSeekBackward() {return true;}

	@Override
	public boolean canSeekForward() {return true;}

    @Override
	public int getAudioSessionId() { return 0; }

	@Override
	public int getBufferPercentage() { return 0; }

	@Override
	public int getCurrentPosition()
    {
		if(musicService!=null && bindFlag && musicService.isPlaying())
			return musicService.getPos();
		else 
		    return 0;
	}

	@Override
	public int getDuration()
	{
		if(musicService!=null && bindFlag && musicService.isPlaying())
			return musicService.getDur();
		else 
		    return 0;
	}

	@Override
	public boolean isPlaying() {
		if(musicService!=null && bindFlag)
			return musicService.isPlaying();
		return false;
	}

	@Override
	public void pause() {
		playbackPaused=true;
		musicService.pausePlayer();
	}

	@Override
	public void seekTo(int pos) {
		musicService.seek(pos);
	}

	@Override
	public void start() {
		musicService.go();
	}

    /** ConnectToService  */
    //*******************************************************
    private ServiceConnection musicConnection = new ServiceConnection()
    {
        /** OnConnect  */
        //*****************************
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MusicBinder binder = (MusicBinder) service;
            //get service
            musicService = binder.getService();
            //pass list
            musicService.setList(songsList);
            bindFlag = true;
        }
        
        /** OnDisconnect  */
        //*****************************
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            bindFlag = false;
        }
    };

    /** CreateOptionsMenu  */
    //*******************************************************
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** OptionsMenuItemSelected*/
    //*******************************************************
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** RequestPermissionResult*/
    //*******************************************************
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode) {
            case PERMISSIONS_EXTERNAL: 
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //Granted
                    populateSongsList();
                    Collections.sort(songsList, new Comparator<Song>()
                    {
                        public int compare(Song a, Song b)
                        {
                            return a.getTitle().compareTo(b.getTitle());
                        }
                    });
                } 
                else 
                {
                    Toast.makeText(getApplicationContext(), "Permission Failed", Toast.LENGTH_SHORT).show();   
                    
                }
                return;

            // other 'case' lines to check for other
            // permissions this app might request
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }//end-permission




    /** TabFragment Class.  */
    //*******************************************************
    public static class TabFragment extends Fragment
    {
        //Identifier for each section
        private static final String ARG_SECTION_NAME = "section_name";

        public TabFragment() {
        }

        @Override
        public void onAttach(Context context)
        {
            super.onAttach(context);
        }

        //Create a fragment with given section/tab name
        public static TabFragment newInstance(String sectionName) 
        {
            TabFragment fragment = new TabFragment();
            Bundle args = new Bundle();
            args.putString(ARG_SECTION_NAME, sectionName);
            fragment.setArguments(args);
            return fragment;
        }

        /** OnCreateView   */
        //**********************************
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            String sectionName = getArguments().getString(ARG_SECTION_NAME);
            View rootView = inflater.inflate(R.layout.fragment_list, container, false);

            switch(sectionName)
            {
                //Songs
                case "songs":
                {
                    //Grab songsView
                    ListView listView = (ListView) rootView.findViewById(R.id.item_list);
                    //Set Adapter
                    SongsAdapter songsAdapter = new SongsAdapter(getActivity(), ((MainActivity) getActivity()).getSongsList());
                    listView.setAdapter(songsAdapter);
                    songsAdapter.notifyDataSetChanged();
                    break;
                }

                //Albums
                case "albums":
                {
                    rootView = inflater.inflate(R.layout.fragment_grid, container, false);
                    break;
                }
                
                //Artists
                case "artists":
                {
                    rootView = inflater.inflate(R.layout.fragment_list, container, false);
                    break;
                }

                //Playlists
                case "playlists":
                {
                    break;
                }
            }

            return rootView;
        }
    }//end-fragmentClass

    /** SectionsPagerAdapter Class */
    //*******************************************************
    public class SectionsPagerAdapter extends FragmentPagerAdapter { 
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a TabFragment (defined as a static inner class below).
            return TabFragment.newInstance(getPageTitle(position).toString());
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "songs";
                case 1:
                    return "albums";
                case 2:
                    return "artists";
                case 3:
                    return "playlists";
            }
            return null;
        }
    }

    /** NoisyAudio Class */
    //*******************************************************
    private class NoisyAudioStreamReceiver extends BroadcastReceiver
    {
        //Handle NOISY actions (i.e. headphone getting unplugged during playback etc.)
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
                pause();
        }
    }
  



    /** PopulateSongsList */
    //*******************************************************
    public void populateSongsList()
    {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst())
        {
            //get columns
            int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM);

            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                songsList.add(new Song(thisId, thisTitle, thisArtist, thisAlbum));
            } while (musicCursor.moveToNext());
        }
    }//end-populate

    /** GetSongsList */
    //*******************************************************
    public ArrayList<Song> getSongsList()
    {
        return songsList;
    }

    /** SetController */
    //*******************************************************
    public void setController()
    {
        if (controller == null)
            controller = new MusicController(this);

        controller.setPrevNextListeners(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    playNext();
                }
            }, new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    playPrev();
                }

            });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.container));
        controller.setEnabled(true);

		if(musicService!=null && bindFlag)
            musicService.setController(controller);
    }

    /** GetPermission */
    //*******************************************************
    public void getPermission()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_EXTERNAL);
        }
    }

    /** SongOnClick */
    //*******************************************************
    public void songOnClick(View view)
    {
        musicService.setSong(Integer.parseInt(view.getTag().toString()));
        musicService.playSong();
        if (playbackPaused)
        {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    /** PlayNext */
    //*******************************************************
    private void playNext()
    {
        musicService.playNext();
        if (playbackPaused)
        {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    /** PlayPrev */
    //*******************************************************
    private void playPrev()
    {
        musicService.playPrev();
        if (playbackPaused)
        {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    /** InitContollerBar */
    //*******************************************************
    public void initControllerBar()
    {
        cbRoot = (LinearLayout) findViewById(R.id.controller_bar);
        cbArtwork = (ImageView) cb.findViewById(R.id.cb_art);
        cbTitle = (TextView) cb.findViewById(R.id.cb_title);
        cbArtist = (TextView) cb.findViewById(R.id.cb_artist);
        cbPlayPauseButton = (TextView) cb.findViewById(R.id.cb_play_pause);

        //Listener for ControllerBar -- Go to NowPlayingActivity
        cb.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                // IMPLEMENT ME:
                // Intent to go to NowPlayingActivity
                Toast.makeText(getApplicationContext(), "Go To NowPlayingActivity", Toast.LENGTH_SHORT).show();   
            }
        });

        //Listener for Play/Pause -- send message to the MediaPlaer
        playPauseButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() 
        {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean flag) 
            {
                CheckBox button = (CheckBox) v;
                boolean state = button.isChecked();
                
                //IsPlaying
                if (state)
                    musicService.go();
                //IsPaused
                else
                {
                    playbackPaused = true;
                    musicService.pausePlayer();
                }
            }
        });

    }
}//end-class


