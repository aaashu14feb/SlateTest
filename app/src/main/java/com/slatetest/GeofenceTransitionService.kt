package com.slatetest

import android.app.IntentService
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.slatetest.Constants.BROADCAST_ACTION
import com.slatetest.Constants.BROADCAST_TASK_ENTER
import java.util.*

class GeofenceTransitionService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        // Handling errors
        if (geofencingEvent.hasError()) {
            val errorMsg = getErrorString(geofencingEvent.getErrorCode())
            Log.e(TAG, errorMsg)
            return
        }

        val geoFenceTransition = geofencingEvent.getGeofenceTransition()
        // Check if the transition type is of interest
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Get the geofence that were triggered
            val triggeringGeofences = geofencingEvent.getTriggeringGeofences()

            val geofenceTransitionDetails = getGeofenceTransitionDetails(geoFenceTransition, triggeringGeofences)
            Log.d(TAG, geofenceTransitionDetails)

            val intentBroadcast = Intent(BROADCAST_ACTION)
            intentBroadcast.putExtra(BROADCAST_TASK_ENTER, getGeofenceStatus(geoFenceTransition))
            sendBroadcast(intentBroadcast)
        }
    }


    private fun getGeofenceTransitionDetails(geoFenceTransition: Int, triggeringGeofences: List<Geofence>): String {
        // get the ID of each geofence triggered
        val triggeringGeofencesList = ArrayList<String>()
        for (geofence in triggeringGeofences) {
            triggeringGeofencesList.add(geofence.getRequestId())
        }

        var status: String? = null
        when (geoFenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> status = "Entering "
            Geofence.GEOFENCE_TRANSITION_EXIT -> status = "Exiting "
        }
        return status!! + TextUtils.join(", ", triggeringGeofencesList)
    }

    private fun getGeofenceStatus(geoFenceTransition: Int): Boolean {
        var status = false
        when (geoFenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> status = true
            Geofence.GEOFENCE_TRANSITION_EXIT -> status = false
        }
        return status
    }

    companion object {

        private val TAG = GeofenceTransitionService::class.java.simpleName

        private fun getErrorString(errorCode: Int): String {
            return when (errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "GeoFence not available"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Too many GeoFences"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Too many pending intents"
                else -> "Unknown error."
            }
        }
    }
}