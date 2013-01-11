package com.ycao.mypush;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

public class SplashActivity extends Activity {

	private final int SPLASH_DISPLAY_LENGHT = 1000; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

		new Handler().postDelayed(new Runnable() {
			public void run() {
				Intent mainIntent = new Intent(SplashActivity.this,
						MyPushMain.class);
				SplashActivity.this.startActivity(mainIntent);
				SplashActivity.this.finish();
				//overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);      

				overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
				}

		}, SPLASH_DISPLAY_LENGHT);

	}
}
