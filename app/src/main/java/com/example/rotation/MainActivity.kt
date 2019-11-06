package com.example.rotation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.properties.Delegates
import kotlin.io.*

class MainActivity : AppCompatActivity()/*, SensorEventListener*/ {
    private var mManager: SensorManager by Delegates.notNull<SensorManager>()
    private var mSensor: Sensor by Delegates.notNull<Sensor>()
    private var maSensor: Sensor by Delegates.notNull<Sensor>()
    private var mgSensor: Sensor by Delegates.notNull<Sensor>()

    /* ALPHA = 0でローパスなしになる */
    private val ALPHA = 0.8F
    private val MATRIX_SIZE = 16
    private val DIMENSION = 3

    private var PF = ParticleFilter(100, 0, 1)

    /* 生の値たち */
    private var raw_acceleration_value = FloatArray(3)
    private var raw_magnetic_value = FloatArray(3)
    private var raw_gravity_value = FloatArray(3)
    /* 加速度のローパスで生成した重力 */
    private var acc_gravity_value = FloatArray(3)
    /* 世界座標軸での加速度3ベクトル */
    private var world_coordinate_acceleration1 = FloatArray(6)
    private var world_coordinate_acceleration2 = FloatArray(6)
    /* 前回の加速度を保管しておくやつ */
    private var prev_wca1 = FloatArray(3)
    private var prev_wca2 = FloatArray(3)

    /* テスト用 CSV readline */
    private val TEST = true     // テスト時はtrueにしておく
    private fun testByCSV() {
        /* res/raw/acc.csv としてデータを保存することで以下の記述で読み込むことができる
         * おそらくこの方法が一番楽ちん */
        val inputstream = resources.openRawResource(R.raw.acc)
        var secTime = 0.02
        val FileName = "output.csv"
        var isFirst = true
        Log.d("log", "Calculate Start")
        applicationContext.openFileOutput(FileName, Context.MODE_PRIVATE).use { it.write("".toByteArray()) }
        inputstream.bufferedReader().useLines { it.forEach { line: String ->
            val tmp = line.split(",")
            val test_acc_value = FloatArray(3) { i -> tmp[i + 1].toFloat() }
            //val test_gravity_value = FloatArray(3) { i -> tmp[i + 4].toFloat()}
            val test_magnetic_value = FloatArray(3) { i -> tmp[i + 7].toFloat() }
            //val time = 0.02      // 取得したデータが50Hz固定なので，
            lowpassFilter(acc_gravity_value, test_acc_value)
            /* 加速度から生み出した重力と地磁気センサから速度と加速度の配列を計算する */
            var R = generate_rotation_matrix(
                acc_gravity_value,
                test_magnetic_value,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Y
            )
            if (R.isZero() || !isFirst) {
                secTime += 0.02
            } else {
                secTime = 0.02
                isFirst = false

                var acc_linear_value = FloatArray(4)
                for (i in 0..2) {
                    acc_linear_value[i] = test_acc_value[i] - acc_gravity_value[i]
                }
                //var invertR = FloatArray(MATRIX_SIZE)
                //Matrix.invertM(invertR, 0, R, 0)
                var wc_acceleration = FloatArray(4)
                Matrix.multiplyMV(wc_acceleration, 0, R, 0, acc_linear_value, 0)
                for (i in 0..2) {
                    prev_wca1[i] = world_coordinate_acceleration1[i + 3]
                    world_coordinate_acceleration1[i + 3] = wc_acceleration[i]
                }
                var tmp_speed = generate_speed(prev_wca1, world_coordinate_acceleration1, secTime)
                for (i in 0..2) {
                    world_coordinate_acceleration1[i] = tmp_speed[i]
                }
                val o = "speed, acc:" + world_coordinate_acceleration1.joinToString(",")
                Log.d("tag", o)
                /* 重力センサと地磁気センサから速度と加速度の配列を計算する
                R = generate_rotation_matrix(
                    test_gravity_value,
                    test_magnetic_value,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z
                )
                acc_linear_value = FloatArray(4)
                for (i in 0..2) {
                    acc_linear_value[i] = test_acc_value[i] - test_gravity_value[i]
                }
                invertR = FloatArray(16)
                Matrix.invertM(invertR, 0, R, 0)
                wc_acceleration = FloatArray(4)
                Matrix.multiplyMV(wc_acceleration, 0, invertR, 0, acc_linear_value, 0)
                for (i in 0..2) {
                    prev_wca2[i] = world_coordinate_acceleration2[i + 3]
                    world_coordinate_acceleration2[i + 3] = wc_acceleration[i]
                }
                tmp_speed = generate_speed(prev_wca2, world_coordinate_acceleration2, time)
                for (i in 0..2) {
                    world_coordinate_acceleration2[i] = tmp_speed[i]
                }
                */
                /* PartileFilterにとおす */
                val output = PF.run(world_coordinate_acceleration1, world_coordinate_acceleration1)
                val outputString = tmp[0] + "," + output.joinToString(",") + "," + world_coordinate_acceleration1.joinToString(",") + "\n\n"
                val outputParticle = PF.debug_partilue()
                val outputlikelihood = PF.debug_likelihood()
                /* ファイルに書き出したいな */
                applicationContext.openFileOutput(FileName, Context.MODE_APPEND).use {
                    it.write(outputString.toByteArray())
                    for (i in outputParticle.indices) {
                        val o = outputParticle[i].joinToString(",") + "\n"
                        it.write(o.toByteArray())
                    }
                }
            }
        } }
        Log.d("log", "Calculate End")
    }




    var prev_time1 = 0.0
    var prev_time2 = 0.0
    val epsiron = 0.000001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (TEST) {
            testByCSV()
        } else {
            mSensor = mManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            maSensor = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            mgSensor = mManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        }
    }
/*
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                raw_acceleration_value = event.values.clone()
                lowpassFilter(acc_gravity_value, raw_acceleration_value)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lowpassFilter(raw_magnetic_value, event.values.clone())
            }
            Sensor.TYPE_GRAVITY -> {
                raw_gravity_value = event.values.clone()
            }
        }
        if (!raw_acceleration_value.isZero() && !raw_magnetic_value.isZero()) {
            val now_time = event.timestamp * 1e-9 // 秒
            val time = now_time - prev_time1
            prev_time1 = now_time

            var R = generate_rotation_matrix(acc_gravity_value, raw_magnetic_value, SensorManager.AXIS_X, SensorManager.AXIS_Z)
            var acc_linear_value = FloatArray(4)
            for (i in 0..2) {
                acc_linear_value[i] = raw_acceleration_value[i] - acc_gravity_value[i]
            }
            var invertR = FloatArray(16)
            Matrix.invertM(invertR, 0, R, 0)
            var wc_acceleration = FloatArray(4)
            Matrix.multiplyMV(wc_acceleration, 0, invertR, 0, acc_linear_value, 0)
            for (i in 0..2) {
                prev_wca1[i] = world_coordinate_acceleration1[i+3]
                world_coordinate_acceleration1[i+3] = wc_acceleration[i]
            }
            val tmp_speed = generate_speed(prev_wca1, world_coordinate_acceleration1, time)
            for (i in 0..2) {
                world_coordinate_acceleration1[i] = tmp_speed[i]
            }
        }
        if (!raw_gravity_value.isZero() && !raw_magnetic_value.isZero() && !raw_acceleration_value.isZero()) {
            val now_time = event.timestamp * 1e-9 // 秒
            val time = now_time - prev_time2
            prev_time2 = now_time

            var R = generate_rotation_matrix(raw_gravity_value, raw_magnetic_value, SensorManager.AXIS_X, SensorManager.AXIS_Z)
            var acc_linear_value = FloatArray(4)
            for (i in 0..2) {
                acc_linear_value[i] = raw_acceleration_value[i] - raw_gravity_value[i]
            }
            var invertR = FloatArray(16)
            Matrix.invertM(invertR, 0, R, 0)
            var wc_acceleration = FloatArray(4)
            Matrix.multiplyMV(wc_acceleration, 0, invertR, 0, acc_linear_value, 0)
            for (i in 0..2) {
                prev_wca2[i] = world_coordinate_acceleration2[i+3]
                world_coordinate_acceleration2[i+3] = wc_acceleration[i]
            }
            val tmp_speed = generate_speed(prev_wca2, world_coordinate_acceleration2, time)
            for (i in 0..2) {
                world_coordinate_acceleration2[i] = tmp_speed[i]
            }
        }

        if (!world_coordinate_acceleration1.isZero() && !world_coordinate_acceleration2.isZero()) {
            val output = PF.run(world_coordinate_acceleration1, world_coordinate_acceleration2)
            Log.d("TAG", "output: "+ output.joinToString(separator = ", "))
            for (i in 0..2) {
                world_coordinate_acceleration1[i] = 0F
                world_coordinate_acceleration2[i] = 0F
            }
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
        mManager.registerListener(this, maSensor, SensorManager.SENSOR_DELAY_FASTEST)
        mManager.registerListener(this, mgSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }
*/

    /*--------------------------　自作関数群　--------------------------*/

    fun lowpassFilter(vecPrev: FloatArray, vecNow: FloatArray) {
        for (i in vecPrev.indices) {
            vecPrev[i] = ALPHA * vecPrev[i] + (1-ALPHA) * vecNow[i]
        }
    }

    fun generate_rotation_matrix(gravity: FloatArray, geomagnetic: FloatArray, AXIS_X: Int = SensorManager.AXIS_X, AXIS_Y: Int = SensorManager.AXIS_Y): FloatArray {
        var rotationMatrix = FloatArray(MATRIX_SIZE)
        var inclinationMatrix = FloatArray(MATRIX_SIZE)
        var remapedMatrix = FloatArray(MATRIX_SIZE)
        var orientationValues = FloatArray(DIMENSION)

        val result = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic)
        if (!result) {
            return FloatArray(MATRIX_SIZE)
        }
        SensorManager.remapCoordinateSystem(rotationMatrix, AXIS_X, AXIS_Y, remapedMatrix)
        SensorManager.getOrientation(remapedMatrix, orientationValues)
        return remapedMatrix
    }

    /* isEmptyが想像通りに動かないので，全要素0ならtrueが返るメンバ関数 */
    fun FloatArray.isZero(): Boolean {
        var counter = 0
        for (item in this) {
            if (item == 0F) {
                counter++
            }
        }
        if (counter == this.size) {
            return true
        } else {
            return false
        }
    }

    fun toDeg(va: Float): Float {
        if (va >= 0) {
            return Math.toDegrees(va.toDouble()).toFloat()
        } else {
            return Math.toDegrees(va.toDouble()).toFloat()
        }
    }

    /**
     * 加速度2つを微分して速度に変換する
     * @param prev_acc [FloatArray]
     * @param next_acc [FloatArray]
     * @param time [sec]
     * @return [FloatArray]
     */
    private fun generate_speed(prev_acc: FloatArray, next_acc: FloatArray, sec: Double): FloatArray {
        val Ftime = sec.toFloat()
        var result = FloatArray(3)
        for (i in 0..2) {
            result[i] = ((prev_acc[i] + next_acc[i+3]) * Ftime) / 2
        }
        return result
    }
}
