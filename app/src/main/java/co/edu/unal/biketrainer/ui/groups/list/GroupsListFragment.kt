package co.edu.unal.biketrainer.ui.groups.list

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
import co.edu.unal.biketrainer.model.Group
import co.edu.unal.biketrainer.model.User
import co.edu.unal.biketrainer.ui.groups.GroupsFragment
import co.edu.unal.biketrainer.ui.routes.list.GroupsListViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.fragment_groups_list.*
import java.text.SimpleDateFormat

class GroupsListFragment : Fragment(), AdapterView.OnItemClickListener {

    companion object {
        private var user: User? = null
        private var type: String? = null
        fun newInstance(user: User?, type: String?): Fragment {
            val fragment = GroupsListFragment()
            this.user = user
            this.type = type
            return fragment
        }
    }

    private lateinit var viewModel: GroupsListViewModel
    private val db = FirebaseFirestore.getInstance()
    private var items = ArrayList<Group>()

    private val email by lazy { user?.id.toString() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groups_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GroupsListViewModel::class.java)
        chargeList()

        groups.onItemClickListener = this
    }


    private fun chargeList() {
        var collection: Query

        when (type) {
            this.requireContext().getString(R.string.menu_recommended_groups) -> {
                collection = db.collection("groups").whereEqualTo("level", user?.level)
                    .orderBy("created_at", Query.Direction.DESCENDING)
            }
            this.requireContext().getString(R.string.menu_my_groups_list) -> {
                collection = db.collection("groups").whereEqualTo("created_by", user?.id)
                    .orderBy("created_at", Query.Direction.DESCENDING)
            }
            this.requireContext().getString(R.string.menu_near_groups) -> {
                collection =
                    db.collection("groups").orderBy("created_at", Query.Direction.DESCENDING)
            }
            this.requireContext().getString(R.string.menu_top_groups) -> {
                collection =
                    db.collection("groups").orderBy("created_at", Query.Direction.DESCENDING)
            }
            else -> {
                collection =
                    db.collection("groups").orderBy("created_at", Query.Direction.DESCENDING)
            }

        }

        collection.get()
            .addOnSuccessListener { query ->
                query.documents.forEach {
                    val group = Group()
                    group.id = it.id

                    //group.comments = it?.data?.get("comments").toString()
                    group.created_at = (it?.data?.get("created_at") as Timestamp)
                    group.created_by = it.data?.get("created_by").toString()
                    /*var destination = Location(
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
                        (Gson().toJsonTree(it.data?.get("destination")) as JsonObject).get("speed").asFloat*/
                    //group.destination = destination
                    group.level = it.data?.get("level").toString()
                    group.name = it.data?.get("name").toString()
                    /*var origin =
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
                        (Gson().toJsonTree(it.data?.get("origin")) as JsonObject).get("speed").asFloat*/
                    //group.origin = origin
                    /*var groupGroup = ArrayList<Location>()
                    (Gson().toJsonTree(it.data?.get("group")) as JsonArray).forEach { element ->
                        var location = Location(element.asJsonObject.get("provider").asString)
                        location.altitude = element.asJsonObject.get("altitude").asDouble
                        location.longitude = element.asJsonObject.get("longitude").asDouble
                        location.latitude = element.asJsonObject.get("latitude").asDouble
                        location.time = element.asJsonObject.get("time").asLong
                        location.accuracy = element.asJsonObject.get("accuracy").asFloat
                        location.bearing = element.asJsonObject.get("bearing").asFloat
                        location.speed = element.asJsonObject.get("speed").asFloat
                        groupGroup.add(location)
                    }*/
                    group.bikers = it.data?.get("bikers").toString().toInt()
                    //group.gpublic = it.data?.get("gpublic") as Boolean
                    //group.group = groupGroup
                    //group.security = it.data?.get("security").toString().toFloatOrNull()
                    //group.visitors = it.data?.get("visitors").toString().toIntOrNull()
                    items.add(group)
                    var adapter = GroupAdapter(
                        this.requireContext(),
                        android.R.layout.simple_list_item_1,
                        items, this
                    )
                    groups.adapter = adapter
                }
            }
    }


    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        var fragment = GroupsFragment.newInstance(user, items[position])
        this.activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.nav_host_fragment, fragment)?.commit()
        Toast.makeText(this.requireContext(), items[position].name.toString(), Toast.LENGTH_SHORT)
            .show()
    }

    private class GroupAdapter(
        context: Context,
        resource: Int, objects: ArrayList<Group>, groupsListFragment: GroupsListFragment
    ) : ArrayAdapter<Group>(context, resource) {

        var vi: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var objects: ArrayList<Group> = objects
        var groupsListFragment = groupsListFragment

        override fun getCount(): Int {
            return objects.size
        }

        override fun getItem(position: Int): Group? {
            return objects[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = vi.inflate(R.layout.list_item_group, parent, false)

            val name = view.findViewById<TextView>(R.id.tv_name)
            val level = view.findViewById<TextView>(R.id.tv_level)
            val participants = view.findViewById<TextView>(R.id.tv_bikers)
            val owner = view.findViewById<TextView>(R.id.tv_owner)
            val distance = view.findViewById<TextView>(R.id.tv_distance)
            val duration = view.findViewById<TextView>(R.id.tv_duration)


            val deleteButton = view.findViewById<ImageView>(R.id.delete)

            val group = getItem(position) as Group

            name.text = group.name?.capitalize()
            participants.text = group.bikers.toString()
            level.text = group.level
            owner.text = group.created_by
            //duration.text = SimpleDateFormat("HH:mm:ss").format(group.average_duration!!)
            //distance.text = "%.2f km".format(group.origin?.distanceTo(group.destination)?.div(1000))

            if (groupsListFragment.email != group.created_by) {
                deleteButton.visibility = View.INVISIBLE
            }

            deleteButton.setOnClickListener(View.OnClickListener {
                val builder = AlertDialog.Builder(context)
                builder.setMessage("¿Seguro quieres eliminar este grupo?")
                    .setCancelable(false)
                    .setPositiveButton("Si") { dialog, id ->
                        groupsListFragment.db.collection("groups")
                            .document(this.objects[position].id!!).delete()
                        groupsListFragment.items.clear()
                        var ft =
                            groupsListFragment.activity?.supportFragmentManager?.beginTransaction()
                        ft?.detach(groupsListFragment)
                        ft?.attach(groupsListFragment)
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