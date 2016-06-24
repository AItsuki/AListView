package com.aitsuki.alistview;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.aitsuki.alistview.widget.AListView;
import com.aitsuki.alistview.widget.OnLoadMoreListener;
import com.aitsuki.alistview.widget.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {

    private MainAdapter adapter;
    int page = 0;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final AListView listView = (AListView) findViewById(R.id.listView);
        random = new Random();

        listView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 三秒后刷新成功
                listView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean success = random.nextBoolean();
                        if (success) {
                            List<Integer> integers = requestData(0);
                            adapter.notifyDataChanged(integers, true);
                            listView.refreshComplete(true);
                            page = 1;
                            listView.setCanLoadMore(true);
                        } else {
                            listView.refreshComplete(false);
                        }
                    }
                }, 3000);
            }
        });

        listView.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                listView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean success = random.nextBoolean();
                        if (success) {
                            List<Integer> integers = requestData(page);
                            // 返回数据不够10条，已经是最后一页了, 禁用加载更多
                            if (integers == null || integers.size() < 10) {
                                listView.setCanLoadMore(false);
                                return;
                            }
                            adapter.notifyDataChanged(integers, false);
                            listView.loadMoreComplete(true);
                            page++;
                        } else {
                            listView.loadMoreComplete(false);
                        }

                    }
                }, 3000);
            }
        });

        adapter = new MainAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int item = (int) parent.getAdapter().getItem(position);
                Toast.makeText(MainActivity.this, "" + item, Toast.LENGTH_SHORT).show();
            }
        });

        listView.autoRefresh();
    }

    private List<Integer> requestData(int page) {
        List<Integer> data = new ArrayList<>();

        // 假设我们只有三页数据（测试loadMore）
        if (page < 3) {
            int start = page * 10;
            int end = start + 10;
            for (int i = start; i < end; i++) {
                data.add(i);
            }
        }
        return data;
    }


    class MainAdapter extends BaseAdapter {

        List<Integer> data = new ArrayList<>();

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Integer getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                TextView textView = new TextView(MainActivity.this);
                textView.setText(String.valueOf(getItem(position)));
                textView.setTextColor(Color.BLACK);
                textView.setBackgroundColor(0x55ff0000);
                textView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200));
                textView.setGravity(Gravity.CENTER);
                convertView = textView;
            } else {
                ((TextView) convertView).setText(String.valueOf(getItem(position)));
            }
            return convertView;
        }

        public void notifyDataChanged(List<Integer> data, boolean isRefresh) {
            if (isRefresh) {
                this.data.clear();
            }
            this.data.addAll(data);
            notifyDataSetChanged();
        }
    }
}
