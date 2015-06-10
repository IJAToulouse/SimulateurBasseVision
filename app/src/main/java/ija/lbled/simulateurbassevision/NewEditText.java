package ija.lbled.simulateurbassevision;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * Created by l.bled on 09/06/2015.
 */
public class NewEditText extends EditText {
    public NewEditText(Context context) {
        super(context);
    }

    public NewEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NewEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i("acuite", "BACK_ICI");
        }
        return false;
    }
}
