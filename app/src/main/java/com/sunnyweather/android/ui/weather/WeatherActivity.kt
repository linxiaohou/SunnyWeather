package com.sunnyweather.android.ui.weather

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.sunnyweather.android.R
import com.sunnyweather.android.databinding.ActivityWeatherBinding
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.model.getSky
import java.text.SimpleDateFormat
import java.util.Locale

class WeatherActivity : AppCompatActivity() {
    lateinit var weatherBinding: ActivityWeatherBinding
    val viewMode by lazy { ViewModelProvider(this)[WeatherViewMode::class.java] }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val decorView = window.decorView
        decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT
        weatherBinding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(weatherBinding.root)
        if (viewMode.locationLng.isEmpty()) {
            viewMode.locationLng = intent.getStringExtra("location_lng") ?: ""
        }
        if (viewMode.locationLat.isEmpty()) {
            viewMode.locationLat = intent.getStringExtra("location_lat") ?: ""
        }
        if (viewMode.placeName.isEmpty()) {
            viewMode.placeName = intent.getStringExtra("place_name") ?: ""
        }
        viewMode.weatherLiveData.observe(this, Observer { result ->
            val weather = result.getOrNull()
            if (weather != null) {
                showWeatherInfo(weather)
            } else {
                Toast.makeText(this, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            weatherBinding.swipeRefresh.isRefreshing = false
        })
        weatherBinding.swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        refreshWeather()
        weatherBinding.swipeRefresh.setOnRefreshListener { refreshWeather() }
        weatherBinding.nowLayout.navBtn.setOnClickListener {
            weatherBinding.drawerLayout.openDrawer(GravityCompat.START)
        }
        weatherBinding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(
                    drawerView.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    fun refreshWeather() {
        viewMode.refreshWeather(viewMode.locationLng, viewMode.locationLat)
        weatherBinding.swipeRefresh.isRefreshing = true
    }

    private fun showWeatherInfo(weather: Weather) {
        weatherBinding.nowLayout.placeName.text = viewMode.placeName
        val realtime = weather.realtime
        val daily = weather.daily
        //填充now.xml布局中的数据
        val currentTempText = "${realtime.temperature.toInt()} ℃"
        weatherBinding.nowLayout.currentTemp.text = currentTempText
        weatherBinding.nowLayout.currentSky.text = getSky(realtime.skycon).info
        val currentPM25Text = "空气指数 ${realtime.airQuality.aqi.chn.toInt()}"
        weatherBinding.nowLayout.currentAQI.text = currentPM25Text
        weatherBinding.nowLayout.nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)
        //填充forecast.xml布局中的数据
        weatherBinding.forecastLayout.forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for (i in 0 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val view = LayoutInflater.from(this).inflate(
                R.layout.forecast_item,
                weatherBinding.forecastLayout.forecastLayout,
                false
            )
            val dateInfo: TextView = view.findViewById(R.id.dateInfo)
            val skyIcon: ImageView = view.findViewById(R.id.skyIcon)
            val skyInfo: TextView = view.findViewById(R.id.skyInfo)
            val temperatureInfo: TextView = view.findViewById(R.id.temperatureInfo)
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateInfo.text = simpleDateFormat.format(skycon.date)
            val sky = getSky(skycon.value)
            skyIcon.setImageResource(sky.icon)
            skyInfo.text = sky.info
            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()} ℃"
            temperatureInfo.text = tempText
            weatherBinding.forecastLayout.forecastLayout.addView(view)
        }
        //填充life_index.xml布局中的数据
        val lifeIndex = daily.lifeIndex
        weatherBinding.lifeIndexLayout.coldRiskText.text = lifeIndex.coldRisk[0].desc
        weatherBinding.lifeIndexLayout.dressingText.text = lifeIndex.dressing[0].desc
        weatherBinding.lifeIndexLayout.ultravioletText.text = lifeIndex.ultraviolet[0].desc
        weatherBinding.lifeIndexLayout.carWashingText.text = lifeIndex.carWashing[0].desc
        weatherBinding.weatherLayout.visibility = View.VISIBLE
    }
}