package sa.moha.sonataos.sys

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GeoState(
    val lat: Double? = null, val lon: Double? = null,
    val speedKmh: Double = 0.0, val bearing: Float? = null, val accuracy: Float = 99f,
    val heading: Float? = null, val hasFix: Boolean = false,
)

/** GPS speed/location via platform LocationManager (no Google Play Services needed on dongles) + magnetometer compass. */
class SensorsHub(private val context: Context) : SensorEventListener, LocationListener {
    private val _state = MutableStateFlow(GeoState())
    val state: StateFlow<GeoState> = _state
    var onFix: ((Location) -> Unit)? = null
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rot = FloatArray(9); private val orient = FloatArray(3)
    private var grav: FloatArray? = null; private var mag: FloatArray? = null

    @SuppressLint("MissingPermission")
    fun start() {
        runCatching {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10_000L, 50f, this, Looper.getMainLooper())
            (lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))?.let { onLocationChanged(it) }
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() { runCatching { lm.removeUpdates(this) }; sm.unregisterListener(this) }

    override fun onLocationChanged(l: Location) {
        val gps = l.provider == LocationManager.GPS_PROVIDER
        _state.value = _state.value.copy(
            lat = l.latitude, lon = l.longitude,
            speedKmh = if (gps && l.hasSpeed()) l.speed * 3.6 else _state.value.speedKmh,
            bearing = if (l.hasBearing()) l.bearing else _state.value.bearing,
            accuracy = l.accuracy, hasFix = true,
        )
        onFix?.invoke(l)
    }

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> grav = e.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> mag = e.values.clone()
        }
        val g = grav ?: return; val m = mag ?: return
        if (SensorManager.getRotationMatrix(rot, null, g, m)) {
            SensorManager.getOrientation(rot, orient)
            val deg = ((Math.toDegrees(orient[0].toDouble()) + 360) % 360).toFloat()
            _state.value = _state.value.copy(heading = deg)
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    @Deprecated("Deprecated in Java") override fun onStatusChanged(p: String?, s: Int, b: android.os.Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
