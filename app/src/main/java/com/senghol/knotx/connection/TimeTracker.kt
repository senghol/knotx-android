package com.senghol.knotx.connection
class TimeTracker {

    private var startTime: Long = 0L
    private var isRunning = false

    fun trigger() {
        startTime = System.currentTimeMillis()
        isRunning = true
    }

    fun untrigger(tag: String = "TimeTracker") {
        if (!isRunning) {
            return
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime


        isRunning = false
    }
}