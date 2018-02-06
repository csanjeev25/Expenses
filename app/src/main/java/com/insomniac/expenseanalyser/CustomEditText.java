package com.insomniac.expenseanalyser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Sanjeev on 2/6/2018.
 */

public class CustomEditText extends AppCompatEditText{

    private static final int DEFAULT_CLEAR_ICON = android.R.drawable.ic_menu_close_clear_cancel;
    private final Rect bounds = new Rect();
    private Drawable clearDrawable;
    private boolean showClearButton = false;
    private ClearTextListener mClearTextListener;

    public void setClearTextListener(ClearTextListener listener){
        mClearTextListener = listener;
    }

    public CustomEditText(Context context){
        super(context);
        init(context,null,0);
    }

    public CustomEditText(Context context, AttributeSet attributeSet){
        super(context,attributeSet);
        init(context,attributeSet,0);
    }

    public CustomEditText(Context context,AttributeSet attributeSet,int defStyleAttr){
        super(context,attributeSet,defStyleAttr);
        init(context,attributeSet,defStyleAttr);
    }

    private void init(Context context,AttributeSet attributeSet,int defStyleAttr){
        int resourceID = context.getResources().getIdentifier("custom_edit_text_icon","drawable",context.getPackageName());
        if(resourceID == 0)
            resourceID = DEFAULT_CLEAR_ICON;

        clearDrawable = ContextCompat.getDrawable(context,resourceID);
        setPadding(getPaddingLeft(),getPaddingTop(),getPaddingRight() + clearDrawable.getIntrinsicWidth(),getPaddingBottom());
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                showClearButton = !charSequence.toString().isEmpty();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(bounds);
        if(showClearButton){
            bounds.left = bounds.right - clearDrawable.getIntrinsicWidth();
            bounds.top = bounds.bottom - (clearDrawable.getIntrinsicHeight())/2;
            bounds.bottom = bounds.top + clearDrawable.getIntrinsicHeight();
            clearDrawable.setBounds(bounds);
            clearDrawable.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(showClearButton){
            int x = (int) event.getX();
            int y = (int) event.getY();
            int left = getWidth() - getPaddingRight() - clearDrawable.getIntrinsicWidth();
            int right = getWidth();
            boolean tappedX = x >= left && x <= right && y >= 0 && y <= (getBottom() - getTop());

            if(tappedX){
                if(event.getAction() == MotionEvent.ACTION_UP){
                    setText("");
                    showClearButton = false;
                    requestFocus();
                    if(mClearTextListener != null)
                        mClearTextListener.onTextCleared(this);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    public interface ClearTextListener{
        void onTextCleared(CustomEditText customEditText);
    }


}
