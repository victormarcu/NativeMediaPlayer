package untitled.nativemediaplayer;

public class Song {
    private long id;
    private String title;
    private String artist;

    public Song(long songID, String songTitle, String songArtist){
        this.id = songID;
        this.title = songTitle;
        this.artist = songArtist;
    }

    public long getID(){
        return this.id;
    }

    public String getTitle(){
        return this.title;
    }

    public String getArtist(){
        return this.artist;
    }

    public String getSongInfo(){
        return this.title + " - "+ this.artist;
    }
}
