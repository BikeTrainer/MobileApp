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
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment() {


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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        text_home.visibility = View.VISIBLE
        val childFragment = RoutesListFragment.newInstance(
            user,
            this.getString(R.string.menu_recommended_routes)
        )
        childFragmentManager.beginTransaction()
            .replace(R.id.child_fragment_container, childFragment).commit()
    }
}