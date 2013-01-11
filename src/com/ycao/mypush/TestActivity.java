package com.ycao.mypush;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.text.Html;
import android.text.Html.ImageGetter;


public class TestActivity extends Activity {
	ImageGetter imgGetter = new Html.ImageGetter() {
		public Drawable getDrawable(String source) {
			Drawable drawable = null;
			drawable = Drawable.createFromPath(source);
			drawable.setBounds(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight());
			return drawable;
		}
	};
    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extra = new Bundle();  
        extra = getIntent().getExtras();  
        String id =  extra.getString("id"); 
        //???????��?List�??对�??��?�?RL�??
        List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("id", id));
      

        //对�??��???
        String param = URLEncodedUtils.format(params, "UTF-8");

        //baseUrl			
        String baseUrl = "http://citsm.sinaapp.com/getpush.php";

        //�?RL�???��???
        HttpGet getMethod = new HttpGet(baseUrl + "?" + param);
        			
        HttpClient httpClient = new DefaultHttpClient();

        try {
            HttpResponse response = httpClient.execute(getMethod); //??��GET请�?
            TextView tv = new TextView(this);
           
           
            
            //tv.setText(Html.fromHtml("<b>text3:</b><span style='color:red;'>dfff</span>"));
            tv.setText(Html.fromHtml(EntityUtils.toString(response.getEntity(), "utf-8"),imgGetter,null));
            
            
            addContentView(tv, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
           // Log.i(TAG, "resCode = " + response.getStatusLine().getStatusCode()); //?��??????
            //Log.i(TAG, "result = " + EntityUtils.toString(response.getEntity(), "utf-8"));//?��?????��?�??�?
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
       
    }

}
