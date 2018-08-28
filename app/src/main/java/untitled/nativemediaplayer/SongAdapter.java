package untitled.nativemediaplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SongAdapter extends BaseAdapter {
    private ArrayList<Song> songs;
    private LayoutInflater songInflater;

    public SongAdapter(Context c, ArrayList<Song> songList){
        this.songs = songList;
        this.songInflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /**
     * Provides a view for the SongList
     * @param position is the position in the list of data that should be displayed in the
     *                 list item view.
     * @param convertView is the recycled view to populate.
     * @param parent is the parent ViewGroup that is used for inflation.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //Inflate song layout
        LinearLayout songLayout = (LinearLayout)songInflater.inflate(R.layout.song, parent, false);

        //Get the Title/Artist text views
        TextView songView = songLayout.findViewById(R.id.song_title);
        TextView artistView = songLayout.findViewById(R.id.song_artist);

        //get the song using the position
        Song currentSong = songs.get(position);

        //Display song & artist information in their appropriate fields
        songView.setText(currentSong.getTitle());
        artistView.setText(currentSong.getArtist());

        //set Position as tag
        songLayout.setTag(position);
        return songLayout;
    }
}
