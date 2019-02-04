package com.slatetest

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.slatetest.model.Geofence
import kotlinx.android.synthetic.main.dialog_geoinput.*


class GeoInputDialog : DialogFragment() {

    var geofence: Geofence? = null

    private var listener: AddGeofenceListener? = null

    fun setListener(listener: AddGeofenceListener) {
        this.listener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        geofence?.let {
            geofence_name.setText(it.name)
            geofence_latitude.setText(it.latitude.toString())
            geofence_longitude.setText(it.longitude.toString())
            geofence_radius.setText(it.radius.toString())
        }
        geofence_latitude.hint = String.format(resources.getString(R.string.hint_latitude), Constants.Geometry.MinLatitude, Constants.Geometry.MaxLatitude)
        geofence_longitude.hint = String.format(resources.getString(R.string.hint_longitude), Constants.Geometry.MinLongitude, Constants.Geometry.MaxLongitude)
        geofence_radius.hint = String.format(resources.getString(R.string.hint_radius), Constants.Geometry.MinRadius, Constants.Geometry.MaxRadius)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity?.layoutInflater
        val view = inflater?.inflate(R.layout.dialog_geoinput, null)

        val dialog = AlertDialog.Builder(activity).setView(view)
                .setPositiveButton(R.string.Add, null)
                .setNegativeButton(R.string.Cancel) { dialog, id ->
                    this@GeoInputDialog.dialog?.cancel()
                    listener?.onDialogNegativeClick(this@GeoInputDialog)
                }
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                when {
                    isValidData() -> {
                        listener?.let {
                            val geofence = Geofence().apply {
                                name = (geofence_latitude.text.toString())
                                latitude = (java.lang.Double.parseDouble(geofence_latitude.text.toString()))
                                longitude = (java.lang.Double.parseDouble(geofence_longitude.text.toString()))
                                radius = (java.lang.Float.parseFloat(geofence_radius.text.toString()))
                            }
                            it.onDialogPositiveClick(this@GeoInputDialog, geofence)
                            dialog.dismiss()
                        }
                    }
                    else -> Toast.makeText(requireContext(), getString(R.string.toast_validation), Toast.LENGTH_SHORT).show()
                }
            }
        }
        return dialog
    }

    private fun isValidData(): Boolean {
        var valid = true

        val name = geofence_name.text.toString()
        val latitudeString = geofence_latitude.text.toString()
        val longitudeString = geofence_longitude.text.toString()
        val radiusString = geofence_radius.text.toString()

        if (name.isBlank() || latitudeString.isBlank() || longitudeString.isBlank() || radiusString.isBlank()) {
            valid = false
        } else {
            val latitude = latitudeString.toDouble()
            val longitude = longitudeString.toDouble()
            val radius = radiusString.toFloat()
            if (latitude < Constants.Geometry.MinLatitude || latitude > Constants.Geometry.MaxLatitude
                    || longitude < Constants.Geometry.MinLongitude || longitude > Constants.Geometry.MaxLongitude
                    || radius < Constants.Geometry.MinRadius || radius > Constants.Geometry.MaxRadius) {
                valid = false
            }
        }
        return valid
    }

    interface AddGeofenceListener {
        fun onDialogPositiveClick(dialog: DialogFragment, geofence: Geofence)

        fun onDialogNegativeClick(dialog: DialogFragment)
    }
}