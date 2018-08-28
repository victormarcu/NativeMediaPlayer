package untitled.nativemediaplayer;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Random;

/**
 * Service class that handles Media playback with the help of MediaPlayer instance
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

//FFmpegMediaPlayer is a reimplementation of Android's MediaPlayer class.
//FFmpegMediaPlayer relies on FFmpeg and native code.
//  private FFmpegMediaPlayer ffmpegMusicPlayer;

    private MediaPlayer musicPlayer;
    private ArrayList<Song> songList;
    private int songPosition;
    private Deque<Integer> songStack;

    //Binder class
    private final IBinder musicBind = new MusicBinder();

    //used for shuffle;
    private boolean shuffle = false;
    private Random random = new Random();

    public void onCreate(){
        super.onCreate();
        this.songPosition = 0;
        songStack = new ArrayDeque<>();
        musicPlayer = new MediaPlayer();
        this.initMusicPlayer();
    }

    public void initMusicPlayer(){
        //wake lock allows music to continue playing when the device is idle.
        musicPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        musicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        //Set the methods in this class as listener for when the MediaPlayer instance is created/song is completed/error thrown
        musicPlayer.setOnPreparedListener(this);
        musicPlayer.setOnCompletionListener(this);
        musicPlayer.setOnErrorListener(this);
    }

    public void setSongList(ArrayList<Song> songs){
        this.songList = songs;
    }

    /**
     * Get the song selectable by current position in the list.
     * Get the ID of that song
     * Create URI for that song using the ID
     * Set the URI as Data Source for the media Player.
     * Prepare Data Source for play in the service class.
     */
    public void playSong(){
        musicPlayer.reset();

        Song songToBePlayed = songList.get(songPosition);
        String songTitle = songToBePlayed.getTitle();
        String songArtist = songToBePlayed.getArtist();
        long currentSongID = songToBePlayed.getID();
        Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSongID);

        //Set data source
        try{
            musicPlayer.setDataSource(MusicService.this, trackUri);
        }catch (Exception e){
            Log.e("MUSIC SERVICE: ", "ERROR setting data source.", e);
        }

        //Update the TextView that displays information about the current song being played
        MainActivity.updateCurrentSongInfo(this.songPosition+1 + ". " + songTitle + " - " +songArtist);

        //Call async method of the MediaPlayer to prepare the song
        musicPlayer.prepareAsync();
    }

    /**
     * Implementation for 3 Listeners of the MediaPlayer instance
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        MainActivity.showController();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(musicPlayer.getCurrentPosition() > 0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d("LOG_TAG:", "onError()");
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                musicPlayer.release();
                musicPlayer = new MediaPlayer();
                initMusicPlayer();
                return true;
            default:
                Log.d("MultiPlayer", "Error: " + what + "," + extra);
                musicPlayer.release();
                musicPlayer = new MediaPlayer();
                initMusicPlayer();
                break;
        }
        return false;
    }

    /**
     * Method that sets the song selected by the user using the songIndex
     * The index of the song selected by the user is also added to the stack of songs (this helps with keeping a sort of order of songs played - used for playPrevious).
     * @param songIndex - index of the song to be played in arrayList of songs.
     */
    public void setSong(int songIndex){
        this.songPosition = songIndex;
        enqueueSong(songIndex);
    }

    /**
     * Methods that apply standard playback control functions that the activity interfaces with.
     */
    public int getPosition(){
        return musicPlayer.getCurrentPosition();
    }

    public int getDuration(){
        return musicPlayer.getDuration();
    }

    public boolean isPlaying(){
        return musicPlayer.isPlaying();
    }

    public void pausePlayer(){
        musicPlayer.pause();
    }

    public void seek(int position){
        musicPlayer.seekTo(position);
    }
    public void go(){
        musicPlayer.start();
    }

    /**
     * Remove the top index from the stack and play the song at that index.
     * If the stack is empty, just play the previous song in the list.
     */
    public void playPrev(){
        if(!songStack.isEmpty()){
            songPosition = songStack.pop();
        }else {
            songPosition--;
            if (songPosition < 0) {
                songPosition = songList.size() - 1;
            }
        }
        playSong();
    }

    /**
     * If shuffle is active pick a new random song from the list, otherwise play the next song
     * Before playing the song, add the song index to the stack so that there's a reference to previously played songs.
     */
    public void playNext(){
        //pick a new song at random from the list (except the current song)
        //Could enhance functionality by using a queue of songs and preventing songs from being repeated until all songs have been played
        if(shuffle){
            int newSongIndex = songPosition;
            while(newSongIndex == songPosition){
                newSongIndex = random.nextInt(songList.size()-1);
            }
            songPosition = newSongIndex;
        }else{
            songPosition++;
            if(songPosition == songList.size()){
                songPosition = 0;
            };
        }

        enqueueSong(songPosition);
        playSong();
    }

    /**
     *  Simple method that adds the next songPosition to the stack of songs that have been already played.
     *  The stack is used to keep track of songs that have been played so far, which helps with playing previously played songs.
     * @param songPosition = index of the song that is about to be played
     */
    public void enqueueSong(int songPosition){
        if(!songStack.isEmpty()){
            if (songStack.peek() != songPosition) {
                songStack.push(songPosition);
            }
        }else{
            songStack.push(songPosition);
        }
    }

    //Update instance variable responsible with shuffle state.
    public void setShuffle(){
        if(shuffle) {
            shuffle = false;
        }else {
            shuffle = true;
        }
    }

    @Override
    public void onDestroy(){
        musicPlayer.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    //executed when the user quits the app.
    public boolean onUnbind(Intent intent){
        musicPlayer.stop();
        musicPlayer.release();
        return false;
    }

    /**
        Helper class that helps with the interaction between Service and activity
     */
    public class MusicBinder extends Binder{
        MusicService getService(){
            return MusicService.this;
        }
    }
}
