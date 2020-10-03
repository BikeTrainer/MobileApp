package co.edu.unal.biketrainer

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.facebook.CallbackManager
import com.facebook.FacebookActivity
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_auth.*

class AuthActivity : AppCompatActivity() {

    private  val GOOGLE_SIGN_IN = 100
    private  val callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {

        Thread.sleep(2000)
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        //setup
        setup()
        sesion()
    }

    override fun onStart() {
        super.onStart()

        authLayout.visibility=View.VISIBLE
    }

    //Mantener la sesion
    private fun sesion(){
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email",null)
        val provider = prefs.getString("provider", null)

        if(email != null && provider != null){
            authLayout.visibility = View.INVISIBLE
            showHome(email,ProviderType.valueOf(provider))
        }
    }

    private fun setup(){
        title="Autenticacion"

        //Registrar usuarios
        signUpButton.setOnClickListener{

            showSignUp(emailEditText.text.toString())

        }

        //Loguear usuarios registrados
        logInButton.setOnClickListener{
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()){

                FirebaseAuth.getInstance().
                signInWithEmailAndPassword(emailEditText.text.toString(),passwordEditText.text.toString()).
                addOnCompleteListener{
                    if(it.isSuccessful){
                        showHome(it.result?.user?.email ?:"", ProviderType.BASIC)
                    } else {
                        showAlert()
                    }

                }
            }
        }

        //Logueo por google
        googleButton.setOnClickListener{
            //Configuracion
            val googleConf=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()

            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)

        }

        //Logueo por facebook
        facebookButton.setOnClickListener{

            //Pantalla de autenticacion nativa de facebook
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
            //Configuracion
            LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult>{

                override fun onSuccess(result: LoginResult?) {
                    //Se controla cuando la operacion se hizo satisfactoriamente
                    result?.let {
                        val token = it.accessToken

                        val credential = FacebookAuthProvider.getCredential(token.token)

                        //Autenticacion de facebook para que este reflejada en firebase
                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener{
                            if(it.isSuccessful){
                                showHome(it.result?.user?.email ?:"", ProviderType.FACEBOOK)
                            } else {
                                showAlert()
                            }
                        }
                    }
                }

                override fun onCancel() {
                    //Se controla cuando se cancela la operacion
                }

                override fun onError(error: FacebookException?) {
                    //Se controla cuando sucede un error
                    showAlert()
                }

            })
        }

    }

    //Alerta si no es posible loguearse
    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un autenticando al usuario")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog=builder.create()
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

    //Ir al registro enviando el email si se lleno
    private fun showSignUp (email: String){
        val signUpIntent = Intent(this, SignUpActivity::class.java).apply {
            putExtra("email",email)
        }
        startActivity(signUpIntent)
    }

    //Autenticacion de google y facebook para que este reflejada en firebase
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        //Desencadena una llamada a una operacion del logueo por facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)

        //Recuperar cuenta de google
        if (requestCode == GOOGLE_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            //Si no se recupera ninguna cuenta se envia una alerta
            try {
                val account = task.getResult(ApiException::class.java)

                if (account != null){
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener{
                        if(it.isSuccessful){
                            showHome(account.email ?:"", ProviderType.GOOGLE)
                        } else {
                            showAlert()
                        }
                    }
                }
            } catch (e: ApiException){
                showAlert()
            }

        }
    }
}