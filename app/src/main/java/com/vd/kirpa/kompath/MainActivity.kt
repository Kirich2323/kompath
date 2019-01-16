package com.vd.kirpa.kompath

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.firebase.analytics.FirebaseAnalytics


class MainActivity : AppCompatActivity(), SensorEventListener, RewardedVideoAdListener {

    private var mSensorManager: SensorManager? = null
    private var mMagnetSensor: Sensor? = null
    private var mAccelSensor: Sensor? = null
    private var compass: Bitmap? = null
    private var compassView: ImageView? = null
    private var degreesTextView: TextView? = null
    private var gravity: FloatArray = FloatArray(3)
    private var geoMagnetic: FloatArray = FloatArray(3)
    private lateinit var levelImageView: ImageView

    private var adConsumed = false
    private lateinit var mRewardedVideoAd: RewardedVideoAd
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        compass = BitmapFactory.decodeResource(resources, R.drawable.compass)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val bundle = Bundle()
        bundle.putString("test_string", findViewById<TextView>(R.id.degreeTextView).text.toString())
        mFirebaseAnalytics.logEvent("test_event", bundle)

        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713")

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.loadAd("ca-app-pub-3940256099942544/5224354917", AdRequest.Builder().build())
        mRewardedVideoAd.rewardedVideoAdListener = this

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mMagnetSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mAccelSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_GRAVITY)

        findViewById<Button>(R.id.levelButton).setOnClickListener {
            if (mRewardedVideoAd.isLoaded) { mRewardedVideoAd.show() }
        }

        levelImageView = findViewById(R.id.levelImageView)
        degreesTextView = findViewById(R.id.degreeTextView)
        compassView = findViewById(R.id.compassView)
        compassView?.setImageBitmap(compass as Bitmap)
        Thread {
            while(true) {
                Thread.sleep(1000)
                val bundle = Bundle()
                bundle.putString("direction", findViewById<TextView>(R.id.degreeTextView).text.toString())
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.EARN_VIRTUAL_CURRENCY, bundle)
            }
        }.start()
    }

    override fun onRewarded(reward: RewardItem) {
        Toast.makeText(this, "onRewarded! currency: ${reward.type} amount: ${reward.amount}",
            Toast.LENGTH_SHORT).show()

        adConsumed = true
        findViewById<Button>(R.id.levelButton).visibility = View.INVISIBLE
        levelImageView.visibility = View.VISIBLE
    }

    override fun onRewardedVideoAdLeftApplication() {
        //Toast.makeText(this, "onRewardedVideoAdLeftApplication", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoAdClosed() {
        //Toast.makeText(this, "onRewardedVideoAdClosed", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoAdFailedToLoad(errorCode: Int) {
        //Toast.makeText(this, "onRewardedVideoAdFailedToLoad", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoAdLoaded() {
        //Toast.makeText(this, "onRewardedVideoAdLoaded", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoAdOpened() {
        //Toast.makeText(this, "onRewardedVideoAdOpened", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoStarted() {
        //Toast.makeText(this, "onRewardedVideoStarted", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoCompleted() {
        //Toast.makeText(this, "onRewardedVideoCompleted", Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when(event.sensor.type) {

                Sensor.TYPE_MAGNETIC_FIELD -> {
                    geoMagnetic[0] = event.values[0]
                    geoMagnetic[1] = event.values[1]
                    geoMagnetic[2] = event.values[2]
                }
                Sensor.TYPE_GRAVITY-> {
                    gravity[0] = event.values[0]
                    gravity[1] = event.values[1]
                    gravity[2] = event.values[2]
                }
            }

            if (compass == null) {
                //Log.d("kirich", "empty compass")
                return
            }

            compassView?.post {
                var R = FloatArray(9)
                var I = FloatArray(9)
                val success = SensorManager.getRotationMatrix(R, I, gravity, geoMagnetic)
                if (success) {
                    var orient = FloatArray(3)
                    SensorManager.getOrientation(R, orient)
                    var degrees = Math.toDegrees(orient[0].toDouble())
                    degrees = (degrees + 360) % 360
                    var textDegrees = degrees
                    if (gravity[2] > 0) {
                        degrees *= -1
                    }
                    else {
                        textDegrees = (360 - textDegrees)% 360
                    }

                    compassView?.rotation = degrees.toFloat()
                    degreesTextView?.text = "${textDegrees.toInt()}"

                    if (adConsumed) {
                        val len = 40
                        val radius = 80
                        val bm = Bitmap.createBitmap(levelImageView.width, levelImageView.height, Bitmap.Config.ARGB_8888)
                        var c = Canvas(bm)
                        val p = Paint()
                        p.strokeWidth = 25.0f

                        p.color = Color.BLUE
                        var cx = levelImageView.width / 2.0f
                        var cy = levelImageView.height / 2.0f
                        c.drawLine(cx - len, cy - len, cx + len, cy + len, p)
                        c.drawLine(cx + len, cy - len, cx - len, cy + len, p)

                        p.color = Color.RED
                        cx = cx - radius * Math.sin(orient[2].toDouble()).toFloat()
                        cy = cy + radius * Math.sin(orient[1].toDouble()).toFloat()
                        c.drawLine(cx - len, cy - len, cx + len, cy + len, p)
                        c.drawLine(cx + len, cy - len, cx - len, cy + len, p)

                        levelImageView.setImageBitmap(bm)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this, mMagnetSensor, SensorManager.SENSOR_DELAY_GAME)
        mSensorManager!!.registerListener(this, mAccelSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
