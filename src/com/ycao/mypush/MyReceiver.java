package com.ycao.mypush;


import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import android.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import cn.jpush.android.api.JPushInterface;

/**
 * �Զ��������
 * 
 * ������������ Receiver����
 * 1) Ĭ���û����������
 * 2) ���ղ����Զ�����Ϣ
 */
public class MyReceiver extends BroadcastReceiver {
	private static final String TAG = "MyReceiver";
	public  final static String SER_KEY = "com.ycao.pushmessage";  
    public  final static String PAR_KEY = "com.ycao.pushmessage";  
	public void Alert(Context context,String msg){
		Toast toast = Toast.makeText(context,msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        LinearLayout tsv = (LinearLayout) toast.getView();
        ImageView iv = new ImageView(context);
        iv.setImageResource(R.drawable.ic_dialog_alert);
        tsv.addView(iv,0);
        toast.show();
		
	}
	@Override
	public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();	
        if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
            String regId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
            Log.d(TAG, "����Registration Id : " + regId);
            //send the Registration Id to your server...
        } else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
        	Log.d(TAG, "���յ������������Զ�����Ϣ: " + bundle.getString(JPushInterface.EXTRA_MESSAGE));
        	Bundle bundle1 = new Bundle();
           
    		String cmd =  bundle.getString("cn.jpush.android.MESSAGE");
			 Person mPerson = new Person();  
			 mPerson.setMethod("cmd");  
			 mPerson.setArgs(cmd);  
			 bundle1.putSerializable(SER_KEY,mPerson);
        	Intent i = new Intent(context, MyPushMain.class);
        	i.putExtras(bundle1);
        	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	
        	SharedPreferences preferences =  context.getSharedPreferences("mypush", Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("lastCmd",cmd);
			editor.commit();
			
        	context.startActivity(i);
        	
        	
     

        
        } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
            SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd	hh:mm:ss");     
            String   date   =   sDateFormat.format(new   java.util.Date()); 
            Log.d(TAG, "���յ�����������֪ͨ"+bundle.getString("cn.jpush.android.NOTIFICATION_CONTENT_TITLE"));

            SQLiteDatabase db = context.openOrCreateDatabase("mypush.db", Context.MODE_PRIVATE,null);
            db.execSQL("insert into mypush(content,datetime) values(?,?);",new Object[]{bundle.getString("cn.jpush.android.NOTIFICATION_CONTENT_TITLE"),date});
            
            db.close(); 
            
        	
        } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
            Log.d(TAG, "�û��������֪ͨ");
        	//���Զ����Activity
            Bundle bundle1 = new Bundle();
            StringBuilder sb = new StringBuilder();
    		for (String key : bundle.keySet()) {
    			sb.append("\nkey:" + key + ", value:" + bundle.getString(key));
    		}
    		String id = "";
    		try{
    			JSONObject jsonObject = new JSONObject(bundle.getString("cn.jpush.android.EXTRA")); 
    			id = jsonObject.getString("id");
    			
    		} catch (JSONException ex) { 
    			
    			
    		}
    		
    		sb.append("\n"+id);
            bundle1.putString("content", sb.toString());
            bundle1.putString("id", id);
            Person mPerson = new Person();  
            mPerson.setMethod("id");  
            mPerson.setArgs(id);  
            bundle1.putSerializable(SER_KEY,mPerson);
        	Intent i = new Intent(context, MyPushMain.class);
        	
        	i.putExtras(bundle1);
        	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	
        	SharedPreferences preferences =  context.getSharedPreferences("mypush", Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("lastId",id);
			editor.commit();
			
        	context.startActivity(i);
 	
        } else {
        	Log.d(TAG, "Unhandled intent - " + intent.getAction());
        }
	}
	
	// ��ӡ���е� intent extra ����
	public static String printBundle(Bundle bundle) {
		StringBuilder sb = new StringBuilder();
		for (String key : bundle.keySet()) {
			sb.append("\nkey:" + key + ", value:" + bundle.getString(key));
		}
		return sb.toString();
	}
	
	
    
     
}
