package com.abhishekbishnoi.speedometer

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.anastr.speedviewlib.ImageSpeedometer
import com.github.anastr.speedviewlib.components.indicators.ImageIndicator
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var oldlat = 0.0;
    private var oldlong = 0.0;
    private var isStarted = false
    private var tripDistance: Long = 0
    private var currentDistance: Long =0
    private var tripDistancef = 0.0f
    private var avgSpeedf = 0.0f
    private var timePassed: Long = 0


    @SuppressLint("MissingPermission", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()

        val speedometer = findViewById<ImageSpeedometer>(R.id.imageSpeedometer)
        speedometer.minSpeed = 0f
        speedometer.maxSpeed = 50f
        val drawable: Drawable = resources.getDrawable(R.drawable.image_indicator1)
        val imageIndicator = ImageIndicator(this, drawable)
        speedometer.indicator = imageIndicator






        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationByGps()

        locationRequest = LocationRequest().apply {

            interval = TimeUnit.SECONDS.toMillis(5)

            fastestInterval = TimeUnit.SECONDS.toMillis(5)

            maxWaitTime = TimeUnit.SECONDS.toMillis(5)

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let {

                    locationByGps()

                } ?: {

                }
            }
        }

        speedometer.speedTo(0.0f, 500)
        val duration = findViewById<TextView>(R.id.duration)

        val maxTime: Long = 43200000
        var timer: Long = 0
        var minutes: Long =0
        var seconds: Long = 0
        val cdtimer = object : CountDownTimer(43200000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                timePassed = (maxTime - millisUntilFinished)
                timer = (maxTime - millisUntilFinished)/1000
                minutes = (timer/60)
                seconds = timer - minutes*60
                if (seconds<10){
                    duration.text = "$minutes : 0$seconds Min"
                }else{
                    duration.text = "$minutes : $seconds Min"
                }
            }

            override fun onFinish() {

            }
        }



        val startButton = findViewById<Button>(R.id.startButton)

        startButton.setOnClickListener {

            speedometer.speedTo(0.0f, 1000)

            if (!isStarted){
                isStarted = true
                cdtimer.start()

                locationByGps()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

                startButton.text = "STOP"
                startButton.setBackgroundColor(Color.GRAY)

            }else{

                isStarted = false
                cdtimer.cancel()
                startButton.text = "START"
                startButton.setBackgroundColor(resources.getColor(R.color.purple_500))
                fusedLocationClient.removeLocationUpdates(locationCallback)
                val speed = findViewById<TextView>(R.id.currentSpeed)
                speed.text = "0"

            }



            }







    }

    private fun requestPermission(){
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 11)

        }
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), 11)

        }
    }

    @SuppressLint("MissingPermission")
    private fun locationByGps(){

        fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
            if (location==null){
                Toast.makeText(this, "Turn ON Location", Toast.LENGTH_LONG).show()
            }
            else{
                val latitude = location.latitude
                val longitude = location.longitude
                val lat = findViewById<TextView>(R.id.latitude)
                val log = findViewById<TextView>(R.id.longitude)
                lat.text = latitude.toString()
                log.text = longitude.toString()
                val distance = findViewById<TextView>(R.id.tripDistance)
                val avgSpeed = findViewById<TextView>(R.id.avgSpeed)
                val totalDistance = findViewById<TextView>(R.id.totalDistance)
                val signalImage = findViewById<ImageView>(R.id.signalStrength)

                if (location.hasSpeed()){
                    val speedometer = findViewById<ImageSpeedometer>(R.id.imageSpeedometer)
                    speedometer.speedTo((location.speed)*18/5, 4500)
                    val speed = findViewById<TextView>(R.id.currentSpeed)

                    speed.text = String.format("%.1f", (location.speed * 18/5))

                }

                if (isStarted){

                    currentDistance = calculateDistance(oldlat,oldlong,latitude,longitude)
                    tripDistance += currentDistance
                    tripDistancef = (tripDistance/1000000).toFloat()
                    distance.text = tripDistancef.toString()
                    avgSpeedf = ((tripDistance/timePassed) * 36/10000).toFloat()
                    avgSpeed.text = avgSpeedf.toString()
                }

                if (location.accuracy <= 4){
                    signalImage.setImageResource(R.drawable.signal3)
                } else if (location.accuracy <= 10){
                    signalImage.setImageResource(R.drawable.signal2)
                } else if (location.accuracy <= 50){
                    signalImage.setImageResource(R.drawable.signal1)
                } else {
                    signalImage.setImageResource(R.drawable.signal0)
                }

                oldlat = latitude
                oldlong = longitude



            }
        }
    }

    private fun calculateDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Long {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lng2 - lng1)
        val a = (Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + (Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2)))
        val c = 2 * Math.asin(Math.sqrt(a))
        return Math.round(6371000 * c)
    }


}