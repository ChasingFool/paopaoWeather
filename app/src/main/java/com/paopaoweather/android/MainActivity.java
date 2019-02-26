package com.paopaoweather.android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
//import com.baidu.mapapi.SDKInitializer;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import interfaces.heweather.com.interfacesmodule.bean.Lang;
import interfaces.heweather.com.interfacesmodule.bean.Unit;
import interfaces.heweather.com.interfacesmodule.bean.air.forecast.AirForecast;
import interfaces.heweather.com.interfacesmodule.bean.air.forecast.AirForecastBase;
import interfaces.heweather.com.interfacesmodule.bean.air.now.AirNow;
import interfaces.heweather.com.interfacesmodule.bean.weather.forecast.Forecast;
import interfaces.heweather.com.interfacesmodule.bean.weather.forecast.ForecastBase;
import interfaces.heweather.com.interfacesmodule.bean.weather.now.Now;
import interfaces.heweather.com.interfacesmodule.view.HeConfig;
import interfaces.heweather.com.interfacesmodule.view.HeWeather;

public class MainActivity extends AppCompatActivity {

    private TextView degreeText;

    private TextView weatherInfoText;

    private TextView title_city;

    private TextView airInfoText;

    private LinearLayout forecastLayout;

    public LocationClient mLocationClient;  //定位信息

    public String location_city;

    public Context mcontext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        mcontext = this;
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        //SDKInitializer.initialize(getApplicationContext());


        setContentView(R.layout.activity_weather);

        title_city = (TextView) findViewById(R.id.title_city);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        airInfoText = (TextView) findViewById(R.id.air_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);

        HeConfig.init("HE1901211517501600", "fb73502e69654b4b80c8694a08f3a48c");
        HeConfig.switchToFreeServerNode();

        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                .READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }else {
            requestLocation();
        }



    }

    //初始化位置
    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        //option.setScanSpan(3000);
        option.setIsNeedAddress(true);
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        mLocationClient.setLocOption(option);
    }

    //获取位置信息
    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    public class MyLocationListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            StringBuilder currentPosition = new StringBuilder();
            currentPosition.append(bdLocation.getLatitude()).append(",").append(bdLocation.getLongitude());
            //currentPosition.append(bdLocation.getCity());
            String location = currentPosition.toString();
            location_city = bdLocation.getCity();
            Log.i("onReceiveLocation", "location: "+location);
            //在获取位置后再初始化天气
            intiWeather(location);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0){
                    for (int result :grantResults){
                        if (result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this, "必须同意所有权限才能使用本程序",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    //初始化天气，通过 百度 定位并获取城市数据
    public void intiWeather(final String location) {
        //requestLocation();
        Log.i("Log", "location: "+location);
        HeWeather.getWeatherNow(this, location,  Lang.CHINESE_SIMPLIFIED, Unit.METRIC,
                new HeWeather.OnResultWeatherNowBeanListener() {

                    public void onError(Throwable e) {
                        Log.i("Log", "onError: ", e);
                    }

                    public void onSuccess(List<Now> dataObject) {
                        Log.i("Log", "onSuccess: " + new Gson().toJson(dataObject));

                        Now now = (Now) dataObject.get(0);
                        degreeText.setText(now.getNow().getTmp() + "℃");
                        weatherInfoText.setText(now.getNow().getCond_txt());
                        title_city.setText(now.getBasic().getLocation());
                    }
                });

        //获取目前的空气状态
        HeWeather.getAirNow(this, location_city, Lang.CHINESE_SIMPLIFIED, Unit.METRIC,
                new HeWeather.OnResultAirNowBeansListener() {

                    @Override
                    public void onError(Throwable throwable) {
                        Log.i("Log", "onError: ", throwable);
                        Toast.makeText(mcontext,throwable.toString(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(List<AirNow> list) {
                        Log.i("Log", "onSuccess: " + new Gson().toJson(list));
                        AirNow airNow = list.get(0);
                        airInfoText.setText(airNow.getAir_now_city().getQlty() +
                                " " + airNow.getAir_now_city().getAqi());
                    }
                });

        //获取未来七天的天气预报
        HeWeather.getWeatherForecast(this, location, Lang.CHINESE_SIMPLIFIED, Unit.METRIC,
                new HeWeather.OnResultWeatherForecastBeanListener(){

                    @Override
                    public void onError(Throwable throwable) {
                        Log.i("Log", "onError: ", throwable);
                    }

                    @Override
                    public void onSuccess(List<Forecast> list) {
                        Log.i("Log", "onSuccess: " + new Gson().toJson(list));
                        forecastLayout.removeAllViews();
                        Forecast forecast = list.get(0);
                        //final AirForecast[] airForecast = new AirForecast[1];
                        //AirForecastBase airForecastBase = new AirForecastBase();
                        //获取七天空气质量预报
//                        HeWeather.getAirForecast(mcontext, location, new HeWeather.OnResultAirForecastBeansListener() {
//                            @Override
//                            public void onError(Throwable throwable) {
//                                Log.i("Log", "onError: ", throwable);
//                                Toast.makeText(mcontext,throwable.toString(), Toast.LENGTH_LONG).show();
//                            }
//
//                            @Override
//                            public void onSuccess(List<AirForecast> list) {
//                                airForecast[0] = list.get(0);
//                                Log.i("Log", "onSuccess: " + new Gson().toJson(list));
//                            }
//                        });

                        for (ForecastBase forecastBase: forecast.getDaily_forecast()){
                            final View view = LayoutInflater.from(mcontext).inflate(R.layout.forecast_item, forecastLayout, false);
                            TextView dataAndInfo = (TextView) view.findViewById(R.id.dateAndInfo_text);
                            //final TextView airInfo = (TextView) view.findViewById(R.id.air_text);
                            ImageView infoImage = (ImageView) view.findViewById(R.id.info_image);
                            TextView maxAndMin = (TextView) view.findViewById(R.id.maxAndMin_text);

                            Log.i("dataAndInfo:", forecastBase.getDate() + "·" + forecastBase.getCond_txt_d());
                            //设置日期和天气描述
                            dataAndInfo.setText(forecastBase.getDate() + "·" + forecastBase.getCond_txt_d());

                            //设置空气状态

//                            HeWeather.getAirNow(mcontext, "auto_ip", Lang.CHINESE_SIMPLIFIED, Unit.METRIC,
//                                    new HeWeather.OnResultAirNowBeansListener() {
//
//                                        @Override
//                                        public void onError(Throwable throwable) {
//                                            Log.i("Log", "onError: ", throwable);
//                                        }
//
//                                        @Override
//                                        public void onSuccess(List<AirNow> list) {
//                                            Log.i("Log", "onSuccess: " + new Gson().toJson(list));
//                                            AirNow airNow = list.get(0);
//                                            airInfo.setText(airNow.getAir_now_city().getQlty());
//                                        }
//                                    });

                                //设置天气状态图片
                            String cond_code_d = forecastBase.getCond_code_d();
                            StringBuilder imageUri = new StringBuilder();
                            imageUri.append("https://cdn.heweather.com/cond_icon/").append(cond_code_d).append(".png");
                            Glide.with(mcontext).load(imageUri.toString()).into(infoImage);

                                //设置最高温度和最低温度
                                StringBuilder tmp = new StringBuilder();
                                tmp.append(forecastBase.getTmp_max()).append("/")
                                        .append(forecastBase.getTmp_min()).append("℃");
                                maxAndMin.setText(tmp.toString());

                                forecastLayout.addView(view);
                            }



                    }
                });

    }
}
