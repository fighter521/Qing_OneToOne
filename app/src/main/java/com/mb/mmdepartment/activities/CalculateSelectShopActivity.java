package com.mb.mmdepartment.activities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.google.gson.Gson;
import com.mb.mmdepartment.adapter.MarketSelAdapter;
import com.mb.mmdepartment.base.TApplication;
import com.mb.mmdepartment.bean.lupinmodel.LuPinModel;
import com.mb.mmdepartment.tools.CustomToast;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.mb.mmdepartment.R;
import com.mb.mmdepartment.base.BaseActivity;
import com.mb.mmdepartment.bean.market_address.Description;
import com.mb.mmdepartment.bean.market_address.Root;
import com.mb.mmdepartment.biz.calculate.MarcketSelBiz;
import com.mb.mmdepartment.listener.OnRecycItemClickListener;
import com.mb.mmdepartment.listener.RequestListener;
import com.mb.mmdepartment.network.OkHttp;
import com.mb.mmdepartment.view.LoadingDialog;
import com.tencent.stat.StatService;
import com.umeng.analytics.MobclickAgent;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.jpush.android.api.JPushInterface;

/**
 * 帮你算超市选择界面
 */
public class CalculateSelectShopActivity extends BaseActivity implements RequestListener,OnRecycItemClickListener{
    private final String TAG = CalculateSelectShopActivity.class.getSimpleName();
    private RecyclerView recycle_market;
    private RecyclerView.Adapter adapter;
    private boolean[] isSel;
    private ArrayList<Description> list;
    private TextView tv_next;
    private MarcketSelBiz biz;
    private List<String> record;
    private GridLayoutManager manager;
    private LoadingDialog dialog;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    manager = new GridLayoutManager(CalculateSelectShopActivity.this, 2);
                    recycle_market.setLayoutManager(manager);
                    recycle_market.setAdapter(adapter);
                    break;
                case 1:
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    showToast("请求出错");
                    break;
            }
        }
    };

    /**
     *初始化加载view
     * @return
     */
    @Override
    public int getLayout() {
        return R.layout.activity_market_sel;
    }

    /**
     * 初始化参数
     * @param savedInstanceState
     */
    @Override
    public void init(Bundle savedInstanceState) {
        initView();
        setListener();
    }

    /**
     * 设置toolbar
     * @param action
     * @param isTrue
     */
    @Override
    protected void setToolBar(ActionBar action, boolean isTrue) {
        action.setTitle("超市选择");
        action.setHomeButtonEnabled(isTrue);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LuPingDestory("help_Accu_Shop", "page", "end", new Date());
        TApplication.activities.remove(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        JPushInterface.onPause(this);
        StatService.onPause(this);
    }

    /**
     * 设置监听器
     */
    private String shop_id="";
    private void setListener() {
        tv_next.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (record.isEmpty()) {
                    CustomToast.show(CalculateSelectShopActivity.this,"提示","请先选择超市");
                    return;
                }
                LuPing("btn_Accu_Shop_Next","other","next",new Date());
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < record.size(); i++) {
                    builder.append(record.get(i)+",");
                }
                shop_id=builder.toString();
                int last=shop_id.lastIndexOf(",");
                shop_id = shop_id.substring(0, last);
                startActivity(CalculateSelectShopActivity.this, CalculateSelectCategoryActivity.class, "shop_id", shop_id);
            }
        });
    }
    /**
     * 初始化参数
     */
    private void initView() {
        recycle_market= (RecyclerView) findViewById(R.id.recycle_market);
        tv_next=(TextView)findViewById(R.id.tv_next);
        record = new ArrayList<>();
        if (isNetworkConnected(this)) {
                biz = new MarcketSelBiz();
                biz.getMacketList(0,50,TAG,this);
        }else {
            showToast("网络无连接");
        }

    }
    @Override
    public void onBackPressed() {
        OkHttp.mOkHttpClient.cancel(TAG);
        super.onBackPressed();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                OkHttp.mOkHttpClient.cancel(TAG);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResponse(Response response) {
        if (response.isSuccessful()) {
            Gson gson = new Gson();
            try {
                String json = response.body().string();
                final Root root = gson.fromJson(json, Root.class);
                if ("0".equals(String.valueOf(root.getStatus()))) {
                    list = root.getData().getList();
                    isSel = new boolean[list.size()];
                    for (int i=0;i<list.size();i++){
                        isSel[i]=false;
                        TApplication.market.put(list.get(i).getShop_id(),list.get(i).getShop_name());
                    }
                    adapter = new MarketSelAdapter(list,this,isSel);
                    handler.sendEmptyMessage(0);
                } else {
                    handler.sendEmptyMessage(1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (isSel[position]){
            LuPing(list.get(position).getShop_id(), "shop", "unselected", new Date());
            record.remove(list.get(position).getShop_id());
            isSel[position]=false;
        }else {
            LuPing(list.get(position).getShop_id(),"shop","selected",new Date());
            record.add(list.get(position).getShop_id());
            isSel[position] = true;
        }
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onFailue(Request request, IOException e) {

    }
}
