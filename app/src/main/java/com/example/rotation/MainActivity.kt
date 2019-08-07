package com.example.rotation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Switch
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


    private val MATRIX_SIZE = 16
    private val DIMENSION = 3
    private var mMagneticValues = FloatArray(3)
    private var mAccelerometerValues = FloatArray(3)


    var time = 0.0
    val epsiron = 0.000001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        mManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mgSensor = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun cast(a: Double): Double {
        if (abs(a) > this.epsiron) {
            return a
        } else {
            return 0.0
        }
    }

    fun toDegrees(values: FloatArray): FloatArray {
        values.map { value ->
            if (value >= 0) {
                Math.floor(Math.toDegrees(value.toDouble()))
            } else {
                Math.floor(Math.toDegrees(value.toDouble()))
            }
        }
        return values
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                mAccelerometerValues = event.values.clone()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mMagneticValues = event.values.clone()
            }
        }
        if (mAccelerometerValues.isNotEmpty() && mMagneticValues.isNotEmpty()) {
            var rotationMatrix = FloatArray(MATRIX_SIZE)
            var inclinationMatrix = FloatArray(MATRIX_SIZE)
            var remapedMatrix = FloatArray(MATRIX_SIZE)
            var orientationValues = FloatArray(DIMENSION)

            SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, mAccelerometerValues, mMagneticValues)
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapedMatrix)
            SensorManager.getOrientation(remapedMatrix, orientationValues)

            var degs = toDegrees(orientationValues)

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
        mManager.registerListener(this, mgSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }
}
