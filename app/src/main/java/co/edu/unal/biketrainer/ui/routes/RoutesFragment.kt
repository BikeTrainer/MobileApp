package co.edu.unal.biketrainer.ui.routes

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.edu.unal.biketrainer.R
import co.edu.unal.biketrainer.R.*
import co.edu.unal.biketrainer.model.Group
import co.edu.unal.biketrainer.model.Route
import co.edu.unal.biketrainer.model.User
import co.edu.unal.biketrainer.ui.routes.list.RoutesListFragment
import co.edu.unal.biketrainer.utils.Utils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.core.MapboxNavigation
import kotlinx.android.synthetic.main.fragment_routes.*
import kotlinx.android.synthetic.main.save_route_dialog.view.*
import java.lang.ref.WeakReference


class RoutesFragment : Fragment(), OnMapReadyCallback, PermissionsListener,
    OnLocationCameraTransitionListener {


    companion object {
        private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME: Long = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
        private var user: User? = null
        private var group: Group? = null
        private var staticRoute: Route? = null

        fun newInstance(user: User?): Fragment {
            val fragment = RoutesFragment()
            this.user = user
            return fragment
        }

        fun newInstance(user: User?, route: Route): Fragment {
            staticRoute = route
            return newInstance(user)
        }

        fun newInstance(group: Group?): Fragment {
            val fragment = RoutesFragment()
            this.staticRoute = group?.route
            return fragment
        }
    }

    private lateinit var routeCoordinates: ArrayList<Location>

    private lateinit var locationEngine: LocationEngine
    private lateinit var mapboxNavigation: MapboxNavigation
    private var mapboxMap: MapboxMap? = null
    private var permissionsManager: PermissionsManager = PermissionsManager(this)


    private var comments: String? = null
    private var destination: Location? = null
    private var level: String? = null
    private var name: String? = null
    private var origin: Location? = null
    private var securityLevel: Float? = null
    private var distance = 0f


    private val db = FirebaseFirestore.getInstance()
    private val email by lazy { user?.id.toString() }

    private val callback: RoutesFragmentLocationCallback =
        RoutesFragmentLocationCallback(this, false)

    private lateinit var viewModel: RoutesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layout.fragment_routes, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RoutesViewModel::class.java)
        map_view.onCreate(savedInstanceState)

        map_view.getMapAsync(this)

        val mapboxNavigationOptions = this.context?.let {
            MapboxNavigation
                .defaultNavigationOptionsBuilder(
                    it,
                    Utils.getMapboxAccessToken(this.requireContext())
                )
                .build()
        }
        mapboxNavigation = mapboxNavigationOptions?.let { MapboxNavigation(it) }!!
        startRecording.visibility = View.VISIBLE
        initListeners()
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        startRecording.setOnClickListener {
            routeCoordinates = ArrayList<Location>()
            mapboxNavigation.let {
                callback.trackingMode = true
                it.startTripSession()
            }
            origin = mapboxMap?.locationComponent?.lastKnownLocation
            mapboxMap?.locationComponent?.setCameraMode(CameraMode.TRACKING_GPS_NORTH, this)
            startRecording.visibility = View.GONE
            stopRecording.visibility = View.VISIBLE
        }

        stopRecording.setOnClickListener {

            if (locationEngine != null) {
                locationEngine.removeLocationUpdates(callback)
            }
            destination = mapboxMap?.locationComponent?.lastKnownLocation
            stopRecording.visibility = View.GONE
            println("stop : " + locationEngine)

            // Mostrar dialogo
            val dialogView = layoutInflater.inflate(layout.save_route_dialog, null)
            val dialog = AlertDialog.Builder(stopRecording.context).setView(dialogView)
                .setTitle("Guardar Ruta")

            val alertDialog = dialog.show()

            dialogView.dialogSaveButton.setOnClickListener {
                println("guardar : " + locationEngine)
                name = dialogView.dialogSaveName.text.toString().capitalize()
                comments = dialogView.dialogSaveComment.text.toString()
                if (securityLevel == null) {
                    Snackbar.make(
                        it,
                        "Debes seleccionar un nivel de seguridad",
                        Snackbar.LENGTH_SHORT
                    )
                } else if (comments == null) {
                    Snackbar.make(dialogView, "Debes comentar la ruta", Snackbar.LENGTH_SHORT)
                } else if (name == null) {
                    Snackbar.make(
                        it,
                        "Debes escribir un nombre para esta ruta",
                        Snackbar.LENGTH_SHORT
                    )
                } else if (level == null) {
                    Snackbar.make(
                        it,
                        "Debes seleccionar el nivel de la ruta",
                        Snackbar.LENGTH_SHORT
                    )
                } else {
                    saveRoute()
                    alertDialog.dismiss()
                }
            }

            dialogView.dialogCancelButton.setOnClickListener {
                alertDialog.dismiss()
            }

            dialogView.ratingBarSaveRoute.setOnRatingBarChangeListener { ratingBar, fl, b ->
                securityLevel = fl
                println(securityLevel)
            }

            dialogView.spinnerSaveRoute.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        level = p0?.getItemAtPosition(p2).toString()
                        println(level)
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        TODO("Not yet implemented")
                    }
                }
        }
    }

    private fun saveRoute() {
        //TODO: Save on firebase route
        var route = Route()
        route.name = name
        route.level = level
        route.created_by = email
        route.created_at = Timestamp.now()
        route.origin = origin
        route.destination = destination
        route.average_duration = destination?.time!!.minus(origin?.time!!)
        route.security = securityLevel
        route.comments = comments
        route.visitors = 0
        route.route = routeCoordinates
        route.distance = distance

        db.collection("routes").add(
            route
        )
        routeCoordinates.clear()

        var fragment =
            RoutesListFragment.newInstance(user, this.getString(R.string.menu_my_routes_list))
        this.activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.nav_host_fragment, fragment)?.commit()
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

    @SuppressLint("ResourceAsColor", "MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.LIGHT, Style.OnStyleLoaded {
            this.mapboxMap = mapboxMap
            enableLocationComponent(it)

            if (staticRoute != null) {

                val latLngBounds = LatLngBounds.Builder()
                    .include(
                        LatLng(
                            staticRoute!!.origin!!.latitude,
                            staticRoute!!.origin!!.longitude
                        )
                    )
                    .include(
                        LatLng(
                            staticRoute!!.destination!!.latitude,
                            staticRoute!!.destination!!.longitude
                        )
                    )
                    .include(
                        LatLng(
                            mapboxMap.locationComponent.lastKnownLocation?.latitude!!,
                            mapboxMap.locationComponent.lastKnownLocation?.longitude!!
                        )
                    )
                    .build()

                mapboxMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(latLngBounds, 200, 200, 200, 200)

                )

                mapboxMap.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            staticRoute!!.origin!!.latitude,
                            staticRoute!!.origin!!.longitude
                        )
                    ).title("Origen: " + staticRoute!!.name)
                )

                mapboxMap.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            staticRoute!!.destination!!.latitude,
                            staticRoute!!.destination!!.longitude
                        )
                    ).title("Destino: " + staticRoute!!.name)
                )

                it.addSource(
                    GeoJsonSource(
                        "line-source",
                        FeatureCollection.fromFeatures(
                            arrayOf<Feature>(
                                Feature.fromGeometry(
                                    staticRoute!!.route?.let { it1 ->
                                        LineString.fromLngLats(it1.map { location ->
                                            Point.fromLngLat(location.longitude, location.latitude)
                                        })
                                    }
                                )
                            )
                        )
                    )
                )

                it.addLayer(
                    LineLayer("linelayer", "line-source").withProperties(
                        PropertyFactory.lineWidth(10f),
                        PropertyFactory.lineColor(
                            ContextCompat.getColor(
                                this.requireContext(),
                                color.colorRoute
                            )
                        )
                    )
                )
            } else {
                initLocationEngine()
            }
        })


    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(context)) {

            val customLocationComponentOptions = LocationComponentOptions.builder(requireContext())
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(requireContext(), color.mapboxGreen))
                .pulseEnabled(true)
                .pulseMaxRadius(3.0f)
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
        staticRoute = null
        if (this::locationEngine.isInitialized && locationEngine != null) {
            locationEngine.removeLocationUpdates(callback)
        }
        map_view?.onDestroy()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            this.requireContext(),
            string.user_location_permission_explanation,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap?.style!!)
        } else {
            Toast.makeText(
                this.requireContext(),
                string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private class RoutesFragmentLocationCallback(
        routesFragment: RoutesFragment,
        tracingkMode: Boolean,
    ) :
        LocationEngineCallback<LocationEngineResult> {

        var trackingMode = tracingkMode
        private val activityRef = WeakReference(routesFragment)

        @SuppressLint("ResourceAsColor")
        override fun onSuccess(result: LocationEngineResult?) {
            var activity = activityRef.get()
            if (activity != null) {
                var location: Location? = result?.lastLocation ?: return
                if (trackingMode) {
                    if (!activity.routeCoordinates.isEmpty()) {
                        var lastLocation =
                            activity.routeCoordinates[activity.routeCoordinates.size - 1]
                        if (location?.latitude != lastLocation.latitude && location?.longitude != lastLocation.longitude) {
                            activity.routeCoordinates.add(location!!)
                            activity.distance += location.distanceTo(lastLocation)
                            Toast.makeText(
                                activity.requireContext(),
                                "Has recorrido " + activity.distance.toString(),
                                Toast.LENGTH_SHORT
                            )
                        }
                    } else {
                        activity.routeCoordinates.add(location!!)
                    }
                }


                if (activity.mapboxMap != null && result.lastLocation != null) {
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

                    if (trackingMode) {
                        map.setStyle(Style.LIGHT, Style.OnStyleLoaded {
                            it.addSource(
                                GeoJsonSource(
                                    "line-source",
                                    FeatureCollection.fromFeatures(
                                        arrayOf<Feature>(
                                            Feature.fromGeometry(
                                                LineString.fromLngLats(activity.routeCoordinates.map { location ->
                                                    Point.fromLngLat(
                                                        location.longitude,
                                                        location.latitude
                                                    )
                                                })
                                            )
                                        )
                                    )
                                )
                            )

                            it.addLayer(
                                LineLayer("linelayer", "line-source").withProperties(
                                    PropertyFactory.lineWidth(10f),
                                    PropertyFactory.lineColor(
                                        ContextCompat.getColor(
                                            activity.requireContext(),
                                            color.colorBikeTrainer
                                        )
                                    )
                                )
                            )
                        })
                    }

                    map.locationComponent.forceLocationUpdate(location)
                }
            }

        }

        override fun onFailure(exception: Exception) {
            Log.d("Location Change ", exception.localizedMessage)
        }

    }

    override fun onLocationCameraTransitionFinished(cameraMode: Int) {
        if (cameraMode != CameraMode.NONE) {
            mapboxMap?.locationComponent?.zoomWhileTracking(
                15.0,
                750,
                object : MapboxMap.CancelableCallback {
                    override fun onCancel() {
                        mapboxMap?.locationComponent?.tiltWhileTracking(0.0)
                    }

                    override fun onFinish() {
                        mapboxMap?.locationComponent?.tiltWhileTracking(45.0)
                    }

                })
        } else {
            mapboxMap?.easeCamera(CameraUpdateFactory.tiltTo(0.0))
        }
    }

    override fun onLocationCameraTransitionCanceled(cameraMode: Int) {
        mapboxMap?.locationComponent?.apply {
            this.tiltWhileTracking(45.0)
        }
    }

}