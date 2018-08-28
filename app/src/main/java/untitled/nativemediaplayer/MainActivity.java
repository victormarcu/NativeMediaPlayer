package untitled.nativemediaplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ActionMenuItemView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ArrayList<Song> songList;
    private ListView songView;
    private static TextView currentSongView;

    //Service & Intennt
    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;
    private Vibrator myVib;

    //Instance of MusicController
    private static MusicController musicController;

    // Constant used as a parameter to assist with the permission requesting process.
    private final int PERMISSION_CODE = 1;

    private boolean paused = false;
    private boolean playbackPaused = false;
    private boolean isShuffleActive = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Requests permission
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
            return;
        }

        init();

        // Song Adapter class: takes the song list found on the phone and populates the view with Title &  Artist name
        SongAdapter songAdapter = new SongAdapter(this, songList);
        songView.setAdapter(songAdapter);

        //Set the MusicController widget
        setMusicController();

    }

    /**
     * Initializing method.
     */
    private void init() {
        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        currentSongView = findViewById(R.id.currentSongView);

        //Invokes the method that searches the phone for possible songs and populates the arrayList with <Song> objects
        getSongList();

        //helper method that sorts song list Alphabetically
        sortListAlphabetically();
    }

    //Shuffle & End button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    //Connect to service using the binder (helper class)
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;

            //get the service & pass the song list - update the bound flag.
            musicService = binder.getService();
            musicService.setSongList(songList);
            musicBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    /**
     * Start the Service instance when the Activity instance starts
     */
    protected void onStart(){
        super.onStart();
        if(playIntent == null){
            //Create new Intent using the Service class and bind it.
            if(!musicBound) {
                playIntent = new Intent(this, MusicService.class);

                //use the intent object created above so that when the connection
                //to the bound Service instance is made, the song list is passed.
                bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
                startService(playIntent);
            }
        }
    }

    /**
     * Song position is set as Tag for each item in the listView in the Adapter class
     * Pass this tag (song position) to the music service before starting the playback.
     */
    public void songPicked(View view){
        myVib.vibrate(25);
        songView.setPadding(0,0,0,350);

        int indexOfSongPicked = Integer.parseInt(view.getTag().toString());

        musicService.setSong(indexOfSongPicked);
        musicService.playSong();

        if(playbackPaused){
            setMusicController();
            playbackPaused = false;
        }

        //Show Controller & currentSong textView.
        showController();
        SystemClock.sleep(50);
        currentSongView.setVisibility(View.VISIBLE);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        ActionMenuItemView shuffle = findViewById(R.id.action_shuffle);
        switch (item.getItemId()){
            case R.id.action_shuffle:
                myVib.vibrate(25);
                if(!isShuffleActive){
                    isShuffleActive = true;
                    Toast.makeText(this, "Shuffle: ON", Toast.LENGTH_SHORT).show();
                    shuffle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rand2, 0,0,0);
                }else{
                    isShuffleActive = false;
                    Toast.makeText(this, "Shuffle: OFF", Toast.LENGTH_SHORT).show();
                    shuffle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rand, 0,0,0);
                }
                musicService.setShuffle();
                break;
            case R.id.action_end:
                myVib.vibrate(25);
                stopService(playIntent);
                musicService = null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Method that checks the phone and populates an arrayList with <song> Objects
     */
    public void getSongList(){
        //Create Content Resolver instance & retrieve URI for external music files
        ContentResolver musicResolver = getContentResolver();
        Uri musicURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        //Create cursor instance using the Content Resolver to /query/ the music files
        Cursor musicCursor = musicResolver.query(musicURI, null, null, null, null);

        //iterate through the results IF results available.
        if(musicCursor != null && musicCursor.moveToFirst()){
            //get columns
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            //Use the column indexes to create a new song object & add it to the songList.
            do{
                long id = musicCursor.getLong(idColumn);
                String title = musicCursor.getString(titleColumn);
                String artist = musicCursor.getString(artistColumn);

                this.songList.add(new Song(id, title, artist));
            }while(musicCursor.moveToNext());
        }

        try {
            musicCursor.close();
        }catch (NullPointerException e){
            Log.d("getSongList()","ERROR releasing the cursor. Cursor already released");
        }
    }

    /**
     * Method that instantiates the MediaController widget which is a
     * standard widget with play/pause, rewind, fast-forward, and skip (previous/next) buttons & seek bar.
     */
    public void setMusicController() {
        musicController = new MusicController(this);
        musicController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myVib.vibrate(15);
                playNext();
            }
        }, new View.OnClickListener(){
            public void onClick(View v){
                myVib.vibrate(15);
                playPrev();
            }
        });

        try {
            currentSongView.setPadding(0, 17, 0, 283);
        }catch (NullPointerException e){
            Log.d("currentSongView error:", "View not yet initialized");
        }

        musicController.setMediaPlayer(this);
        musicController.setAnchorView(currentSongView);
        musicController.setEnabled(true);
    }

    /**
     * 2 methods that facilitate skipping to the next/prev song by calling methods from the Service.
     * reset the controller and update the playbackPaused flag when playback has been paused beforehand to ensure no unpredictable behaviour.
     */
    public void playNext(){
        musicService.playNext();
        if(playbackPaused){
            setMusicController();
            playbackPaused = false;
        }
        showController();
    }
    public void playPrev(){
        musicService.playPrev();
        if(playbackPaused){
            setMusicController();
            playbackPaused = false;
        }
        showController();
    }


    /**
     * Updates the musicController widget
     * Method is static because it is called by the Music Service once the song is ready to be played.
     */
    public static void showController() {
        Log.d("Controller", "showController()");
        musicController.show(0);
    }

    /**
     * MediaPlayerControl widget interface methods
     */
    @Override
    public void start() {
        if(musicService != null && musicBound && !musicService.isPlaying()){
            myVib.vibrate(15);
            musicService.go();
        }
    }

    @Override
    public void pause() {
        if(musicService !=null && musicBound && musicService.isPlaying()){
            playbackPaused = true;
            myVib.vibrate(15);
            musicService.pausePlayer();
        }
    }

    /**
     * Getter method for the song's length
     * makes a call to the service to return song length or 0 if nothing is playing.
     */
    @Override
    public int getDuration() {
        if(musicService != null && musicBound && musicService.isPlaying()){
            return musicService.getDuration();

        }else if(playbackPaused){
                try {
                    return musicService.getDuration();
                }catch(NullPointerException e){
                    return 0;
                }
            }
        return 0;
    }

    /**
     *  Call to the service to return current position of the song.
     */
    @Override
    public int getCurrentPosition() {
        if(musicService != null && musicBound && musicService.isPlaying()){
            return musicService.getPosition();

        }else if(playbackPaused){
            try{
                return musicService.getPosition();
            }catch (NullPointerException e){
                return 0;
            }
        }
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicService != null && musicBound){
           return musicService.isPlaying();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }
    @Override
    public boolean canPause() {
        return true;
    }
    @Override
    public boolean canSeekBackward() {
        return true;
    }
    @Override
    public boolean canSeekForward() {
        return true;
    }
    @Override
    public int getAudioSessionId() {
        return 0;
    }

    protected void sortListAlphabetically(){
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    @Override
    protected void onPause(){
        super.onPause();
        this.paused = true;
    }

    /**
     * Ensures the MusicController displays when the user returns to the app
     */
    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setMusicController();
            paused = false;
        }
    }

    /**
     * Hides the MusicController when user leaves the app
     */
    @Override
    protected void onStop(){
        musicController.hide();
        super.onStop();
    }

    protected void onDestroy() {
        if(musicBound){
            unbindService(musicConnection);
        }
        stopService(playIntent);
        musicService = null;
        super.onDestroy();
    }

    /** Displays a permission dialog when requested.
     * Depending on the user response, initialize the List of songs & display them or
     * Notify the user that permission to read phone data is necessary and close the app.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {

            // User accepts the permission(s).
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();

                SongAdapter songAdapter = new SongAdapter(this, songList);
                songView.setAdapter(songAdapter);

                // Manually passes the song list since the ServiceConnection instance was bind before the song list was formed.
                musicService.setSongList(songList);

                setMusicController();

                // If User denies the permission.
            } else {
                Toast.makeText(this, "Please grant permission and restart application.", Toast.LENGTH_SHORT).show();

                // Runs a thread for a slight delay prior to shutting down the app.
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            sleep(1500);
                            System.exit(0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }
        }
    }

    /**
     * Static method used to update the information about the current song being played.
     * This method is called by the Music Service once the previous song is finished playing.
     * @param currentSongInfo - String containing song Title & Artist.
     */
    public static void updateCurrentSongInfo(String currentSongInfo){
        currentSongView.setText(currentSongInfo);
    }

    /**
     * Ensure the application doesn't crash when user presses back key - instead go to home screen
     */
    @Override
    public void onBackPressed(){
        if(musicBound){
            Intent setIntent = new Intent(Intent.ACTION_MAIN);
            setIntent.addCategory(Intent.CATEGORY_HOME);
            setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(setIntent);
        }
    }
}
