package com.xhateya.idn.xheweather

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.xhateya.idn.xheweather.Constants.API_KEY
import com.xhateya.idn.xheweather.Constants.METRIC_UNIT
import com.xhateya.idn.xheweather.models.WeatherResponse
import com.xhateya.idn.xheweather.network.WeatherConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {


    companion object{
        private const val TAG = "MainActivity"
    }


    private lateinit var  mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)







        mFusedLocationClient= LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()){
            Toast.makeText(
                this,
                "Your Location Provider is turned off, please turn it on",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION

                )
                .withListener(object : MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()){
                            requestLocationData()
                        }
                        if (report.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please Enable them as it is mandatory for the app to work",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogPermission()
                    }


                }).onSameThread()
                .check()

            Toast.makeText(
                this,
                "Your Location Provider is already on",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showRationalDialogPermission() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions requires for this feature. it can be enabled under Application Settings ")
            .setPositiveButton(
                "Go To Settings"
            ){_,_ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel"){ dialog,
                                          _ ->

                dialog.dismiss()
            }.show()



    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest= com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()!!
        )
    }

    private val mLocationCallback= object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude= mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", " $longitude")


            getLocationWeatherDataDetails(latitude, longitude)


        }
    }

    private fun getLocationWeatherDataDetails(latitude: Double, longitude: Double){
        if(Constants.isNetworkAvailable(this)){
            val client= WeatherConfig.getWeatherService()
                .getWeather(latitude,longitude, METRIC_UNIT, API_KEY)

            client.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    val responseBody= response.body()
                    if(response.isSuccessful && responseBody != null){

                        hideProgressDialog()
                        setupUI(responseBody)

                        Log.i("Response Result", "$responseBody")
                    }else{
                        Log.e(TAG, "onFailure: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e(TAG,"Error: ${t.message.toString()}")
                    hideProgressDialog()
                }

            })
        }else{
            Toast.makeText(
                this@MainActivity,
                "No Internet Connection Available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
    private fun showCustomProgressDialog(){
        mProgressDialog= Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()

    }

    private fun hideProgressDialog(){
        if(mProgressDialog !=null){
            mProgressDialog!!.dismiss()
        }
    }

    private fun setupUI(responseBody: WeatherResponse){
        for (i in responseBody.weather.indices){
            Log.i("Weather Name", responseBody.weather.toString())


            val tvCity: TextView= findViewById(R.id.tv_city)
            tvCity.text = responseBody.name

            val tvStatus: TextView = findViewById(R.id.tv_sts)
            tvStatus.text = responseBody.weather[i].description

            val tvDegree: TextView = findViewById(R.id.tv_degree)
            tvDegree.text = responseBody.main.temp.toString()+ getUnit(application.resources.configuration.locales.toString())


            val ivWeather: ImageView= findViewById(R.id.iv_weather)
            when(responseBody.weather[i].icon){
                "01d" -> ivWeather.setImageResource(R.drawable.ic_sunny)
                "02d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "03d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "04d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "09d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "10d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "11d" -> ivWeather.setImageResource(R.drawable.ic_sunny)
                "13d" -> ivWeather.setImageResource(R.drawable.ic_sunny)
                "01n" -> ivWeather.setImageResource(R.drawable.ic_sunny)
                "02n" -> ivWeather.setImageResource(R.drawable.ic_sunny)
                "03n" -> ivWeather.setImageResource(R.drawable.ic_sunny)
                "10n" -> ivWeather.setImageResource(R.drawable.ic_sunny)
                "11n" -> ivWeather.setImageResource(R.drawable.ic_sunny)

            }






        }
    }

    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)

    }

    private fun getUnit(value: String): String? {
        var value = "°C"
        if("US" == value || "LR" == value || "MM" == value){
            value = "°F"
        }
        return value

    }

}