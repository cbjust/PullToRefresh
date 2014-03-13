package com.cb.refresh;

import com.cb.refresh.view.MyListView;
import com.cb.refresh.view.MyListView.MyListViewListener;
import com.pptv.refresh.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.ArrayAdapter;

public class MainActivity extends Activity {

	private static MyListView mListView;
	private String[] items = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mListView = (MyListView) findViewById(R.id.main_list);

		mListView.setSelector(new ColorDrawable(Color.TRANSPARENT));
		mListView.setPullRefreshEnable(true);
		mListView.setMyListViewListener(new MyListViewListener() {

			@Override
			public void onRefresh() {
				mHandler.sendEmptyMessageDelayed(0, 2000); // 这里只是简单让刷新状态保持5秒
			}
		});

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
		mListView.setAdapter(adapter);
	}

	private final static Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 0:
				mListView.stopRefresh();
				break;

			default:
				break;
			}
		}

	};
}
