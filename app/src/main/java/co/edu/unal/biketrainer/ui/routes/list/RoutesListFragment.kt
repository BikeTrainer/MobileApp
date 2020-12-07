package co.edu.unal.biketrainer.ui.routes.list

import android.app.AlertDialog
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.edu.unal.biketrainer.R
import co.edu.unal.biketrainer.model.Route
import co.edu.unal.biketrainer.model.User
import co.edu.unal.biketrainer.ui.routes.RoutesFragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.fragment_routes_list.*
import java.text.SimpleDateFormat


class RoutesListFragment : Fragment(), AdapterView.OnItemClickListener {

    companion object {
        private var user: User? = null
        private var type: String? = null
        fun newInstance(user: User?, type: String?): Fragment {
            val fragment = RoutesListFragment()
            this.user = user
            this.type = type
            return fragment
        }
    }

    private lateinit var viewModel: RoutesListViewModel
    private val db = FirebaseFirestore.getInstance()
    private var items = ArrayList<Route>()

    private var mParentListener: OnChildFragmentInteractionListener? = null

    private val email by lazy { user?.id.toString() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_routes_list, container, false)
    }

    interface OnChildFragmentInteractionListener {
        fun messageFromChildToParent(myString: String?)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RoutesListViewModel::class.java)
        chargeList()

        routes.onItemClickListener = this
    }


    private fun chargeList() {
        var collection: Query

        when (type) {
            this.requireContext().getString(R.string.menu_recommended_routes) -> {
                collection = db.collection("routes").whereEqualTo("level", user?.level)
                    .orderBy("created_at", Query.Direction.DESCENDING)
            }
            this.requireContext().getString(R.string.menu_my_routes_list) -> {
                collection = db.collection("routes").whereEqualTo("created_by", user?.id)
                    .orderBy("created_at", Query.Direction.DESCENDING)
            }
            this.requireContext().getString(R.string.menu_near_routes) -> {
                collection =
                    db.collection("routes").orderBy("created_at", Query.Direction.DESCENDING)
            }
            this.requireContext().getString(R.string.menu_top_routes) -> {
                collection =
                    db.collection("routes").orderBy("created_at", Query.Direction.DESCENDING)
            }
            else -> {
                collection =
                    db.collection("routes").orderBy("created_at", Query.Direction.DESCENDING)
            }

        }

        collection.get()
            .addOnSuccessListener { query ->
                query.documents.forEach {
                    val route = Route()
                    route.id = it.id
                    route.average_duration =
                        it?.data?.get("average_duration").toString().toLongOrNull()
                    route.comments = it?.data?.get("comments").toString()
                    route.created_at = (it?.data?.get("created_at") as Timestamp)
                    route.created_by = it.data?.get("created_by").toString()
                    var destination = Location(
                        (Gson().toJsonTree(it.data?.get("destination")) as JsonObject).get("provider").asString
                    )
                    destination.altitude =
                        (Gson().toJsonTree(it.data?.get("destination")) as JsonObject).get("altitude").asDouble
                    destination.longitude =
                        (Gson().toJsonTree(it.data?.get("destination")) as JsonObject).get("longitude").asDouble
                    destination.latitude =
                        (Gson().toJsonTree(it.data?.get("destination")) as JsonObject).get("latitude").asDouble
                    destination.time =
                        (Gson().toJsonTree(it.data?.get("destination")) as JsonObject).get("time").asLong
                    destination.accuracy =
                        (Gson().toJsonTree(it.data?.get("destination")) as JsonObject).get("accuracy").asFloat
                    destination.bearing =
                        (Gson().toJsonTree(it.data?.get("destination")) as JsonObject).get("bearing").asFloat
                    destination.speed =
                        (Gson().toJsonTree(it.data?.get("destination")) as JsonObject).get("speed").asFloat
                    route.destination = destination
                    route.level = it.data?.get("level").toString()
                    route.name = it.data?.get("name").toString()
                    var origin =
                        Location((Gson().toJsonTree(it.data?.get("origin")) as JsonObject).get("provider").asString)
                    origin.altitude =
                        (Gson().toJsonTree(it.data?.get("origin")) as JsonObject).get("altitude").asDouble
                    origin.longitude =
                        (Gson().toJsonTree(it.data?.get("origin")) as JsonObject).get("longitude").asDouble
                    origin.latitude =
                        (Gson().toJsonTree(it.data?.get("origin")) as JsonObject).get("latitude").asDouble
                    origin.time =
                        (Gson().toJsonTree(it.data?.get("origin")) as JsonObject).get("time").asLong
                    origin.accuracy =
                        (Gson().toJsonTree(it.data?.get("origin")) as JsonObject).get("accuracy").asFloat
                    origin.bearing =
                        (Gson().toJsonTree(it.data?.get("origin")) as JsonObject).get("bearing").asFloat
                    origin.speed =
                        (Gson().toJsonTree(it.data?.get("origin")) as JsonObject).get("speed").asFloat
                    route.origin = origin
                    var routeRoute = ArrayList<Location>()
                    (Gson().toJsonTree(it.data?.get("route")) as JsonArray).forEach { element ->
                        var location = Location(element.asJsonObject.get("provider").asString)
                        location.altitude = element.asJsonObject.get("altitude").asDouble
                        location.longitude = element.asJsonObject.get("longitude").asDouble
                        location.latitude = element.asJsonObject.get("latitude").asDouble
                        location.time = element.asJsonObject.get("time").asLong
                        location.accuracy = element.asJsonObject.get("accuracy").asFloat
                        location.bearing = element.asJsonObject.get("bearing").asFloat
                        location.speed = element.asJsonObject.get("speed").asFloat
                        routeRoute.add(location)
                    }
                    route.route = routeRoute
                    route.security = it.data?.get("security").toString().toFloatOrNull()
                    route.visitors = it.data?.get("visitors").toString().toIntOrNull()
                    items.add(route)
                    var adapter = RouteAdapter(
                        this.requireContext(),
                        android.R.layout.simple_list_item_1,
                        items, this
                    )
                    routes.adapter = adapter
                }
            }
    }


    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (type != getContext()?.getString(R.string.groups_list_routes)){
            var fragment = RoutesFragment.newInstance(user, items[position])
            this.activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.nav_host_fragment, fragment)?.commit()
            Toast.makeText(
                this.requireContext(),
                items[position].name.toString(),
                Toast.LENGTH_SHORT
            )
                .show()
        }else{
            println("agregar id al grupo")
            println(items[position].id)

            // Envir mensaje al GroupFragment
            mParentListener?.messageFromChildToParent("Hello, parent. I am your child.");
        }
    }

    private class RouteAdapter(
        context: Context,
        resource: Int, objects: ArrayList<Route>, routesListFragment: RoutesListFragment
    ) : ArrayAdapter<Route>(context, resource) {

        var vi: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var objects: ArrayList<Route> = objects
        var routesListFragment = routesListFragment

        override fun getCount(): Int {
            return objects.size
        }

        override fun getItem(position: Int): Route? {
            return objects[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = vi.inflate(R.layout.list_item_route, parent, false)

            val name = view.findViewById<TextView>(R.id.tv_name)
            val security = view.findViewById<RatingBar>(R.id.tv_security)
            val level = view.findViewById<TextView>(R.id.tv_level)
            val duration = view.findViewById<TextView>(R.id.tv_duration)
            val owner = view.findViewById<TextView>(R.id.tv_owner)
            val distance = view.findViewById<TextView>(R.id.tv_distance)

            val deleteButton = view.findViewById<ImageView>(R.id.delete)

            val route = getItem(position) as Route

            name.text = route.name?.capitalize()
            security.rating = if (route.security != null) route.security!! else 0f
            level.text = route.level
            owner.text = route.created_by
            duration.text = SimpleDateFormat("HH:mm:ss").format(route.average_duration!!)
            distance.text = "%.2f km".format(route.origin?.distanceTo(route.destination)?.div(1000))

            if (routesListFragment.email != route.created_by || type == getContext().getString(R.string.groups_list_routes)) {
                deleteButton.visibility = View.INVISIBLE
            }
            println(type)
            println(getContext().getString(R.string.groups_list_routes))


            deleteButton.setOnClickListener(View.OnClickListener {
                val builder = AlertDialog.Builder(context)
                builder.setMessage("¿Seguro quieres eliminar esta ruta?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { dialog, id ->
                        routesListFragment.db.collection("routes")
                            .document(this.objects[position].id!!).delete()
                        routesListFragment.items.clear()
                        var ft =
                            routesListFragment.activity?.supportFragmentManager?.beginTransaction()
                        ft?.detach(routesListFragment)
                        ft?.attach(routesListFragment)
                        ft?.commit()
                    }
                    .setNegativeButton("No") { dialog, id ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()


            })

            return view
        }
    }


}