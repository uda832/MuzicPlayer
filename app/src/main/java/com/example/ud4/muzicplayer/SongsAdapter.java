package com.example.ud4.muzicplayer;

import java.util.ArrayList;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.net.Uri;
import com.squareup.picasso.Picasso;

public class SongsAdapter extends ArrayAdapter<Song>
{
    private Context mContext;
    public SongsAdapter(Context context, ArrayList<Song> list)
    {
        super(context, 0, list);
        mContext = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent)
    {
        LinearLayout listItemView = (LinearLayout) view;

        // Recycle or Inflate
        if (listItemView == null) 
        {
            listItemView = (LinearLayout) LayoutInflater.from(getContext()).inflate(
                R.layout.song, parent, false);
        }

        //Grab data from object
        Song song = getItem(position);
        String title = song.getTitle();
        String artist = song.getArtist();
        Uri albumArtUri = song.getAlbumArtUri();

        //Grab Views
        ImageView imageView = (ImageView) listItemView.findViewById(R.id.song_image);
        TextView titleView = (TextView) listItemView.findViewById(R.id.song_title);
        TextView artistView = (TextView) listItemView.findViewById(R.id.song_artist);


        //Set data as views' contents
        Picasso.with(mContext)
               .load(albumArtUri)
               .fit()
               .centerCrop()
               .into(imageView);
        titleView.setText(title);
        artistView.setText(artist);
        listItemView.setTag(position);
        
        return listItemView;
    }

    

}//end-class
