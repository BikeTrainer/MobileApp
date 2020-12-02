package co.edu.unal.biketrainer.ui.routes

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.edu.unal.biketrainer.R
import co.edu.unal.biketrainer.ui.home.HomeFragment
import co.edu.unal.biketrainer.utils.Utils
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.fragment_routes.*
import kotlinx.android.synthetic.main.save_route_dialog.*
import kotlinx.android.synthetic.main.save_route_dialog.view.*
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class RoutesFragment: Fragment(), OnMapReadyCallback, PermissionsListener {


    companion object {
        private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME: Long = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
        const val ARGS_NAME = "email"

        fun newInstance(name: String): Fragment{
            val args = Bundle()
            args.putString(ARGS_NAME, name)
            val fragment = RoutesFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var routeCoordinates: ArrayList<Point>

    private lateinit var locationEngine: LocationEngine
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private var mapboxMap: MapboxMap? = null
    private var permissionsManager: PermissionsManager = PermissionsManager(this)


    private var comments: String? = null
    private var destination: Location? = null
    private var level: String? = null
    private var name: String? = null
    private var origin: Location? = null
    private var securityLevel: Float? = null


    private val db = FirebaseFirestore.getInstance()
    private val email by lazy { arguments?.getString(ARGS_NAME) }

    private val callback: RoutesFragmentLocationCallback = RoutesFragmentLocationCallback(this)

    private lateinit var viewModel: RoutesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_routes, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RoutesViewModel::class.java)
        map_view.onCreate(savedInstanceState)

        map_view.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(
                requireContext(), Utils.getMapboxAccessToken(
                    requireContext()
                )
            )
            .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions)
        startRecording.visibility = View.VISIBLE
        initListeners()
        Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        startRecording.setOnClickListener {
            mapboxNavigation.let {
                initLocationEngine()
                it.startTripSession()
            }
            origin = mapboxMap?.locationComponent?.lastKnownLocation
            startRecording.visibility = View.GONE
            stopRecording.visibility = View.VISIBLE
        }

        stopRecording.setOnClickListener {

            if (locationEngine != null) {
                locationEngine.removeLocationUpdates(callback)
            }
            destination = mapboxMap?.locationComponent?.lastKnownLocation
            stopRecording.visibility = View.GONE
            saveRoute.visibility = View.VISIBLE
            println("stop : "+locationEngine)

            // Mostrar dialogo
            val dialogView = layoutInflater.inflate(R.layout.save_route_dialog, null)
            val dialog = AlertDialog.Builder(stopRecording.context).setView(dialogView).setTitle("Guardar Ruta")

            val alertDialog = dialog.show()

            dialogView.dialogSaveButton.setOnClickListener{
                println("guardar : "+locationEngine)
                alertDialog.dismiss()
                name = dialogView.dialogSaveName.text.toString()
                comments = dialogView.dialogSaveComment.text.toString()
                level = dialogView.dialogSaveLevel.text.toString()
                
                saveRoute()

            }

            dialogView.dialogCancelButton.setOnClickListener{
                alertDialog.dismiss()
            }


        }

        saveRoute.setOnClickListener {
            //TODO: Save on firebase route
            var route = HashMap<String, String>()
            val routeJson = Gson().toJson(routeCoordinates)
            var duration = Calendar.getInstance()
            duration.timeInMillis = destination?.time!!.minus(origin?.time!!)
            route.put(email.toString(), routeJson)
            db.collection("routes").add(
                hashMapOf(
                    "average_duration" to SimpleDateFormat("HH:mm:ss").format(duration.time),
                    "comments" to comments,
                    "created_by" to email.toString(),
                    "created_at" to com.google.firebase.Timestamp.now(),
                    "destination" to destination,
                    "route" to routeJson,
                    "level" to level,
                    "name" to name,
                    "origin" to origin,
                    "security_level" to securityLevel,
                    "total_visits" to ""
                )
            )
            routeCoordinates.clear()
        }

    }

    private fun saveRoute(){
        //TODO: Save on firebase route
        var route = HashMap<String, String>()
        val routeJson = Gson().toJson(routeCoordinates)
        route.put(email.toString(), routeJson )
        db.collection("routes").add(hashMapOf("average_duration" to (destination?.time?.minus(
            origin?.time!!
        )),
            "comments" to comments,
            "created_by" to email.toString(),
            "destination" to destination,
            "route" to routeJson,
            "level" to level,
            "name" to name,
            "origin" to origin,
            "security_level" to securityLevel,
            "total_visits" to ""
        ))

    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this.requireContext())
        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
            .build()

        locationEngine.requestLocationUpdates(request, callback, Looper.getMainLooper())
        locationEngine.getLastLocation(callback)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.LIGHT, Style.OnStyleLoaded {
            this.mapboxMap = mapboxMap
            enableLocationComponent(it)
        })

        routeCoordinates = ArrayList<Point>()
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(context)) {

            val customLocationComponentOptions = LocationComponentOptions.builder(requireContext())
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(requireContext(), R.color.mapboxGreen))
                .build()

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(
                requireContext(), loadedMapStyle
            ).locationComponentOptions(customLocationComponentOptions)
                .build()

            mapboxMap?.locationComponent?.apply {

                activateLocationComponent(locationComponentActivationOptions)

                isLocationComponentEnabled = true

                cameraMode = CameraMode.TRACKING

                renderMode = RenderMode.COMPASS

            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this.activity)
        }
    }
    override fun onStart() {
        super.onStart()
        map_view?.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view?.onPause()
    }

    override fun onStop() {
        super.onStop()
        map_view?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map_view?.onDestroy()
    }


    override fun onLowMemory() {
        super.onLowMemory()
        map_view?.onLowMemory()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback)
        }
        map_view?.onDestroy()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            this.requireContext(),
            R.string.user_location_permission_explanation,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap?.style!!)
        } else {
            Toast.makeText(
                this.requireContext(),
                R.string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
        }
    }
    private class RoutesFragmentLocationCallback(routesFragment: RoutesFragment) :LocationEngineCallback<LocationEngineResult>{

        private val activityRef = WeakReference(routesFragment)

        @SuppressLint("ResourceAsColor")
        override fun onSuccess(result: LocationEngineResult?) {
            var activity = activityRef.get()
            if(activity != null){
                var location: Location? = result?.lastLocation ?: return
                if(!activity.routeCoordinates.isEmpty()){
                    var lastLocation = activity.routeCoordinates[activity.routeCoordinates.size - 1]
                    if( location?.latitude  != lastLocation.latitude() && location?.longitude != lastLocation.longitude()){
                        activity.routeCoordinates.add(
                            Point.fromLngLat(
                                location?.longitude!!,
                                location.latitude
                            )
                        )
                    }
                } else {
                    activity.routeCoordinates.add(
                        Point.fromLngLat(
                            location?.longitude!!,
                            location.latitude
                        )
                    )
                }
                //TODO: Save location
                if(activity.mapboxMap != null && result.lastLocation != null) {
                    val map = activity.mapboxMap
                    map!!.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(
                                    LatLng(
                                        result.lastLocation!!.latitude,
                                        result.lastLocation!!.longitude
                                    )
                                )
                                .zoom(15.0)
                                .build()
                        ), 4000
                    )

                    map.setStyle(Style.LIGHT, Style.OnStyleLoaded {
                        it.addSource(
                            GeoJsonSource(
                                "line-source",
                                FeatureCollection.fromFeatures(
                                    arrayOf<Feature>(
                                        Feature.fromGeometry(
                                            LineString.fromLngLats(activity.routeCoordinates)
                                        )
                                    )
                                )
                            )
                        )

                        it.addLayer(
                            LineLayer("linelayer", "line-source").withProperties(
                                PropertyFactory.lineWidth(10f),
                                PropertyFactory.lineColor(R.color.colorBikeTrainer)
                            )
                        )
                    })
                    map.locationComponent.forceLocationUpdate(location)
                }
            }

        }

        override fun onFailure(exception: Exception) {
            Log.d("Location Change ", exception.localizedMessage)
        }

    }

    fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage("R.string.dialog_fire_missiles")
                .setPositiveButton("R.string.fire",
                    DialogInterface.OnClickListener { dialog, id ->
                        // FIRE ZE MISSILES!
                    })
                .setNegativeButton("R.string.cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        // User cancelled the dialog
                    })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}