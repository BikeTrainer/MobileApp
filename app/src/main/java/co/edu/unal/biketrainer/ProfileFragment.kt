package co.edu.unal.biketrainer

import android.app.FragmentManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import kotlinx.android.synthetic.main.nav_header_main.*

import co.edu.unal.biketrainer.ui.home.HomeFragment
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_gallery.*
import kotlinx.coroutines.awaitAll

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val email by lazy { arguments?.getString(ARGS_NAME) }

    lateinit var option: Spinner
    lateinit var level : String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        Toast.makeText(context, email, Toast.LENGTH_SHORT).show()

        println("name :"+email)


    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val email:String by lazy { arguments?.getString("email").toString() }
        println("llego al perfil"+email)

        val options:MutableList<String> = arrayListOf("Facil","Intermedio","Dificil")




        //Buscar datos
        db.collection("users").document(email).get().addOnSuccessListener {
            profileNameEditText.setText(it.get("name") as String?)
            profileLastnameEditText.setText(it.get("lastname") as String?)
            profilePhoneEditText.setText(it.get("phone") as String?)
            profileDateEditText.setText(it.get("date") as String?)

            //Actualizar datos
            profileSaveButton.setOnClickListener{
                println("boton actualizar")
                db.collection("users").document(email).set(
                    hashMapOf("name" to profileNameEditText.text.toString(),
                        "lastname" to profileLastnameEditText.text.toString(),
                        "phone" to profilePhoneEditText.text.toString(),
                        "date" to profileDateEditText.text.toString(),
                        "level" to level)
                )
            }

            profileLevelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    level = p0?.getItemAtPosition(p2).toString()
                    println(level)

                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }
        }



        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)

    }

    companion object {
        const val ARGS_NAME = "email"
        fun newInstance(name: String): Fragment{
            val args = Bundle()
            args.putString(ARGS_NAME, name)
            val fragment = ProfileFragment()
            fragment.arguments = args
            return fragment
        }
    }

    /*companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }*/
}