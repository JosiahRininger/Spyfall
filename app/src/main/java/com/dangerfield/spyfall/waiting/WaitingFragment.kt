package com.dangerfield.spyfall.waiting

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.dangerfield.spyfall.BuildConfig
import com.dangerfield.spyfall.R
import com.dangerfield.spyfall.util.UIHelper
import com.dangerfield.spyfall.game.GameViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase

import kotlinx.android.synthetic.main.fragment_waiting.*
import kotlinx.coroutines.*
import java.util.ArrayList

class WaitingFragment : Fragment() {

    private var adapter: WaitingPlayersAdapter? = null
    lateinit var viewModel: GameViewModel
    private var isGameCreator: Boolean = false
    private var navigateBack: (() -> Unit)? = null
    private lateinit var navController: NavController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_waiting, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navController =  Navigation.findNavController(parentFragment!!.view!!)
        viewModel = ViewModelProviders.of(activity!!).get(GameViewModel::class.java)

        navigateBack = { UIHelper.customSimpleAlert(context!!,
            resources.getString(R.string.waiting_leaving_title),
            resources.getString(R.string.waiting_leaving_message),
            resources.getString(R.string.leave_action_positive), {leaveGame()},
            resources.getString(R.string.leave_action_negative),{ btn_leave_game.isClickable = true
            }).show()
        }

        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                   navigateBack?.invoke()
                }
            })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if(BuildConfig.FLAVOR == "free") adView.loadAd(AdRequest.Builder().build())

        viewModel.getGameUpdates().observe(viewLifecycleOwner, Observer { updatedGame ->

            //if the game has been started, you cant click create
            btn_start_game.isClickable = !updatedGame.started

            adapter?.players = updatedGame.playerList

            //we know everything is good to go when the player objects list is done
            if(updatedGame.playerList.size == updatedGame.playerObjectList.size && navController.currentDestination?.id == R.id.waitingFragment){
                navController.navigate(R.id.action_waitingFragment_to_gameFragment)
                enterMode()
                viewModel.incrementGamesPlayed()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        changeAccent()
        //only set the listeners once the view has been created
        btn_start_game.setOnClickListener {
            loadMode()
            //only the game creator has the roles automatically
            //TODO: is you wanted a timeout function, this woud be the place
            if(viewModel.roles.isEmpty()){ viewModel.getRolesAndStartGame() }else{ viewModel.startGame() }
        }

        btn_leave_game.setOnClickListener {
            btn_leave_game.isClickable = false
            navigateBack?.invoke() ?: leaveGame()
        }

        configureLayoutManagerAndRecyclerView()
    }

    override fun onResume() {
        super.onResume()

        // we need to check if the user is the game creator every time they come to this screen
        isGameCreator = arguments?.get("FromFragment") == "NewGameFragment"

        tv_acess_code.text = viewModel.ACCESS_CODE

        if(isGameCreator){ viewModel.getRandomLocation() }
    }

    private fun leaveGame() {
        viewModel.removePlayer()
        navController.popBackStack(R.id.startFragment, false)

    }
        private fun configureLayoutManagerAndRecyclerView() {
            rv_player_list_waiting.layoutManager = LinearLayoutManager(context)
            adapter = WaitingPlayersAdapter(context!!, ArrayList(),viewModel)
            rv_player_list_waiting.adapter = adapter
    }

    private fun changeAccent(){
        btn_start_game.background.setTint(UIHelper.accentColor)
        pb_waiting.indeterminateDrawable
            .setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN )
    }

    fun loadMode(){
        btn_start_game.text = ""
        pb_waiting.visibility = View.VISIBLE
        btn_leave_game.isClickable = false
        btn_start_game.isClickable = false
    }
    fun enterMode(){
        btn_start_game.text = getString(R.string.string_btn_start_game)
        pb_waiting.visibility = View.GONE
        btn_leave_game.isClickable = true
        btn_start_game.isClickable = true
    }

}

