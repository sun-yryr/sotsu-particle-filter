package com.example.rotation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.lang.Exception
import java.lang.Math.*
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity(), SensorEventListener {
    private var mManager: SensorManager by Delegates.notNull<SensorManager>()
    private var mSensor: Sensor by Delegates.notNull<Sensor>()
    private var mgSensor: Sensor by Delegates.notNull<Sensor>()

    var time = 0.0
    val epsiron = 0.000001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    fun cast(a: Double): Double {
        if (abs(a) > this.epsiron) {
            return a
        } else {
            return 0.0
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val now = event.timestamp / pow(10.0, 9.0)
            val x = cast(event.values[0]*(now - time))
            val y = cast(event.values[1]*(now - time))
            val z = cast(event.values[2]*(now - time))
            // Log.d("test", "X: "+x.toString()+", Y: "+y.toString()+", Z: "+z.toString())
            Log.d("test:x", x.toString())
            Log.d("test:y", y.toString())
            Log.d("test:z", z.toString())
            this.time = now
        }
    }

    //センサー精度が変更されたときに発生するイベント
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    //アクティビティが閉じられたときにリスナーを解除する
    override fun onPause() {
        super.onPause()
        //リスナーを解除しないとバックグラウンドにいるとき常にコールバックされ続ける
        mManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        //リスナーとセンサーオブジェクトを渡す
        //第一引数はインターフェースを継承したクラス、今回はthis
        //第二引数は取得したセンサーオブジェクト
        //第三引数は更新頻度 UIはUI表示向き、FASTはできるだけ早く、GAMEはゲーム向き
        mManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST)
        //mManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }
}
