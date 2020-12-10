package co.edu.unal.biketrainer.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import co.edu.unal.biketrainer.R
import co.edu.unal.biketrainer.model.Route
import co.edu.unal.biketrainer.model.User
import co.edu.unal.biketrainer.ui.routes.RoutesFragment
import co.edu.unal.biketrainer.utils.Utils
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.anychart.enums.Anchor
import com.anychart.enums.HoverMode
import com.anychart.enums.Position
import com.anychart.enums.TooltipPositionMode
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_about_you.*
import java.text.SimpleDateFormat
import kotlin.math.roundToInt


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AboutYouFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var lastRoute: Route? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_about_you, container, false)



        db.collection("routes").whereEqualTo("created_by", user?.id).orderBy(
            "created_at",
            Query.Direction.DESCENDING
        ).limit(1).get()
            .addOnSuccessListener { query ->
                query.documents.forEach {
                    lastRoute = Utils.getRouteFromDocumentSnap(it)
                    launchChildRouteFragment(lastRoute!!)
                    var cartesian: Cartesian? = AnyChart.column()
                    var cartesianSpeed: Cartesian? = AnyChart.column()

                    var altimetry = ArrayList<DataEntry>()
                    var velocity = ArrayList<DataEntry>()

                    last_route_distance.text = "%.2f km".format(lastRoute!!.distance?.div(1000))
                    last_route_time.text =
                        SimpleDateFormat("HH:mm:ss").format(lastRoute!!.average_duration!!)

                    var currentDistance = 0f
                    lastRoute!!.route?.forEach {
                        if (altimetry.isEmpty()) {
                            currentDistance += it.distanceTo((lastRoute!!.origin)).div(1000)
                        } else {
                            currentDistance += it.distanceTo(
                                lastRoute!!.route!![lastRoute!!.route?.indexOf(
                                    it
                                )!! - 1]
                            ).div(1000)
                        }

                        altimetry.add(
                            ValueDataEntry(
                                "%.2f km".format(currentDistance), it.altitude
                            )
                        )
                        velocity.add(
                            ValueDataEntry(
                                "%.2f km".format(currentDistance), it.speed
                            )
                        )
                    }

//                    var column = cartesian?.column(altimetry)
                    var columnSpeed = cartesianSpeed?.column(velocity)

//                    column?.tooltip()?.titleFormat("{%X}")
//                        ?.position(Position.CENTER_BOTTOM)
//                        ?.anchor(Anchor.CENTER_BOTTOM)
//                        ?.offsetX(0)
//                        ?.offsetY(0)
//                        ?.format("{%Value}{groupsSeparator: }")

                    columnSpeed?.tooltip()?.titleFormat("{%X}")
                        ?.position(Position.CENTER_BOTTOM)
                        ?.anchor(Anchor.CENTER_BOTTOM)
                        ?.offsetX(0)
                        ?.offsetY(0)
                        ?.format("{%Value}{groupsSeparator: }")

//                    var chartView = root.findViewById<AnyChartView>(R.id.any_chart_view)


//                    cartesian!!.animation(true)
//                    cartesian!!.title("Altimetría")
//
//                    cartesian!!.yAxis(0).labels().format("{%Value}{groupsSeparator: }")
//
//                    cartesian!!.tooltip().positionMode(TooltipPositionMode.POINT)
//                    cartesian!!.interactivity().hoverMode(HoverMode.BY_X)
//
//                    cartesian!!.xAxis(0).title("Distancia")
//                    cartesian!!.yAxis(0).title("Altitud")
//
//                    chartView.setChart(cartesian)

                    var charSpeedRoute = root.findViewById<AnyChartView>(R.id.any_chart_view_speed)

                    cartesianSpeed!!.animation(true)
                    cartesianSpeed.title("Velocidad")

                    cartesianSpeed.yAxis(0).labels().format("{%Value}{groupsSeparator: }")

                    cartesianSpeed.tooltip().positionMode(TooltipPositionMode.POINT)
                    cartesianSpeed.interactivity().hoverMode(HoverMode.BY_X)

                    cartesianSpeed.xAxis(0).title("Distancia")
                    cartesianSpeed.yAxis(0).title("Velocidad")


                    charSpeedRoute.setChart(cartesianSpeed)

                }
            }

        var experienceBar = root.findViewById<SeekBar>(R.id.experience_bar)
        if(user?.experience != null){
            experienceBar.progress = user?.experience!!.roundToInt()
        } else {
            experienceBar.progress = 0
        }

        var levelLabel = root.findViewById<TextView>(R.id.experience_level_label)
        levelLabel.text = user?.level
        return root
    }

    private fun launchChildRouteFragment(route: Route) {
        val routesFragment = RoutesFragment.newInstance(
            user,
            route
        ) as RoutesFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.child_last_route_fragment_container, routesFragment).commit()
    }

    companion object {
        var user: User? = null
        fun newInstance(user: User?): Fragment {
            val fragment = AboutYouFragment()
            this.user = user
            return fragment
        }
    }
}