package co.edu.unal.biketrainer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import co.edu.unal.biketrainer.ui.gallery.GalleryFragment
import co.edu.unal.biketrainer.ui.home.HomeFragment
import co.edu.unal.biketrainer.ui.routes.RoutesFragment
import co.edu.unal.biketrainer.ui.routes.list.RoutesListFragment
import co.edu.unal.biketrainer.ui.slideshow.SlideshowFragment
import co.edu.unal.biketrainer.utils.Utils
import com.facebook.login.LoginManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.Mapbox
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.nav_header_main.*

enum class ProviderType {
    BASIC,
    GOOGLE,
    FACEBOOK
}

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    private lateinit var email: String
    private lateinit var provider: String
    private val db = FirebaseFirestore.getInstance()

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

//        val fab: FloatingActionButton = findViewById(R.id.fab)
//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        Mapbox.getInstance(this, Utils.getMapboxAccessToken(this))
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_profile
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration) // Menu hamburguesa
        navView.setupWithNavController(navController)

        //setup
        val bundle = intent.extras
        this.email = bundle?.getString("email")!!
        this.provider = bundle.getString("provider")!!

        setup(email, provider)

        nav_view.setNavigationItemSelectedListener(this)

        //sesion guardada
        val prefs =
            getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }



    private fun setup(email: String, provider:String){
        title = "Inicio"

        // Mostrar datos en el menu drawable
        db.collection("users").document(email).get().addOnSuccessListener {
            nameMenuTextView.text = it.get("name") as String? + " " + it.get("lastname") as String?
            providerMenuTextView.text = provider
            emailMenuTextView.text = email
        }
    }

    private fun logOut(provider: String){
        // Borrar datos sesion
        val prefs = getSharedPreferences(getString(R.string.prefs_file),Context.MODE_PRIVATE).edit()
        prefs.clear()
        prefs.apply()

        if (provider == ProviderType.FACEBOOK.name) {
            LoginManager.getInstance().logOut()
        }

        // Cerrar sesion
        FirebaseAuth.getInstance().signOut()
        val authIntent = Intent(this, AuthActivity::class.java)
        startActivity(authIntent)
        onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                println("opcion home")
                val fragment = HomeFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment).commit()
            }
            R.id.nav_gallery -> {
                println("opcion gallery")
                val fragment = GalleryFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment).commit()
            }
            R.id.nav_slideshow -> {
                println("opcion slideshow")
                val fragment = SlideshowFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment).commit()

            }
            R.id.nav_routes -> {
                println("opcion routes")
                nav_view.menu.clear()
                nav_view.inflateMenu(R.menu.activity_submenu_drawer)
            }
            R.id.nav_profile -> {
                println("opcion profile")
                val fragment = ProfileFragment.newInstance(email.toString())
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment).commit()
            }
            R.id.back_to_main -> {
                nav_view.menu.clear()
                nav_view.inflateMenu(R.menu.activity_main_drawer)
            }
            R.id.routes_record -> {
                val fragment = RoutesFragment.newInstance(email.toString())
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment).commit()
            }
            R.id.routes_list -> {
                val fragment = RoutesListFragment.newInstance(email.toString())
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment).commit()
            }
            R.id.nav_logout -> {
                // Cerrar sesion
                println("opcion logout")
                logOut(provider.toString())
            }
        }
        return true
    }

}