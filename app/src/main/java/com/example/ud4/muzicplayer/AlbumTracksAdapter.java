package com.example.ud4.muzicplayer;

import java.util.ArrayList;//{{{
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.net.Uri;
import com.squareup.picasso.Picasso;//}}}

public class AlbumTracksAdapter extends ArrayAdapter<Song>
{
    private Context mContext;
    public AlbumTracksAdapter(Context context, ArrayList<Song> list)
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
            view = (RelativeLayout) LayoutInflater.from(getContext()).inflate(R.layout.album_track, parent, false);

            holder = new ViewHolder();
            holder.numView = (TextView) view.findViewById(R.id.album_track_num);
            holder.titleView = (TextView) view.findViewById(R.id.album_track_title);
            holder.durationView = (TextView) view.findViewById(R.id.album_track_duration);

            view.setTag(holder);
        }
        //Recycle
        else
            holder = (ViewHolder) view.getTag();

        //Grab data from object
        Song song = getItem(position);

        //holder.numView.setText(song.getTrackNum());
//DEBUG
holder.numView.setText("0");
        holder.titleView.setText(song.getTitle());

        long millis = song.getDuration();
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;

        String time = String.format("%02d:%02d", minute, second);
        holder.durationView.setText(time);

        
        return view;
    }

    private static class ViewHolder
    {
        public TextView numView;
        public TextView titleView;
        public TextView durationView;
    }

    

}//end-class
