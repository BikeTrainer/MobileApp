package co.edu.unal.biketrainer.ui.profile

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import co.edu.unal.biketrainer.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*
import java.security.MessageDigest

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
    private val storage = FirebaseStorage.getInstance()

    private val email by lazy { arguments?.getString(ARGS_NAME) }

    lateinit var option: Spinner
    lateinit var level: String
    private var filePath: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        Toast.makeText(context, email, Toast.LENGTH_SHORT).show()

        println("name :" + email)


    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val email:String by lazy { arguments?.getString("email").toString() }
        println("llego al perfil" + email)

        val options: MutableList<String> = arrayListOf("Facil", "Intermedio", "Dificil")

        //Buscar datos
        db.collection("users").document(email).get().addOnSuccessListener {
            profileNameEditText.setText(it.get("name") as String?)
            profileLastnameEditText.setText(it.get("lastname") as String?)
            profileLevelSpinner.setSelection(options.indexOf(it.get("level") as String?))
            Picasso.get().load(it.get("image_profile") as String?).into(avatar_picker)

            profilePhoneEditText.setText(it.get("phone") as String?)
            profileDateEditText.setText(it.get("date") as String?)

            //Actualizar datos
            profileSaveButton.setOnClickListener {
                println("boton actualizar")

                val storageRef = storage.reference

                var bytes = MessageDigest.getInstance(this.getString(R.string.digest_method))
                    .digest(email.toString().toByteArray())
                var avatarPath = StringBuilder(bytes.size * 2)

                bytes.forEach {
                    val i = it.toInt()
                    avatarPath.append(this.getString(R.string.HEX_CHARS)[i shr 4 and 0x0f])
                    avatarPath.append(this.getString(R.string.HEX_CHARS)[i and 0x0f])
                }

                val avatarRef = storageRef.child("$avatarPath/avatar.jpg")

                var downloadUri: Uri? = null

                avatarRef.putFile(filePath!!).addOnSuccessListener {
                    Toast.makeText(
                        this.requireContext(),
                        "Se actualizó la imagen de perfil",
                        Toast.LENGTH_SHORT
                    )
                    println(avatarRef.path)
                }.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    avatarRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        downloadUri = task.result
                        db.collection("users").document(email).set(
                            hashMapOf(
                                "name" to profileNameEditText.text.toString(),
                                "lastname" to profileLastnameEditText.text.toString(),
                                "phone" to profilePhoneEditText.text.toString(),
                                "date" to profileDateEditText.text.toString(),
                                "level" to level,
                                "image_profile" to downloadUri.toString()
                            )
                        )
                    } else {
                        // Handle failures
                        // ...
                    }
                }


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


        val root = inflater.inflate(R.layout.fragment_profile, container, false)


        val avatarPicker = root.findViewById<ImageView>(R.id.avatar_picker)

        avatarPicker.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                startActivityForResult(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI
                    ), PICK_IMAGE
                )
            }
        })


        // Inflate the layout for this fragment
        return root

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            filePath = data?.data
            avatar_picker.setImageURI(data?.data)
        }
    }

    companion object {
        const val ARGS_NAME = "email"
        fun newInstance(name: String): Fragment {
            val args = Bundle()
            args.putString(ARGS_NAME, name)
            val fragment = ProfileFragment()
            fragment.arguments = args
            return fragment
        }

        private const val PICK_IMAGE = 100
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