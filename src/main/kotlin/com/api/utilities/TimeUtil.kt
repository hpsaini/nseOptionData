package com.api.utilities

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*


object TimeUtil {
    private lateinit var executionTime: String
    private lateinit var executionDate: String



    fun getExecutionTimeStamp(): String {
        if(!::executionTime.isInitialized){
            executionTime = SimpleDateFormat("kkmm").format(Date())
        }
        return executionTime
    }
    fun getDate():String {
        if (!::executionDate.isInitialized) {
            val localDate = LocalDateTime.now()
            executionDate = SimpleDateFormat("ddMMM").format(Date())
        }
        return executionDate
    }
}