package co.edu.unal.biketrainer.ui.groups

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.edu.unal.biketrainer.R
import co.edu.unal.biketrainer.R.*
import co.edu.unal.biketrainer.model.Group
import co.edu.unal.biketrainer.model.User
import co.edu.unal.biketrainer.ui.groups.list.GroupsListFragment
import co.edu.unal.biketrainer.ui.routes.GroupsViewModel
import co.edu.unal.biketrainer.ui.routes.RoutesViewModel
import co.edu.unal.biketrainer.utils.Utils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.navigation.core.MapboxNavigation
import kotlinx.android.synthetic.main.fragment_groups.*
import kotlinx.android.synthetic.main.fragment_routes.*
import kotlinx.android.synthetic.main.save_route_dialog.view.*


class GroupsFragment : Fragment() {

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


    private val db = FirebaseFirestore.getInstance()
    private val email by lazy { user?.id.toString() }

    private lateinit var viewModel: GroupsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(layout.fragment_groups, container, false)
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
        groupsLevelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                level = p0?.getItemAtPosition(p2).toString()
                println(level)
                println(email)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

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

        db.collection("groups").add(
            group
        )
        //groupCoordinates.clear()

        var fragment =
            GroupsListFragment.newInstance(user, this.getString(R.string.menu_my_groups_list))
        this.activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.nav_host_fragment, fragment)?.commit()

    }


}