package com.slatetest

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
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

        val etName = (view?.findViewById(R.id.geofence_name) as EditText)
        val etLatitude = (view?.findViewById(R.id.geofence_latitude) as EditText)
        val etLongitudeString = (view?.findViewById(R.id.geofence_longitude) as EditText)
        val etRadiusString = (view?.findViewById(R.id.geofence_radius) as EditText)

        geofence?.let {
            etName.setText(it.name)
            etLatitude.setText(it.latitude.toString())
            etLongitudeString.setText(it.longitude.toString())
            etRadiusString.setText(it.radius.toString())
        }
        etLatitude.hint = String.format(resources.getString(R.string.hint_latitude), Constants.Geometry.MinLatitude, Constants.Geometry.MaxLatitude)
        etLongitudeString.hint = String.format(resources.getString(R.string.hint_longitude), Constants.Geometry.MinLongitude, Constants.Geometry.MaxLongitude)
        etRadiusString.hint = String.format(resources.getString(R.string.hint_radius), Constants.Geometry.MinRadius, Constants.Geometry.MaxRadius)
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
                    isValidData(view) -> {
                        listener?.let {
                            geofence = Geofence().apply {
                                name = (view?.findViewById(R.id.geofence_name) as EditText).text.toString()
                                latitude = java.lang.Double.parseDouble((view?.findViewById(R.id.geofence_latitude) as EditText).text.toString())
                                longitude = java.lang.Double.parseDouble((view?.findViewById(R.id.geofence_longitude) as EditText).text.toString())
                                radius = java.lang.Float.parseFloat((view?.findViewById(R.id.geofence_radius) as EditText).text.toString())
                            }
                            it.onDialogPositiveClick(this@GeoInputDialog, geofence!!)
                            dialog.dismiss()
                        }
                    }
                    else -> Toast.makeText(requireContext(), getString(R.string.toast_validation), Toast.LENGTH_SHORT).show()
                }
            }
        }

        onViewCreated(view!!, savedInstanceState);
        return dialog
    }

    private fun isValidData(view: View?): Boolean {
        var valid = true

        val name = (view?.findViewById(R.id.geofence_name) as EditText).text.toString()
        val latitudeString = (view?.findViewById(R.id.geofence_latitude) as EditText).text.toString()
        val longitudeString = (view?.findViewById(R.id.geofence_longitude) as EditText).text.toString()
        val radiusString = (view?.findViewById(R.id.geofence_radius) as EditText).text.toString()

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