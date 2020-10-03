package co.edu.unal.biketrainer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_auth.*
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        //setup
        val  bundle = intent.extras
        val email = bundle?.getString("email")
        setup(email ?:"")
    }

    private fun setup(email: String){
        
        signUpEmailEditText.setText(email)

        signUpSaveButton.setOnClickListener {

            if (signUpEmailEditText.text.isNotEmpty() && signUpPasswordEditText.text.isNotEmpty()){

                FirebaseAuth.getInstance().
                createUserWithEmailAndPassword(signUpEmailEditText.text.toString(),signUpPasswordEditText.text.toString()).
                addOnCompleteListener{
                    if(it.isSuccessful){
                        saveUser(it.result?.user?.email ?:"")
                        showHome(it.result?.user?.email ?:"", ProviderType.BASIC)
                    } else {
                        showAlert()
                    }

                }
            }

        }

    }

    //Enviar datos a firebase firestore
    private fun saveUser(email: String){
        db.collection("users").document(email).set(
            hashMapOf("name" to signUpNameEditText.text.toString(),
                "lastname" to signUpLastnameEditText.text.toString(),
                "phone" to signUpPhoneEditText.text.toString(),
                "date" to signUpDateEditText.text.toString())
        )
    }

    //Alerta si no es posible loguearse
    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un autenticando al usuario")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog =builder.create()
        dialog.show()
    }

    //Ir al home y enviar datos de sesion
    private fun showHome (email: String, provider:ProviderType){
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email",email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
    }
}