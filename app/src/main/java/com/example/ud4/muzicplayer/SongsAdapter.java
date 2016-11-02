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
        ViewHolder holder;

        // Inflate
        if (view == null) 
        {
            view = (LinearLayout) LayoutInflater.from(getContext()).inflate( R.layout.song, parent, false);

            holder = new ViewHolder();
            holder.imageView = (ImageView) view.findViewById(R.id.song_image);
            holder.titleView = (TextView) view.findViewById(R.id.song_title);
            holder.artistView = (TextView) view.findViewById(R.id.song_artist);

            view.setTag(holder);
        }
        //Recycle
        else
            holder = (ViewHolder) view.getTag();

        //Grab data from object
        Song song = getItem(position);
        Uri albumArtUri = song.getAlbumArtUri();

        //Set data as views' contents
        Picasso.with(mContext)
               .load(albumArtUri)
               .fit()
               .centerCrop()
               .into(holder.imageView);
        holder.titleView.setText(song.getTitle());
        holder.artistView.setText(song.getArtist());
        
        return view;
    }

    private static class ViewHolder
    {
        public ImageView imageView;
        public TextView titleView;
        public TextView artistView;
    }

    

}//end-class
