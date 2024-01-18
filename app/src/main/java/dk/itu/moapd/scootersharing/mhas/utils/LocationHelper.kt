package dk.itu.moapd.scootersharing.mhas.utils


import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.widget.TextView
import java.util.*

class LocationHelper() {

    private fun Address.toAddressString() : String {
        val address = this
        val stringBuilder = StringBuilder()
        stringBuilder.apply {
            append(address.getAddressLine(0))
        }

        return stringBuilder.toString()
    }

    fun setAddress(context : Context, latitude : Double, longitude : Double, textView: TextView) {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= 33) {
            val geocodeListener = Geocoder.GeocodeListener { addresses ->
                addresses.firstOrNull()?.toAddressString()?.let { address ->
                    textView.text = address
                }
            }
            geocoder.getFromLocation(latitude,longitude,1,geocodeListener)

        } else
            geocoder.getFromLocation(latitude,longitude,1)?.let { addresses ->
                addresses.firstOrNull()?.toAddressString()?.let { address ->
                    textView.text = address
                }
            }
    }
}
