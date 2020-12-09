package co.edu.unal.biketrainer.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.edu.unal.biketrainer.R
import co.edu.unal.biketrainer.model.User
import co.edu.unal.biketrainer.ui.groups.list.GroupsListFragment
import co.edu.unal.biketrainer.ui.profile.AboutYouFragment
import co.edu.unal.biketrainer.ui.routes.list.RoutesListFragment
import com.google.firebase.firestore.FirebaseFirestore


class HomeFragment : Fragment(), RoutesListFragment.RefreshInterface,
    GroupsListFragment.RefreshInterface {


    companion object {
        private var user: User? = null
        fun newInstance(user: User?): Fragment {
            val fragment = HomeFragment()
            this.user = user
            return fragment
        }
    }

    private lateinit var viewModel: HomeViewModel
    private lateinit var db: FirebaseFirestore

    var refreshRoutesInterface: RoutesListFragment.RefreshInterface? = null
    var refreshGroupsInterface: GroupsListFragment.RefreshInterface? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var root = inflater.inflate(R.layout.fragment_home, container, false)
        refreshGroupsInterface = this
        refreshRoutesInterface = this
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        launchChildAboutYouFragment()
        launchChildRoutesFragment()
        launchChildGroupsFragment()
    }

    private fun launchChildAboutYouFragment() {
        val aboutYouFragment = AboutYouFragment.newInstance(
            user
        ) as AboutYouFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.child_info_fragment_container, aboutYouFragment).commit()
        // intialising the RefreshInterface object
//        childRoutesFragment.intialiseRefreshInterface(refreshRoutesInterface)
        // calling the child fragment
        requireActivity().supportFragmentManager.beginTransaction()
            .add(R.id.child_info_fragment_container, aboutYouFragment)
            .addToBackStack(null).commit()
    }

    override fun refreshRoutesFragment() {
        if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
            requireActivity().supportFragmentManager.popBackStack()
        }
        launchChildRoutesFragment()
    }

    override fun refreshGroupsFragment() {
        if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
            requireActivity().supportFragmentManager.popBackStack()
        }
        launchChildGroupsFragment()
    }


    private fun launchChildRoutesFragment() {
        // creating the object for child fragment
        val childRoutesFragment = RoutesListFragment.newInstance(
            user,
            this.getString(R.string.menu_recommended_routes)
        ) as RoutesListFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.child_routes_fragment_container, childRoutesFragment).commit()
        // intialising the RefreshInterface object
        childRoutesFragment.intialiseRefreshInterface(refreshRoutesInterface)
        // calling the child fragment
        requireActivity().supportFragmentManager.beginTransaction()
            .add(R.id.child_routes_fragment_container, childRoutesFragment)
            .addToBackStack(null).commit()
    }

    private fun launchChildGroupsFragment() {
        val childGroupsFragment = GroupsListFragment.newInstance(
            user,
            this.getString(R.string.menu_recommended_routes)
        ) as GroupsListFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.child_group_fragment_container, childGroupsFragment).commit()
        // intialising the RefreshInterface object
        childGroupsFragment.intialiseRefreshInterface(refreshGroupsInterface)
        // calling the child fragment
        requireActivity().supportFragmentManager.beginTransaction()
            .add(R.id.child_group_fragment_container, childGroupsFragment)
            .addToBackStack(null).commit()
    }
}