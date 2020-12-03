package co.edu.unal.biketrainer.model

import android.location.Location
import com.google.firebase.Timestamp

data class Route(
    var name: String? = null,
    var comments: String? = null,
    var level: String? = null,
    var security: Float? = null,
    var origin: Location? = null,
    var destination: Location? = null,
    var created_at: Timestamp? = null,
    var created_by: String? = null,
    var average_duration: Long? = null,
    var route: List<Location>? = null,
    var visitors: Int? = null
)

