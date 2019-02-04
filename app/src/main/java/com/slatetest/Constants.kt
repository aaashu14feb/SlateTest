package com.slatetest

object Constants {

    const val BROADCAST_ACTION = "BROADCAST_ACTION"
    const val BROADCAST_TASK_ENTER = "BROADCAST_TASK_ENTER"

    const val KEY_GEOFENCE_NAME = "KEY_GEOFENCE_NAME"
    const val KEY_GEOFENCE_RADIUS = "KEY_GEOFENCE_RADIUS"
    const val KEY_GEOFENCE_LAT = "GEOFENCE LATITUDE"
    const val KEY_GEOFENCE_LON = "GEOFENCE LONGITUDE"

    object Geometry {
        var MinLatitude = -90.0
        var MaxLatitude = 90.0
        var MinLongitude = -180.0
        var MaxLongitude = 180.0
        var MinRadius = 0.01 // kilometers
        var MaxRadius = 20.0 // kilometers
    }
}


