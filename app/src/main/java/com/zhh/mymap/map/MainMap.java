package com.zhh.mymap.map;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapBaseIndoorMapInfo;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.zhh.mymap.Application;
import com.zhh.mymap.R;
import com.zhh.mymap.indoorview.BaseStripAdapter;
import com.zhh.mymap.indoorview.StripListView;
import com.zhh.mymap.service.LocationService;

public class MainMap extends AppCompatActivity implements SensorEventListener {
    public class SDKReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
        }
    }
    private BroadcastReceiver mReceiver;
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LocationService locationService;
    private StripListView stripListView;
    private BaseStripAdapter mFloorListAdapter;
    private MapBaseIndoorMapInfo mMapBaseIndoorMapInfo = null;
    private BottomSheetBehavior mBottomSheetBehavior1;
    private View bottomSheet;
    private LinearLayout tapactionlayout;
    private boolean isSatellite = false;
    private boolean isTraffic = false;
    private boolean isHeatMap = false;
    private boolean isIndoor = true;
    private ImageView Satellite;
    private ImageView Thermo;
    private ImageView Traffic;
    private ImageView Indoor;
    private String ID = null;

    private SensorManager mSensorManager;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private boolean isFirstLocate = true;
    // UI相关
    private MyLocationData locData;
    private MyLocationConfiguration.LocationMode mCurrentMode;
    private FloatingActionButton requestLocButton;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmap);
        // 注册 SDK 广播监听者
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK);
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new MainMap.SDKReceiver();
        registerReceiver(mReceiver, iFilter);
        mMapView =  findViewById(R.id.bmapView);
        mMapView =  findViewById(R.id.bmapView);
        Satellite=findViewById(R.id.satellite);
        Thermo=findViewById(R.id.baiduHeatMap);
        Traffic = findViewById(R.id.traffic);
        Indoor=findViewById(R.id.indoor);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        /* 因为室内图支持最大缩放级别为22.0f，要做特殊处理，所以要先打开室内图开关，然后做地图刷新，以防设置
         * 缩放级别为22.0f时不生效
         */
        mBaiduMap.setIndoorEnable(true);
        stripListView = new StripListView(this);
        mFloorListAdapter = new BaseStripAdapter(MainMap.this);
        mBaiduMap.setOnBaseIndoorMapListener(new BaiduMap.OnBaseIndoorMapListener() {
            @Override
            public void onBaseIndoorMapMode(boolean b, MapBaseIndoorMapInfo mapBaseIndoorMapInfo) {
                if (b == false || mapBaseIndoorMapInfo == null) {
                    stripListView.setVisibility(View.INVISIBLE);
                    return;
                }
                if(stripListView.getVisibility()==View.VISIBLE&&ID!=null&&ID.equals(mapBaseIndoorMapInfo.getID()))
                    return;
                ID = mapBaseIndoorMapInfo.getID();
                mFloorListAdapter.setmFloorList(mapBaseIndoorMapInfo.getFloors());
                stripListView.setVisibility(View.VISIBLE);
                stripListView.setStripAdapter(mFloorListAdapter);
                mMapBaseIndoorMapInfo = mapBaseIndoorMapInfo;
            }
        });
        stripListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (mMapBaseIndoorMapInfo == null) {
                    return;
                }
                String floor = (String) mFloorListAdapter.getItem(position);
                mBaiduMap.switchBaseIndoorMapFloor(floor, mMapBaseIndoorMapInfo.getID());
                mFloorListAdapter.setSelectedPostion(position);
                mFloorListAdapter.notifyDataSetInvalidated();
            }
        });
        CoordinatorLayout layout = findViewById(R.id.mLayout);
        layout.addView(stripListView);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
        getSupportActionBar().setTitle("Hello World~");
        //浮动框
        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(locData==null) return;
                        LatLng latLng = new LatLng(locData.latitude, locData.longitude);
                        MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(latLng);
                        mBaiduMap.animateMapStatus(update);
                    }
                });
        bottomSheet = findViewById(R.id.bottom_sheet1);
        tapactionlayout = findViewById(R.id.tap_action_layout);
        mBottomSheetBehavior1 = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior1.setPeekHeight(120);
        mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mBottomSheetBehavior1.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    //收起
                    tapactionlayout.setVisibility(View.VISIBLE);
                }
                else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    //展开
                    tapactionlayout.setVisibility(View.GONE);
                }
                else if (newState == BottomSheetBehavior.STATE_SETTLING) {
                    //下滑
                    tapactionlayout.setVisibility(View.GONE);
                }
                else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    //拖动
                    tapactionlayout.setVisibility(View.GONE);
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        tapactionlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBottomSheetBehavior1.getState()==BottomSheetBehavior.STATE_COLLAPSED)
                {
                    mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });
        //更改百度地图里logo和缩放按键的位置
        mMapView.showZoomControls(true);//设置是否显示缩放控件
        mMapView.getChildAt(2).setPadding(0,0,50,450);//这是控制缩放控件的位置
        mMapView.removeViewAt(1);//删除logo
        //mMapView.getChildAt(1).setPadding(10,0,0,0);//这是控制logo的位置

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//获取传感器管理服务
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;


        requestLocButton = findViewById(R.id.fab);
        requestLocButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mCurrentMode) {
                    case NORMAL:
                        requestLocButton.setImageResource(R.drawable.following);
                        mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                        mBaiduMap
                                .setMyLocationConfiguration(new MyLocationConfiguration(
                                        mCurrentMode, true, null));
                        MapStatus.Builder builder = new MapStatus.Builder();
                        builder.overlook(0);
                        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                        break;
                    case COMPASS:
                        requestLocButton.setImageResource(R.drawable.ic_my_location_black_24dp);
                        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
                        mBaiduMap
                                .setMyLocationConfiguration(new MyLocationConfiguration(
                                        mCurrentMode, true, null));
                        MapStatus.Builder builder1 = new MapStatus.Builder();
                        builder1.overlook(0);
                        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder1.build()));
                        break;
                    case FOLLOWING:
                        requestLocButton.setImageResource(R.drawable.compass);
                        mCurrentMode = MyLocationConfiguration.LocationMode.COMPASS;
                        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                                        mCurrentMode, true, null));
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * 设置是否显示卫星图
     */
    public void setMapMode(View view) {
        isSatellite = !isSatellite;
        if(isSatellite)
        {
            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            Satellite.setImageResource(R.drawable.satellite_clicked);
        }
        else{
            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            Satellite.setImageResource(R.drawable.satellite);
        }
        mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /**
     * 设置是否显示交通图
     */
    public void setTraffic(View view) {
        isTraffic = !isTraffic;
        mBaiduMap.setTrafficEnabled(isTraffic);
        mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_COLLAPSED);
        if(isTraffic) Traffic.setImageResource(R.drawable.traffic_clicked);
        else Traffic.setImageResource(R.drawable.traffic);
    }

    /**
     * 设置是否显示百度热力图
     */
    public void setBaiduHeatMap(View view) {
        isHeatMap = !isHeatMap;
        mBaiduMap.setBaiduHeatMapEnabled(isHeatMap);
        mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_COLLAPSED);
        if(isHeatMap) Thermo.setImageResource(R.drawable.thermodynamic_clicked);
        else Thermo.setImageResource(R.drawable.thermodynamic);
    }

    /**
     * 设置是否显示室内图
     */
    public void setIndoor(View view) {
        isIndoor = !isIndoor;
        if(isIndoor)
        {
            mBaiduMap.setIndoorEnable(true);
            Indoor.setImageResource(R.drawable.indoor_clicked);
        }
        else
        {
            mBaiduMap.clear();
            mBaiduMap.setIndoorEnable(false);
            Indoor.setImageResource(R.drawable.indoor);
        }
        mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    protected void onPause() {
        // MapView的生命周期与Activity同步，当activity挂起时需调用MapView.onPause()
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        // MapView的生命周期与Activity同步，当activity恢复时需调用MapView.onResume()
        mMapView.onResume();
        //为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // MapView的生命周期与Activity同步，当activity销毁时需调用MapView.destroy()
        mMapView.onDestroy();
        // 取消监听 SDK 广播
        unregisterReceiver(mReceiver);
        mBaiduMap.setMyLocationEnabled(false);
        mMapView = null;
        super.onDestroy();
    }
    @Override
    protected void onStop() {
        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务
        //取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }
    @Override
    protected void onStart() {
        super.onStart();
        // -----------location config ------------
        locationService = ((Application) getApplication()).locationService;
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService.registerListener(mListener);
        //注册监听
        locationService.setLocationOption(locationService.getDefaultLocationClientOption());
        locationService.start();
    }
    private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || mMapView == null)
                return;
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLocate) {
                mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
                mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                                null, true, null));
                MapStatus.Builder builder1 = new MapStatus.Builder();
                builder1.overlook(0);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder1.build()));
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(update);
                update = MapStatusUpdateFactory.zoomTo(16f);
                mBaiduMap.animateMapStatus(update);
                isFirstLocate = false;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setIconifiedByDefault(true);//默认为true在框内，设置false则在框外
        searchView.setQueryHint("在这里搜索");//设置默认无内容时的文字提示
        SearchView.SearchAutoComplete mSearchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(R.id.search_src_text);
        //设置输入框提示文字样式
        mSearchAutoComplete.setHintTextColor(getResources().getColor(android.R.color.black));//设置提示文字颜色
        mSearchAutoComplete.setTextColor(getResources().getColor(android.R.color.black));//设置内容文字颜色
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];
        if(locData==null) return;
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(locData.accuracy)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(locData.latitude)
                    .longitude(locData.longitude).build();
            mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * 打开离线下载地图
     */
    public void openOffline(View view) {
        Intent intent = new Intent(MainMap.this, OfflineDemo.class);
        startActivity(intent);
        mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

}
