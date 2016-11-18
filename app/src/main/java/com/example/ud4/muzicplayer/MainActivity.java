package com.example.ud4.muzicplayer;

//Import Statements
//****************
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
import android.support.percent.PercentRelativeLayout;
import android.Manifest;
import com.example.ud4.muzicplayer.MusicService.MusicBinder;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.GridView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.text.TextUtils;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.content.ServiceConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import android.media.AudioManager;
import android.net.Uri;
import android.content.Context;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;
//end-imports

public class MainActivity extends AppCompatActivity implements ServiceCallback, Target 
{
    private static final int PERMISSIONS_EXTERNAL = 0;
    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ArrayList<Song> songsList;
    private ArrayList<Album> albumsList;

    private MusicService musicService;
    private Intent playIntent;
    private boolean bindFlag;
    private boolean paused = false;
    private boolean playbackPaused = false;
    
    //Views and Layouts
    private SlidingUpPanelLayout rootLayout;
    private RelativeLayout cbRoot;
    private ImageView cbArtwork;
    private TextView cbTitle;
    private TextView cbArtist;
    private CheckBox cbPlayPauseButton;
    private PercentRelativeLayout npRoot;
    private ImageView npArtwork;
    private TextView npTitle;
    private TextView npArtist;
    private SeekBar npSeekbar;
    private TextView npCurrentTime;
    private TextView npMaxTime;
    private CheckBox npPlayPauseButton;
    private ImageButton npMoreButton;

    private BlurTransformation blurTransformation;
    private Point backgroundSize;
    private BroadcastReceiver seekbarReceiver;
    private NoisyAudioStreamReceiver noisyReceiver;
    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduleFuture;
    private Handler mHandler = new Handler();
    private Runnable updateSeekbarTask = new Runnable()//*****
    {
        @Override
        public void run()
        {
            //Check if the musicService is bound/connected
            if (musicService!=null && bindFlag)
            {
                //Update progress bar and currentTime
                int pos = musicService.getCurrentPos() / 1000;
                npSeekbar.setProgress(pos);
                String currentTime = toSeconds(pos);
                npCurrentTime.setText(currentTime);
            }
        }
    };//end-runnable


    /** OnCreate  */
    //*******************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Populates songsList and albumsList -- Request permission if not granted already
        songsList = new ArrayList<Song>();
        albumsList = new ArrayList<Album>();
        getPermission();

        //Setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set up Sections with fragments (Songs, Albums, etc.)
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        blurTransformation = new BlurTransformation(this, 25F);
        backgroundSize = calcBackgroundSize(getWindowManager().getDefaultDisplay());

        initControllers();

        //Handle NOISY events
        noisyReceiver = new NoisyAudioStreamReceiver();
        IntentFilter noiseFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(noisyReceiver, noiseFilter);

        //SlidingPanel
        rootLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        //rootLayout.addPanelSlideListener(new PanelSlideListener()
        rootLayout.addPanelSlideListener(new MyPanelSlideListener());

        seekbarReceiver = new SeekbarBroadcastReceiver();

        
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
    }//end-onstart
    /** OnStop  */
    //*******************************************************
    @Override
    protected void onStop()
    {
        //controller.hide();
        super.onStop();
    }//end
    /** OnDestroy  */
    //*******************************************************
    @Override
    protected void onDestroy()
    {
        mExecutorService.shutdown();
        musicService.setCallback(null);     //Unregister
        stopService(playIntent);
        unbindService(musicConnection);

        unregisterReceiver(noisyReceiver);
        musicService = null;
        super.onDestroy();
    }//end
    /** OnResume  */
    //*******************************************************
    @Override
    protected void onResume()
    {
        super.onResume();
        
        if (paused)
        {
            //setController();
            updateViews();
            startSeekbarUpdater();
            paused = false;
        }
        LocalBroadcastManager.getInstance(this).registerReceiver((seekbarReceiver), new IntentFilter(MusicService.SEEKBAR_RESULT));
    }//end
    /** OnPause  */
    //*******************************************************
    @Override
    protected void onPause()
    {
        super.onPause();
        stopSeekbarUpdater();
        paused = true;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(seekbarReceiver);
    }//end

    

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
            musicService.setCallback(MainActivity.this);
        }//end
        
        /** OnDisconnect  */
        //*****************************
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            bindFlag = false;
        }//end
    };//end

    /** CreateOptionsMenu  */
    //*******************************************************
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }//end
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
    }//end

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
                    populateAlbumsList();
                    sortList(songsList);
                    sortList(albumsList);
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
        private SongsAdapter songsAdapter;
        private AlbumsAdapter albumsAdapter;

        public TabFragment() {
        }

        @Override
        public void onAttach(Context context)
        {
            super.onAttach(context);

            if (songsAdapter == null)
                songsAdapter = new SongsAdapter(getActivity(), ((MainActivity) getActivity()).getSongsList());
            if (albumsAdapter == null)
                albumsAdapter = new AlbumsAdapter(getActivity(), ((MainActivity) getActivity()).getAlbumsList());
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
                    listView.setAdapter(songsAdapter);
                    listView.setOnItemClickListener(new ListView.OnItemClickListener()
                    {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                        {
                            ((MainActivity) getActivity()).songOnClick(position);
                        }
                    });
                    songsAdapter.notifyDataSetChanged();
                    break;
                }
                //Albums
                case "albums":
                {
                    rootView = inflater.inflate(R.layout.fragment_grid, container, false);
                    GridView gridView = (GridView) rootView.findViewById(R.id.item_list);

                    //Set Adapter
                    gridView.setAdapter(albumsAdapter);
                    gridView.setOnItemClickListener(new GridView.OnItemClickListener()
                    {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                        {
                            ((MainActivity) getActivity()).albumOnClick(position);
                        }
                    });
                    songsAdapter.notifyDataSetChanged();
                    break;
                }
                
                //Artists
                case "artists":
                {
                    break;
                }

                //Playlists
                case "playlists":
                {
                    break;
                }
            }

            return rootView;
        }//end

    }//end-fragmentClass

    /** SectionsPagerAdapter Class */
    //*******************************************************
    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
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
    }//end-PagerAdapter

    /** NoisyAudio Class */
    //*******************************************************
    private class NoisyAudioStreamReceiver extends BroadcastReceiver
    {
        //Handle NOISY actions (i.e. headphone getting unplugged during playback etc.)
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                if(musicService!=null && bindFlag)
                    musicService.pausePlayer();
            }
        }
    }//end

    /** MyPanelSlideListener Class */
    //*******************************************************
    private class MyPanelSlideListener implements PanelSlideListener
    {
        // Slide
        //***********
        @Override
        public void onPanelSlide(View panel, float slideOffset)
        {
            //Show NowPlaying
            if (slideOffset > 0.60)
            {
                //HIDE ControllerBar Views
                cbArtwork.setVisibility(View.INVISIBLE);
                cbTitle.setVisibility(View.INVISIBLE);
                cbArtist.setVisibility(View.INVISIBLE);
                cbPlayPauseButton.setVisibility(View.INVISIBLE);
            }
            //Show ControllerBar
            else
            {
                //SHOW ControllerBar Views
                cbArtwork.setVisibility(View.VISIBLE);
                cbTitle.setVisibility(View.VISIBLE);
                cbArtist.setVisibility(View.VISIBLE);
                cbPlayPauseButton.setVisibility(View.VISIBLE);
            }
        }//end-slide

        // State
        //***********
        @Override
        public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) 
        {
            View decorView = getWindow().getDecorView();

            //SHOW-HIDE the statusBar
            if (newState == PanelState.COLLAPSED)
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            else if (newState == PanelState.EXPANDED)
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        }//end-state
    }//end-listener

    /** SeekbarBroadcastReceiver Class */
    //*******************************************************
    private class SeekbarBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Player has prepared the song:
            
            //Update SeekBar
            int seekBarMax = intent.getIntExtra(MusicService.SEEKBAR_MAX, 0) / 1000;
            String maxTime = toSeconds(seekBarMax);
            npSeekbar.setMax(seekBarMax);
            npMaxTime.setText(maxTime);

            //Update progress every second (on UI thread)
            startSeekbarUpdater();
        }
    }//end-receiver



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
            int albumIdColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM_ID);

            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                long thisAlbumId = musicCursor.getLong(albumIdColumn);

                songsList.add(new Song(thisId, thisTitle, thisArtist, thisAlbumId));
            } while (musicCursor.moveToNext());
        }
    }//end-populate
    /** PopulateAlbumsList */
    //*******************************************************
    public void populateAlbumsList()
    {
        final Uri uri = android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        final String _id = android.provider.MediaStore.Audio.Albums._ID;
        final String album_name = android.provider.MediaStore.Audio.Albums.ALBUM;
        final String artist = android.provider.MediaStore.Audio.Albums.ARTIST;
        final String albumart = android.provider.MediaStore.Audio.Albums.ALBUM_ART;

        final String[] columns = { _id, album_name, artist, albumart};
        Cursor cursor = getContentResolver().query(uri, columns, null, null, null);

        if(cursor!=null && cursor.moveToFirst())
        {
            do 
            {
                long id = cursor.getLong(cursor.getColumnIndex(_id));
                String name = cursor.getString(cursor.getColumnIndex(album_name));
                String artist2 = cursor.getString(cursor.getColumnIndex(artist));
                
                albumsList.add(new Album(id, name, artist2));
                
            } while (cursor.moveToNext());
        }
        cursor.close();
    }//end-populate
    /** SortList */
    //*******************************************************
    public <T extends TitleInterface> void sortList(ArrayList<T> list)
    {
        Collections.sort(list, new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }//end
    /** GetSongsList */
    //*******************************************************
    public ArrayList<Song> getSongsList()
    {
        return songsList;
    }//end
    /** GetAlbumsList */
    //*******************************************************
    public ArrayList<Album> getAlbumsList()
    {
        return albumsList;
    }//end

    //SetController 
    //******************************************************
    //public void setController()
    //{
        //if (controller == null)
            //controller = new MusicController(this);

        //controller.setPrevNextListeners(new View.OnClickListener()
            //{
                //@Override
                //public void onClick(View v)
                //{
                    //playNext();
                //}
            //}, new View.OnClickListener()
            //{
                //@Override
                //public void onClick(View v)
                //{
                    //playPrev();
                //}

            //});
        //controller.setMediaPlayer(this);
        //controller.setAnchorView(findViewById(R.id.container));
        //controller.setEnabled(true);

		//if(musicService!=null && bindFlag)
            //musicService.setController(controller);
    //}//end

    /** GetPermission */
    //*******************************************************
    public void getPermission()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_EXTERNAL);
        }
        else
        {
            populateSongsList();
            populateAlbumsList();
            sortList(songsList);
            sortList(albumsList);
        }
    }//end-getter

    /** SongOnClick */
    //*******************************************************
    public void songOnClick(int position)
    {
        musicService.setSong(position);
        updateViews();
        cbPlayPauseButton.setChecked(true);
        if (playbackPaused)
        {
            playbackPaused = false;
        }
        musicService.playSong();
        rootLayout.setPanelState(PanelState.EXPANDED);
    }//end
    /** AlbumOnClick */
    //*******************************************************
    public void albumOnClick(int position)
    {
        //Open full-screen fragment
    }//end

    /** PlayNext */
    //*******************************************************
    private void playNext()
    {
        int songPos = musicService.getSongPos() + 1;
        if(songPos >= songsList.size()) 
            songPos=0;

        musicService.setSong(songPos);
        updateViews();

        if (playbackPaused)
        {
            //setController();
            playbackPaused = false;
        }
        musicService.playSong();
        //controller.show(0);
    }//end
    /** PlayPrev */
    //*******************************************************
    private void playPrev()
    {
        int songPos = musicService.getSongPos() - 1;
        if(songPos < 0) 
            songPos = songsList.size() - 1;

        musicService.setSong(songPos);
        updateViews();
        if (playbackPaused)
        {
            //setController();
            playbackPaused = false;
        }
        musicService.playSong();
        //controller.show(0);
    }//end

    /** InitContollers */
    //*******************************************************
    public void initControllers()
    {
        //ControllerBar Views
        cbRoot = (RelativeLayout) findViewById(R.id.controller_bar);
        cbArtwork = (ImageView) cbRoot.findViewById(R.id.cb_art);
        cbTitle = (TextView) cbRoot.findViewById(R.id.cb_title);
        cbArtist = (TextView) cbRoot.findViewById(R.id.cb_artist);
        cbPlayPauseButton = (CheckBox) cbRoot.findViewById(R.id.cb_play_pause);

        //NowPlayingPanel Views
        npRoot = (PercentRelativeLayout) findViewById(R.id.now_playing_root);
        npArtwork = (ImageView) npRoot.findViewById(R.id.np_art);
        npTitle = (TextView) npRoot.findViewById(R.id.np_title);
        npArtist = (TextView) npRoot.findViewById(R.id.np_artist);
        npSeekbar = (SeekBar) npRoot.findViewById(R.id.np_seekbar);
        npCurrentTime = (TextView) npRoot.findViewById(R.id.np_current_time);
        npMaxTime = (TextView) npRoot.findViewById(R.id.np_max_time);
        npPlayPauseButton = (CheckBox) npRoot.findViewById(R.id.np_play_pause);
        npMoreButton = (ImageButton) npRoot.findViewById(R.id.np_morebutton);

        //SeekBarChangeListener
        npSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
        {//*****
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser)
            {
                progress = progresValue;
                String currentTime = toSeconds(progress);
                npCurrentTime.setText(currentTime);

                if(fromUser)
                {
                    musicService.seek(progress * 1000); 
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) 
            {
                stopSeekbarUpdater();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                startSeekbarUpdater();
            }
        });//end-seekbar

        //PlayPauseButton Listener for the ControllerBar
        cbPlayPauseButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() 
        {//*****
            @Override
            public void onCheckedChanged(CompoundButton v, boolean flag) 
            {
                CheckBox button = (CheckBox) v;
                boolean state = button.isChecked();
                npPlayPauseButton.setChecked(state);

                //IsPlaying
                if (state)
                {
                    playbackPaused = false;
                    //Start playing
                    musicService.go();
                    startSeekbarUpdater();
                }
                //IsPaused
                else
                {
                    playbackPaused = true;
                    musicService.pausePlayer();
                    stopSeekbarUpdater();
                }
            }
        });//end

        //PlayPauseButton Listener for the NowPlayingPanel
        npPlayPauseButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() 
        {//*****
            @Override
            public void onCheckedChanged(CompoundButton v, boolean flag) 
            {
                CheckBox button = (CheckBox) v;
                boolean state = button.isChecked();
                cbPlayPauseButton.setChecked(state);
                
                //IsPlaying
                if (state)
                {
                    playbackPaused = false;
                    //Start playing
                    musicService.go();
                    startSeekbarUpdater();
                }
                //IsPaused
                else
                {
                    playbackPaused = true;
                    musicService.pausePlayer();
                    stopSeekbarUpdater();
                }
            }
        });//end

        //MoreMenu Listener for the NowPlayingPanel
        npMoreButton.setOnClickListener(new View.OnClickListener()
        {//*****
            @Override
            public void onClick(View view)
            {
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()//*****
                {
                    @Override 
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        switch (item.getItemId()) {
                            case R.id.np_goto_artist:
                                return true;
                            case R.id.np_goto_album:
                                return true;
                            case R.id.np_addto_playlist:
                                return true;
                            case R.id.np_settings:
                                return true;
                            default:
                                return false;
                        }
                    }
                });//end-menu

                popup.inflate(R.menu.np_menu);
                popup.show();
            }
        });//end-more

    }//end
    /** UpdateViews */
    //*******************************************************
    @Override
    public void updateViews()
    {
        int pos = 0;

        if(musicService!=null && bindFlag)
            pos = musicService.getSongPos();

        //Grab song to display
        Song toDisplay = songsList.get(pos);
        
        //Artwork
        Uri uri = toDisplay.getAlbumArtUri();
        //Background
        Picasso.with(this)
               .load(uri)
               .resize(backgroundSize.x, backgroundSize.y).centerCrop()
               .transform(blurTransformation)
               .into(this);

        //Images
        Picasso.with(this)
               .load(uri)
               .fit()
               .centerCrop()
               .into(cbArtwork);
        Picasso.with(this)
               .load(uri)
               .fit()
               .centerCrop()
               .into(npArtwork);

        //Song Title
        cbTitle.setText(toDisplay.getTitle());
        cbTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        cbTitle.setSingleLine(true);
        cbTitle.setMarqueeRepeatLimit(-1);
        cbTitle.setSelected(true);

        npTitle.setText(toDisplay.getTitle());
        npTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        npTitle.setSingleLine(true);
        npTitle.setMarqueeRepeatLimit(-1);
        npTitle.setSelected(true);

        //Artist
        cbArtist.setText(toDisplay.getArtist());
        npArtist.setText(toDisplay.getArtist());

        //PlayPauseButton
        if(musicService!=null && bindFlag)
        {
            cbPlayPauseButton.setChecked(musicService.isPlaying());
            npPlayPauseButton.setChecked(musicService.isPlaying());
        }
    }//end-updater

    /** StartSeekbarUpdater */
    //*******************************************************
    private void startSeekbarUpdater()
    {
        stopSeekbarUpdater();    
        if (!mExecutorService.isShutdown())
        {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(new Runnable() 
            {
                @Override
                public void run()
                {
                    mHandler.post(updateSeekbarTask);
                }
            }, 100, 1000, TimeUnit.MILLISECONDS);
        }

    }//end-updater
    /** StopSeekbarUpdater */
    //*******************************************************
    private void stopSeekbarUpdater() 
    {
        if (mScheduleFuture != null)
        {
            mScheduleFuture.cancel(false);
        }
    }//end-updater



    /** Convert toSeconds */
    //*******************************************************
    private String toSeconds(int time)
    {
        long min = TimeUnit.SECONDS.toMinutes(time);
        long sec = time - TimeUnit.MINUTES.toSeconds(min);
        return String.format("%02d", min) + ":" + String.format("%02d", sec);
    }//end-convert
    /** CalcBackgroundSize */
    //*******************************************************
    private static Point calcBackgroundSize(Display display)
    {
        final Point screenSize = new Point();
        display.getSize(screenSize);

        int scaledWidth = (int) (((double) 720 * screenSize.x) / screenSize.y);
        int croppedWidth = Math.min(scaledWidth, 720);

        int scaledHeight = (int) (((double) 720 * screenSize.y) / screenSize.x);
        int croppedHeight = Math.min(scaledHeight, 720);

        return new Point(croppedWidth, croppedHeight);
    }//end-background
    /** TargetInterfaceMethods */
    //*******************************************************
    @Override
    public void onBitmapFailed(Drawable drawable) {
        getWindow().setBackgroundDrawable(drawable);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
        getWindow().setBackgroundDrawable(
                new BitmapDrawable(getResources(), bitmap));
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        // just prepare mentally
    }
    //end-TargetMethods
}//end-class
