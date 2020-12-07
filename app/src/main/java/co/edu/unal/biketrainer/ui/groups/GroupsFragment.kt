package co.edu.unal.biketrainer.ui.groups

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.edu.unal.biketrainer.R
import co.edu.unal.biketrainer.R.layout
import co.edu.unal.biketrainer.model.Group
import co.edu.unal.biketrainer.model.User
import co.edu.unal.biketrainer.ui.groups.list.GroupsListFragment
import co.edu.unal.biketrainer.ui.routes.GroupsViewModel
import co.edu.unal.biketrainer.ui.routes.list.RoutesListFragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_groups.*


class GroupsFragment : Fragment(), RoutesListFragment.OnChildFragmentInteractionListener {

    companion object {
        private var user: User? = null
        private var staticGroup: Group? = null

        fun newInstance(user: User?): Fragment {
            val fragment = GroupsFragment()
            this.user = user
            return fragment
        }

        fun newInstance(user: User?, group: Group): Fragment {
            staticGroup = group
            return newInstance(user)
        }
    }

    private lateinit var groupCoordinates: ArrayList<Location>

    private var name: String? = null
    private var level: String? = null
    private var bikers: Int? = 2
    private var gpublic: Boolean? = false
    private var id_route: String? = null


    private val db = FirebaseFirestore.getInstance()
    private val email by lazy { user?.id.toString() }

    private lateinit var viewModel: GroupsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(layout.fragment_groups, container, false)
    }

    // Escuchar mensaje desde el fragmente de RoutesList
    override fun messageFromChildToParent(myString: String?) {
        Log.i("TAG", myString!!)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GroupsViewModel::class.java)

        groupsSaveButton.setOnClickListener {
            name = groupsNameEditText.text.toString()
            println(name)

            if (groupPublicSwitch.isChecked){
                gpublic = true
            }
            saveGroup()
        }



        // Carga el fragmente de la lista de rutas
        val fragment =
            RoutesListFragment.newInstance(user, this.getString(R.string.groups_list_routes))
        this.activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.nav_routes_fragment, fragment)?.commit()


        // Lista desplegable de la dificultad
        groupsLevelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                level = p0?.getItemAtPosition(p2).toString()
                println(level)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        // Lista desplegable del numero de participantes
        groupsBikersSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                bikers = p0?.getItemAtPosition(p2).toString().toInt()
                println(bikers)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

    }

    private fun saveGroup() {
        //TODO: Save on firebase group
        var group = Group()
        group.name = name
        group.created_by = email
        group.created_at = Timestamp.now()
        group.level = level
        group.bikers = bikers
        group.gpublic = gpublic!!


        // Enviar objeto a firebase en la collection groups
        db.collection("groups").add(
            group
        )

        /*db.collection("groups").get().addOnSuccessListener{ query ->
            query.documents.forEach {
                println("id " + it.id)
                println("id ref id " + it.reference.id)
            }
        }
        val ref = db.collection("group").document()
        val myId = ref.id
        println("id mid " + myId)*/

        //groupCoordinates.clear()

        var fragment =
            GroupsListFragment.newInstance(user, this.getString(R.string.menu_my_groups_list))
        this.activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.nav_host_fragment, fragment)?.commit()

    }


}