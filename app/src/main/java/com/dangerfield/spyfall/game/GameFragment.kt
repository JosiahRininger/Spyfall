package com.dangerfield.spyfall.game

import android.graphics.Paint
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.dangerfield.spyfall.models.Player
import kotlinx.android.synthetic.main.fragment_game.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.dangerfield.spyfall.BuildConfig
import com.dangerfield.spyfall.R
import com.dangerfield.spyfall.util.UIHelper
import com.google.android.gms.ads.AdRequest

class GameFragment : Fragment() {

    private lateinit var viewModel: GameViewModel
    private lateinit var currentPlayer: Player
    private lateinit var locationsAdapter: GameViewsAdapter
    private lateinit var playersAdapter: GameViewsAdapter
    private  var timer: CountDownTimer? = null
    private lateinit var navController: NavController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //this does not get called again, set all the variables you want to keep(not reassign)
        navController =  NavHostFragment.findNavController(this)
        viewModel = ViewModelProviders.of(activity!!).get(GameViewModel::class.java)

        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    //show alert when user presses back
                    UIHelper.customSimpleAlert(context!!,
                        getString(R.string.leave_game_title),
                        getString(R.string.leave_in_game_message),
                        getString(R.string.leave_action_positive),
                        {endGame()},getString(R.string.leave_action_negative),{}).show()
                }
            })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        tv_game_role.maxTextSize = 96.0f

        if(BuildConfig.FLAVOR == "free") adView2.loadAd(AdRequest.Builder().build()) else adView2.visibility = View.GONE

        //set up views every time
        val firstPlayer = configurePlayerViews()
        configurePlayersAdapter(firstPlayer)
        configureLocationsAdapter()

        //the observers should not be called unless the fragment is in a started or resumed state
        viewModel.getGameUpdates().observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            //someone has reset game
            if(!it.started && navController.currentDestination?.id == R.id.gameFragment){
                viewModel.resetGame().addOnCompleteListener{
                    navController.popBackStack(R.id.waitingFragment, false)
                }
            }
            //now this gets called anytime anything changes
            if(it.started && navController.currentDestination?.id == R.id.gameFragment){
                //but right now it says: if the game has started, and im still on this screen, and something has changed..
                var newFirstPlayer =  configurePlayerViews()
                playersAdapter.first = newFirstPlayer
            }

        })

        viewModel.gameExists.observe(viewLifecycleOwner, androidx.lifecycle.Observer { exists ->
            //someone ended the game
            if(!exists && navController.currentDestination?.id == R.id.gameFragment){ endGame() }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        changeAccent()
        //we set the listeners once the view has actually been inflated
        btn_end_game.setOnClickListener { UIHelper.customSimpleAlert(context!!,
            getString(R.string.end_game_title),
            getString(R.string.end_game_message),
            getString(R.string.end_game_positive_action), {endGame()},
            getString(R.string.negative_action_standard),{}).show()}

        btn_play_again.setOnClickListener{
            viewModel.resetGame().addOnCompleteListener{
                navController.popBackStack(R.id.waitingFragment, false)
            }
        }
        btn_hide.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        btn_hide.setOnClickListener{ hide()}

        tv_game_timer.text = String.format(
            Locale.getDefault(), "%d:%02d",
            viewModel.gameObject.value?.timeLimit, 0
        )
    }

    override fun onResume() {
        super.onResume()


        //just to be super safe
        if(viewModel.gameExists.value!!){
            if(timer == null){
                startTimer(viewModel.gameObject.value!!.timeLimit)
            }else{
                Log.d("GAME", "timer was not null")
            }
        }
    }

    private fun hide(){
        if(tv_game_role.visibility == View.VISIBLE){
            tv_game_role.visibility = View.GONE
            tv_game_location.visibility = View.GONE
            view_role_card.visibility = View.GONE
            btn_hide.text = resources.getString(R.string.string_show)
        }else{
            tv_game_role.visibility = View.VISIBLE
            view_role_card.visibility = View.VISIBLE
            tv_game_location.visibility = View.VISIBLE
            btn_hide.text = resources.getString(R.string.string_hide)
        }
    }

    private fun startTimer(timeLimit : Long): CountDownTimer {

        timer = object : CountDownTimer((60000*timeLimit), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val text = String.format(
                    Locale.getDefault(), "%d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                )
                tv_game_timer.text = text
            }

            override fun onFinish() {
                tv_game_timer.text = "0:00"
                btn_play_again.visibility = View.VISIBLE
            }
        }.start()
        return timer!!
    }

    fun endGame(){
        viewModel.endGame()
        timer?.cancel()
        timer = null
        navController.popBackStack(R.id.startFragment, false)
    }

    private fun configureLocationsAdapter(){
        locationsAdapter = GameViewsAdapter(context!!, ArrayList(), null)
        rv_locations.apply{
            adapter = locationsAdapter
            layoutManager = GridLayoutManager(context, 2) }

        //attaches observer after the locations adapter is actually initialized
        viewModel.getAllGameLocations().observe(viewLifecycleOwner, androidx.lifecycle.Observer { locations ->
            locationsAdapter.items = locations
        })
    }

    private fun configurePlayersAdapter(firstPlayer: String){
        rv_players.apply{
            layoutManager = GridLayoutManager(context, 2)
            playersAdapter = GameViewsAdapter(context,(viewModel.gameObject.value!!.playerList.shuffled()) as ArrayList<String>, firstPlayer)
            adapter = playersAdapter
            setHasFixedSize(true)
        }
    }

    private fun configurePlayerViews(): String {
        //we enforce that no two users have the same username
        currentPlayer = (viewModel.gameObject.value!!.playerObjectList).filter { it.username == viewModel.currentUser }[0]

        //dont let the spy know the location
        tv_game_location.text = if(currentPlayer.role.toLowerCase().trim() == "the spy!"){
            "Figure out the location!"
        } else { "Location: ${viewModel.gameObject.value?.chosenLocation}" }

        tv_game_role.text = "Role: ${currentPlayer.role}"

        return viewModel.gameObject.value!!.playerObjectList[0].username
    }

    private fun changeAccent(){
        btn_end_game.background.setTint(UIHelper.accentColor)
        btn_hide.background.setTint(UIHelper.accentColor)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
    }
}
