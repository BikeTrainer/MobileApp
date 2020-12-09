package co.edu.unal.biketrainer.ui.routes.list

import android.app.AlertDialog
import android.content.Context
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
import co.edu.unal.biketrainer.utils.Utils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_routes_list.*
import java.text.SimpleDateFormat


class RoutesListFragment : Fragment(), AdapterView.OnItemClickListener {

    companion object {
        private var user: User? = null
        private var type: String? = null
        private var level_required: String? = null

        fun newInstance(user: User?, type: String?, level_required: String?): Fragment {
            val fragment = RoutesListFragment()
            this.user = user
            this.type = type
            this.level_required = level_required
            return fragment
        }

        fun newInstance(user: User?, type: String?): Fragment {
            return newInstance(user, type, null)
        }
    }


    private lateinit var viewModel: RoutesListViewModel
    private val db = FirebaseFirestore.getInstance()
    private var items = ArrayList<Route>()

    private var mParentListener: OnChildFragmentInteractionListener? = null
    private var refreshInterface: RefreshInterface? = null


    private val email by lazy { user?.id.toString() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_routes_list, container, false)
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
                if (level_required != null) {
                    collection =
                        db.collection("routes").whereEqualTo("level", level_required)
                            .orderBy("created_at", Query.Direction.DESCENDING)
                } else {
                    collection =
                        db.collection("routes").orderBy("created_at", Query.Direction.DESCENDING)
                }
            }

        }

        collection.get()
            .addOnSuccessListener { query ->
                query.documents.forEach {
                    items.add(Utils.getRouteFromDocumentSnap(it))
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
        if (type != this.getString(R.string.groups_list_routes)) {
            Toast.makeText(
                this.requireContext(),
                items[position].name.toString(),
                Toast.LENGTH_SHORT
            ).show()
            var fragment = RoutesFragment.newInstance(user, items[position])
            this.activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.nav_host_fragment, fragment)?.commit()
        } else {
            parent?.alpha = 1f
            view?.alpha = 0.1f
            mParentListener?.messageFromChildToParent(items[position])
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
            distance.text = "%.2f km".format(route.distance?.div(1000))

            if (routesListFragment.email != route.created_by || type == context.getString(R.string.groups_list_routes)) {
                deleteButton.visibility = View.INVISIBLE
            }
            println(type)
            println(context.getString(R.string.groups_list_routes))


            deleteButton.setOnClickListener(View.OnClickListener {
                val builder = AlertDialog.Builder(context)
                builder.setMessage("¿Seguro quieres eliminar esta ruta?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { dialog, id ->
                        routesListFragment.db.collection("routes")
                            .document(this.objects[position].id!!).delete()
                        routesListFragment.items.clear()
//                        var ft =
//                            routesListFragment.activity?.supportFragmentManager?.beginTransaction()
//                        ft?.detach(routesListFragment)
//                        ft?.attach(routesListFragment)
//                        ft?.commit()
                        routesListFragment.refreshInterface?.refreshRoutesFragment()
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

    fun intialiseRefreshInterface(refreshInterface: RefreshInterface?) {
        this.refreshInterface = refreshInterface!!

    }

    fun initialiseChildManagerInterface(mParentListener: OnChildFragmentInteractionListener) {
        this.mParentListener = mParentListener
    }

    interface RefreshInterface {
        fun refreshRoutesFragment()
    }

    interface OnChildFragmentInteractionListener {
        fun messageFromChildToParent(route: Route?)
    }

}