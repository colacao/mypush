package com.ycao.mypush;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;



import cn.jpush.android.api.JPushInterface;

import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.api.AccountAPI;
import com.weibo.sdk.android.api.UsersAPI;
import com.weibo.sdk.android.keep.AccessTokenKeeper;
import com.weibo.sdk.android.net.RequestListener;
import com.ycao.mypush.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.telephony.SmsManager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.webkit.WebSettings.ZoomDensity;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter.ViewBinder;

public class MyPushMain extends Activity implements OnClickListener{
	private String TAG = "mypush";
	public SharedPreferences sp = null;
	public ArrayList<View> views = null;
	private Weibo mWeibo;
	private ViewPager mViewPager;
	public View view1 = null;
	public View view2 = null;
	public View view3 = null;
	public View view4 = null;
	private ImageView cursor;// 动画图片
	private TextView t1, t2, t3, t4;// 页卡头标
	private int offset = 0;// 动画图片偏移量
	private int currIndex = 0;// 当前页卡编号
	private int bmpW;// 动画图片宽度
	public WebView webview = null;
	public DropDownToRefreshListView listView = null;
	public SimpleAdapter adapter11 = null;
	public ArrayList<HashMap<String, Object>> weibolist = new ArrayList<HashMap<String, Object>>();
	public int currentPage = 1;
	private ImageFileCache fileCache;
	public String lastImg = "";
	private static final String CONSUMER_KEY = "929887641";
	private static final String REDIRECT_URL = "http://citsm.sinaapp.com/mypushadd.php";
	public static Oauth2AccessToken accessToken;
	ImageGetter imgGetter = new Html.ImageGetter() {
		public Drawable getDrawable(String source) {
			Drawable d = null;
			Bitmap b = null;
			try {
				b = fileCache.getImage(source);
				if (b != null) {
					
					Drawable drawable = new BitmapDrawable(b);
					// Log.i(TAG, "asdfasdfasdfsadfsdf");
					d = drawable;

				} else {

					URL aryURI = new URL(source);
					URLConnection conn = aryURI.openConnection();
					conn.connect();
					int lenghtOfFile = conn.getContentLength();
					InputStream input = new BufferedInputStream(aryURI.openStream());
					OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/lualu/" + lastImg);

					byte data[] = new byte[1024];
					long total = 0;
					int count = 0;
					while ((count = input.read(data)) != -1) {
						total += count;

//						if ((int) ((total * 100) / lenghtOfFile) > 99) {
//							Timer t = new Timer();
//							t.schedule(new MyTask2(2), 500);
//						}
						output.write(data, 0, count);
					}
					output.flush();
					output.close();
					input.close();
					InputStream is = conn.getInputStream();
					d = Drawable.createFromStream(is, "");
					is.close();
				}
				// if(d.getIntrinsicWidth()<getWindowManager().getDefaultDisplay().getWidth()){
				int fixA = getWindowManager().getDefaultDisplay().getHeight() / d.getIntrinsicHeight();
				int fixB = d.getIntrinsicHeight() / getWindowManager().getDefaultDisplay().getHeight();
				int fixC = fixA > fixB ? fixA : fixB;
				d.setBounds(0, 0, getWindowManager().getDefaultDisplay().getWidth(), (d.getIntrinsicHeight() * fixC * 10) / 10);
				// }else{
				// d.setBounds(0, 0,
				// getWindowManager().getDefaultDisplay().getWidth(),
				// getWindowManager().getDefaultDisplay().getHeight());
				// }
			} catch (IOException e) {
				// e.printStackTrace();
			}

			return d;
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		setContentView(R.layout.activity_my_push_main);
		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);
		Log.i(TAG,"onCreate");
		Resources res = getResources();
		Drawable drawable = res.getDrawable(R.drawable.bkcolor);
		this.getWindow().setBackgroundDrawable(drawable);
		
		init();
	}
	public void init(){
		initViewPage();
		initList();
		
		initCache();
		
		initTagName();
		initWebView();
	}
	private void initTagName(){
		String tagName = sp.getString("tagName", "");
		Log.i(TAG,tagName);
		String[] sArray = tagName.split(",");
		if(sArray.length>0){
			Set<String> tagSet = new HashSet<String>();
			for (String sTagItme : sArray) {
				if (!ExampleUtil.isValidTagAndAlias(sTagItme)) {
					return;
				}
				tagSet.add(sTagItme);
			}
			JPushInterface.setAliasAndTags(getApplicationContext(), null, tagSet);
		}
		
	}
	private void initCache(){
		fileCache = new ImageFileCache();
		sp = getSharedPreferences("mypush", MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("lastId", "");
		editor.putString("lastCmd", "");
		editor.commit();
	}
	private void initWebView(){
		
		webview = (WebView) view3.findViewById(R.id.webview);
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setSupportZoom(true);
		webview.getSettings().setDefaultZoom(ZoomDensity.CLOSE);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setDefaultTextEncodingName("utf-8");
		webview.setBackgroundColor(0);
		webview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
	}
	public void initData() throws JSONException{
		listView.onRefreshBegin();
		weibolist.clear();
		SQLiteDatabase db = openOrCreateDatabase("mypush.db", Context.MODE_PRIVATE,null);
		db.execSQL("create table if not exists mypush (id integer primary key autoincrement,content text,datetime text);");
		Cursor cursor = db.rawQuery("select * from mypush",null);
		while (cursor.moveToNext()) {
			int _id = cursor.getInt(0); 
			String content = cursor.getString(1);
			String datetime = cursor.getString(2);
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("title", content);
			weibolist.add(map);
		}
		cursor.close();
		db.close(); 

		
		view1.postDelayed(new Runnable() {
			public void run() {
				adapter11.notifyDataSetChanged();
				listView.onRefreshComplete();
				
			}
		}, 1000);
		
		
	}
	public void Alert(Context context, String msg) {
		Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		LinearLayout tsv = (LinearLayout) toast.getView();
		ImageView iv = new ImageView(context);
		iv.setImageResource(R.drawable.adsf);
		tsv.addView(iv, 0);
		toast.show();
	}

	public void CallPhone(String phone) {
		Alert(getApplicationContext(), "CallPhone-" + phone);
		String phonenum = phone;
		// Log.i("phone", phone);
		Intent intent = new Intent("android.intent.action.CALL", Uri.parse("tel:" + phonenum));
		startActivity(intent);
	}

	public void SendSMS(String phone, String msg) {
		Alert(getApplicationContext(), "SendSMS-" + phone + "-" + msg);
		String str_num = phone;
		String str_content = msg;
		SmsManager manager_sms = SmsManager.getDefault();

		ArrayList<String> texts = manager_sms.divideMessage(str_content);
		for (String text : texts) {
			manager_sms.sendTextMessage(str_num, null, text, null, null);
		}

	}
	private Handler handler2 = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 900:
				try {
					String str = (String) msg.obj;
					JSONArray jarr = new JSONObject(str).getJSONArray("statuses");
					for (int i = 0; i < jarr.length(); i++) {
						HashMap<String, Object> map = new HashMap<String, Object>();
						JSONObject jobj = (JSONObject) jarr.opt(i);
						JSONObject uobj = jobj.getJSONObject("user");
						String rstr = uobj.getString("screen_name") + ":" + jobj.getString("text");
						try {
							JSONObject robj = jobj.getJSONObject("retweeted_status");
							JSONObject ruser = null;
							if (!robj.equals(null)) {
								ruser = robj.getJSONObject("user");
								rstr = rstr + "<br/><font color='#999999'>&nbsp;&nbsp;&nbsp;&nbsp;"
										+ (ruser != null ? ruser.getString("screen_name") + robj.getString("text") : "") + "</font>";
								// Log.i(TAG, rstr);
							}
						} catch (JSONException e) {

						}
						map.put("title", rstr + "<br/><br/>来自:<font color='#EA8B2F' size='1'>" + jobj.getString("source") + "</font>");
						@SuppressWarnings("deprecation")
						Drawable drawable = new BitmapDrawable(getBitmap(uobj.getString("profile_image_url")));
						map.put("img", drawable);
						weibolist.add(map);
					}
					adapter11.notifyDataSetChanged();
					listView.onRefreshComplete();
				} catch (JSONException e) {

				}
				;
				break;
			case 100:
				String str = (String) msg.obj;
				Alert(getApplicationContext(), str);
				break;
			case 101:
				String str2 = (String) msg.obj;
				TextView tv = (TextView) view3.findViewById(R.id.txtView);
				webview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
				tv.setText(Html.fromHtml("<img  src=\"" + str2 + "\"/>", imgGetter, null));
				break;
			}

		}
	};
	private class MyTask2 extends TimerTask {
		@Override
		public void run() {
			Message message = new Message();
			message.what = this.what;
			message.obj = this.arg;
			handler2.sendMessage(message);
		}

		private int what = 2;
		private String arg = "";

		public MyTask2(int what, String... args) {
			this.what = what;
			if (args.length > 0) {
				// Log.i(TAG, args[0]);
				arg = args[0];
			}
		}

	}
	public Bitmap getBitmap(String imageUrl) {
		Bitmap mBitmap = null;
		try {
			URL url = new URL(imageUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();
			mBitmap = BitmapFactory.decodeStream(is);

		} catch (MalformedURLException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			// e.printStackTrace();
		}
		return mBitmap;
	}
	public void initList(){
		listView = (DropDownToRefreshListView) view1.findViewById(R.id.mylistview);
		try {
			initData();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		adapter11 = new SimpleAdapter(this, weibolist, R.layout.lv, new String[] { "img", "title" }, new int[] { R.id.img, R.id.title });
		adapter11.setViewBinder(new ViewBinder() {
			public boolean setViewValue(View view, Object data, String textRepresentation) {
				if (data instanceof String) {
					TextView tv = (TextView) view;
					tv.setText(Html.fromHtml(textRepresentation, imgGetter, null));
					return true;
				}
				return false;
			}
		});
		listView.setAdapter(adapter11);
		listView.setOnScrollListener(new OnScrollListener() {
			public void onScrollStateChanged(AbsListView view, int scrollState) {
//				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
//					if (view.getLastVisiblePosition() == view.getCount() - 1) {
//
//						currentPage++;
//						try {
//							initData();
//						} catch (JSONException e) {
//							// TODO Auto-generated catch block
//							//e.printStackTrace();
//						}
//
//					}
//				}
			}

			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

			}
		});

		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				HashMap<String, Object> map = (HashMap<String, Object>) weibolist.get(arg2 - 1);
				TextView tv = (TextView) view3.findViewById(R.id.txtView);
				tv.setText("");
				webview.loadDataWithBaseURL(null, map.get("title").toString(), "text/html", "utf-8", null);
				mViewPager.setCurrentItem(2);
			}

		});
		listView.setOnRefreshListener(new DropDownToRefreshListView.OnRefreshListener() {
			public void onRefresh() {
				try {
					initData();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
		});
	}
	public void initViewPage() {

		mViewPager = (ViewPager) findViewById(R.id.viewpager);

		LayoutInflater mLi = LayoutInflater.from(this);
		view1 = mLi.inflate(R.layout.lay1, null);
		//view2 = mLi.inflate(R.layout.lay2, null);
		view3 = mLi.inflate(R.layout.lay3, null);
		//view4 = mLi.inflate(R.layout.lay4, null);

		views = new ArrayList<View>();
		views.add(view1);
		//views.add(view2);
		views.add(view3);
		//views.add(view4);

		t1 = (TextView) findViewById(R.id.text1);
		//t2 = (TextView) findViewById(R.id.text2);
		t3 = (TextView) findViewById(R.id.text3);
		//t4 = (TextView) findViewById(R.id.text4);

		t1.setOnClickListener(new MyOnClickListener(0));
		//t2.setOnClickListener(new MyOnClickListener(1));
		t3.setOnClickListener(new MyOnClickListener(2));
//		t4.setOnClickListener(new MyOnClickListener(3));

		PagerAdapter mPagerAdapter = new PagerAdapter() {
			public boolean isViewFromObject(View arg0, Object arg1) {
				return arg0 == arg1;
			}

			public int getCount() {
				return views.size();
			}

			public void destroyItem(View container, int position, Object object) {
				((ViewPager) container).removeView(views.get(position));
			}

			public Object instantiateItem(View container, int position) {
				((ViewPager) container).addView(views.get(position));
				return views.get(position);
			}
		};
		mViewPager.setAdapter(mPagerAdapter);

		InitImageView();
		mViewPager.setOnPageChangeListener(new MyOnPageChangeListener());

	}
	public class MyOnPageChangeListener implements OnPageChangeListener {
		int one = offset * 2 + bmpW;
		int two = one * 2;
		int three = one * 3;

		public void onPageSelected(int arg0) {
			Animation animation = null;

			switch (arg0) {
			case 0:
				if (currIndex == 1) {
					animation = new TranslateAnimation(one, 0, 0, 0);
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, 0, 0, 0);
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, 0, 0, 0);
				}
				break;
			case 1:
				if (currIndex == 0) {
					animation = new TranslateAnimation(offset, one, 0, 0);
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, one, 0, 0);

				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, one, 0, 0);

				}
				break;
			case 2:
				if (currIndex == 0) {
					animation = new TranslateAnimation(one, two, 0, 0);
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, two, 0, 0);
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, three, 0, 0);
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, two, 0, 0);
				}
				break;
			case 3:
				if (currIndex == 0) {
					animation = new TranslateAnimation(one, three, 0, 0);

				} else if (currIndex == 1) {
					animation = new TranslateAnimation(two, three, 0, 0);
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, three, 0, 0);

				}
				break;
			}
			currIndex = arg0;
			animation.setFillAfter(true);
			animation.setDuration(150);
			cursor.startAnimation(animation);
		}

		public void onPageScrolled(int arg0, float arg1, int arg2) {
			// Log.i(TAG, arg0 + ":"+arg1+":"+arg2);
		}

		public void onPageScrollStateChanged(int arg0) {
			// Log.i("onPageScrollStateChanged", arg0+"");
		}
	}
	private void InitImageView() {
		cursor = (ImageView) findViewById(R.id.cursor);
		bmpW = BitmapFactory.decodeResource(getResources(), R.drawable.a).getWidth();
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;
		offset = (screenW / 2 - bmpW) / 2;
		Matrix matrix = new Matrix();
		matrix.postTranslate(offset, 0);
		cursor.setImageMatrix(matrix);
	}
	public class MyOnClickListener implements View.OnClickListener {
		private int index = 0;

		public MyOnClickListener(int i) {
			index = i;
		}

		public void onClick(View v) {
			mViewPager.setCurrentItem(index);
		}
	};
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.add(Menu.NONE, Menu.FIRST + 1, 1, "绑定新浪微博").setIcon(

		android.R.drawable.ic_menu_add);

		menu.add(Menu.NONE, Menu.FIRST + 2, 2, "自推").setIcon(

		android.R.drawable.ic_menu_help);
		
		menu.add(Menu.NONE, Menu.FIRST + 3, 4, "清空").setIcon(
		android.R.drawable.ic_menu_delete);

		return true;

	}
	private void clearDb(){
		weibolist.clear();
		SQLiteDatabase db = openOrCreateDatabase("mypush.db", Context.MODE_PRIVATE,null);
		db.execSQL("delete from mypush;");

		db.close(); 
	}
	private void initSinaWeibo() {
		// Log.i(TAG, "initWeibo");
		mWeibo = Weibo.getInstance(CONSUMER_KEY, REDIRECT_URL);
		mWeibo.authorize(MyPushMain.this, new AuthDialogListener());

	}

	public class AuthDialogListener implements WeiboAuthListener {
		public void onComplete(Bundle values) {
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");
			MyPushMain.accessToken = new Oauth2AccessToken(token, expires_in);
			if (MyPushMain.accessToken.isSessionValid()) {

				SharedPreferences.Editor editor = sp.edit();
				editor.putString("token", token);
				editor.putString("expires_in", expires_in);
				editor.commit();

				AccessTokenKeeper.keepAccessToken(MyPushMain.this, accessToken);

				AccountAPI aca = new AccountAPI(MyPushMain.accessToken);
				aca.getUid(new RequestListener() {
					public void onComplete(String arg0) {
						try {
							JSONTokener jsonParser = new JSONTokener(arg0);
							JSONObject person = (JSONObject) jsonParser.nextValue();
							String uid = person.getString("uid");
							UsersAPI ua = new UsersAPI(MyPushMain.accessToken);
							ua.show(Long.parseLong(uid), new RequestListener() {
								public void onComplete(String arg0) {
									Log.i(TAG, arg0);
									try {
										JSONTokener jsonParser = new JSONTokener(arg0);

										JSONObject person = (JSONObject) jsonParser.nextValue();
										String strname = person.getString("name");
										String[] sArray = ("android,all," + strname).split(",");
										Set<String> tagSet = new HashSet<String>();
										for (String sTagItme : sArray) {
											if (!ExampleUtil.isValidTagAndAlias(sTagItme)) {
												return;
											}
											tagSet.add(sTagItme);
										}
										JPushInterface.setAliasAndTags(getApplicationContext(), null, tagSet);
										initData();
										SharedPreferences.Editor editor = sp.edit();
										editor.putString("tagName", ("android,all," + strname));
										editor.commit();
										Log.i(TAG,"bindook");
									} catch (JSONException e) {

									}
								}

								public void onError(WeiboException arg0) {
								}

								public void onIOException(IOException arg0) {
								}

							});

						} catch (JSONException e) {

						}
					}

					public void onError(WeiboException arg0) {
					}

					public void onIOException(IOException arg0) {
					}
				});

			}
		}

		public void onError(WeiboDialogError e) {
			Toast.makeText(getApplicationContext(), "Auth error : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}

		public void onCancel() {
			Toast.makeText(getApplicationContext(), "Auth cancel", Toast.LENGTH_LONG).show();
		}

		public void onWeiboException(WeiboException e) {
			Toast.makeText(getApplicationContext(), "Auth exception : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}

	}
	public void sendPush(String str) {
		Log.i(TAG,str);
		List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
		String tagName = sp.getString("tagName", "");
		String[] sArray = tagName.split(",");
		Log.i(TAG, sArray[sArray.length - 1]);
		params.add(new BasicNameValuePair("push", sArray[sArray.length - 1]));
		params.add(new BasicNameValuePair("msg", str));
		String param = URLEncodedUtils.format(params, "UTF-8");
		String baseUrl = "http://citsm.sinaapp.com/push.php";
		HttpGet getMethod = new HttpGet(baseUrl + "?" + param);
		HttpClient httpClient = new DefaultHttpClient();
		try {
			httpClient.execute(getMethod);
		} catch (ClientProtocolException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case Menu.FIRST + 1:
			initSinaWeibo();
			// Toast.makeText(this, "删除菜单被点击了", Toast.LENGTH_LONG).show();

			break;

		case Menu.FIRST + 2:
			String tagName = sp.getString("tagName", "");
			String[] sArray = tagName.split(",");
			sendPush("你好："+sArray[sArray.length - 1]+",欢迎使用我的推送");

			break;
		case Menu.FIRST + 3:
			clearDb();
			try {
				initData();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
	
		}
		return false;

	}
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

}
