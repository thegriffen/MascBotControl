package com.thegriffen.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class VerticleSwitchView extends View {

	private Paint outlinePaint, switchPaint;
	private boolean switchDown = true;
	private VerticleSwitchListener listener;

	public VerticleSwitchView(Context context) {
		super(context);
		initSwitch();
	}

	public VerticleSwitchView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initSwitch();
	}

	public VerticleSwitchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initSwitch();
	}

	public void initSwitch() {
		setFocusable(true);

		outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		outlinePaint.setColor(Color.BLACK);
		outlinePaint.setStrokeWidth(1);
		outlinePaint.setStyle(Paint.Style.FILL_AND_STROKE);

		switchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		switchPaint.setColor(Color.RED);
		switchPaint.setStrokeWidth(1);
		switchPaint.setStyle(Paint.Style.FILL_AND_STROKE);
	}
	
	public void setOnSwitchedListener(VerticleSwitchListener listener) {
		this.listener = listener;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int height = getMeasuredHeight();
		int width = getMeasuredWidth();

		canvas.drawRect(0, 0, width, height, outlinePaint);
		
		if(switchDown) {
			canvas.drawRect(0, height / 2, width, height, switchPaint);
		}
		else {
			canvas.drawRect(0, 0, width, height / 2, switchPaint);
		}
		
		canvas.save();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int actionType = event.getAction();
		if(actionType == MotionEvent.ACTION_UP) {
			switchDown = !switchDown;
			if(listener != null) {
				listener.OnSwitched(switchDown);
			}
		}
		invalidate();
		return true;
	}

}
