package co.edu.unal.biketrainer.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.edu.unal.biketrainer.R
import co.edu.unal.biketrainer.model.User
import co.edu.unal.biketrainer.ui.routes.list.RoutesListFragment
import com.google.firebase.firestore.FirebaseFirestore


class HomeFragment : Fragment(), RoutesListFragment.RefreshInterface {


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

    var refreshInterface: RoutesListFragment.RefreshInterface? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var root = inflater.inflate(R.layout.fragment_home, container, false)
        refreshInterface = this
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        launchChildFragment()
    }

    override fun refreshFragment() {
        if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
            requireActivity().supportFragmentManager.popBackStack()
        }
        launchChildFragment()
    }


    private fun launchChildFragment() {
        // creating the object for child fragment
        val childFragment = RoutesListFragment.newInstance(
            user,
            this.getString(R.string.menu_recommended_routes)
        ) as RoutesListFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.child_fragment_container, childFragment).commit()
        // intialising the RefreshInterface object
        childFragment.intialiseRefreshInterface(refreshInterface)
        // calling the child fragment
        requireActivity().supportFragmentManager.beginTransaction()
            .add(R.id.child_fragment_container, childFragment)
            .addToBackStack(null).commit()
    }
}