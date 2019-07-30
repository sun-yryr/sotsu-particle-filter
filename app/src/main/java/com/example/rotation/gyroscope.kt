package com.example.rotation

import android.hardware.Sensor
import android.hardware.SensorEvent

public fun gyroToRotate(sensor: SensorEvent): Array<Double> {
    val x = sensor.values[0].toDouble()
    val y = sensor.values[1].toDouble()
    val z = sensor.values[2].toDouble()
    val res = arrayOf(x,y,z)
    return res
}