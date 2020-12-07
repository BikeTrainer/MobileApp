package co.edu.unal.biketrainer.model

import com.google.firebase.Timestamp

class Group (
    var id: String? = null,
    var name: String? = null,
    var level: String? = null,
    var bikers: Int? = 0,
    var created_by: String? = null,
    var created_at: Timestamp? = null,
    var gpublic: Boolean = false,
    var id_route: String? = null
)