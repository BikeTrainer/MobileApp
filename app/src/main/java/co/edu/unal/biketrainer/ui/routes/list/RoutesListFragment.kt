package co.edu.unal.biketrainer.ui.routes.list

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.edu.unal.biketrainer.R
import co.edu.unal.biketrainer.model.Route
import co.edu.unal.biketrainer.ui.routes.RoutesFragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.fragment_routes_list.*

class RoutesListFragment : Fragment(), AdapterView.OnItemClickListener {

    companion object {
        const val ARGS_NAME = "email"
        fun newInstance(name: String): Fragment {
            val args = Bundle()
            args.putString(ARGS_NAME, name)
            val fragment = RoutesListFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var viewModel: RoutesListViewModel
    private val db = FirebaseFirestore.getInstance()
    private var items = ArrayList<Route>()

    private val email by lazy { arguments?.getString(RoutesFragment.ARGS_NAME) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_routes_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RoutesListViewModel::class.java)
        db.collection("routes").whereEqualTo("created_by", email).get()
            .addOnSuccessListener { query ->
                query.documents.forEach {
                    val route = Route()
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
                        items
                    )
                    routes.adapter = adapter
                }
            }

        routes.onItemClickListener = this

    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        var fragment = RoutesFragment.newInstance(email.toString(), items[position])
        this.activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.nav_host_fragment, fragment)?.commit()
        Toast.makeText(this.requireContext(), items[position].toString(), Toast.LENGTH_SHORT)
            .show()
    }

    private class RouteAdapter(
        context: Context,
        resource: Int, objects: ArrayList<Route>
    ) : ArrayAdapter<Route>(context, resource) {

        var vi: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var objects: ArrayList<Route> = objects

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
            val security = view.findViewById<TextView>(R.id.tv_security)
            val level = view.findViewById<TextView>(R.id.tv_level)

            val route = getItem(position) as Route

            name.text = route.name
            security.text = route.security.toString()
            level.text = route.level

            return view
        }
    }


}