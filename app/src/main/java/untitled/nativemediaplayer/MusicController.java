package untitled.nativemediaplayer;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.MediaController;

/**
 * A subclass of {@link MediaController} that presents a widget with song functionality including
 * play/pause, fast-forward/rewind, and etc. The widget also contains a seek bar, which updates as
 * the song plays and contains text indicating the duration of the song and the player's current
 * position.
 */
public class MusicController extends MediaController {
    Context context;

    public MusicController(Context context) {
        super(context);
        this.context = context;
    }

    public void hide(){

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if(keyCode == KeyEvent.KEYCODE_BACK){
            ((MainActivity)context).onBackPressed();
            Log.d("CDA", "onKeyDown Called");
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

}
