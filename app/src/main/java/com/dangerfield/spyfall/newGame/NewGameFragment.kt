package com.dangerfield.spyfall.newGame

import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.dangerfield.spyfall.util.UIHelper
import com.dangerfield.spyfall.R
import com.dangerfield.spyfall.game.GameViewModel
import com.dangerfield.spyfall.models.Game
import com.dangerfield.spyfall.models.GamePack
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_new_game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class NewGameFragment : Fragment() {

    private lateinit var viewModel: GameViewModel
    private lateinit var packsAdapter: PacksAdapter
    private var hasNetworkConnection = false
    lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_game, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //assigned in on create, as this does not need to be assigned again
        viewModel = ViewModelProviders.of(activity!!).get(GameViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = NavHostFragment.findNavController(this)

        //observer updates our value of internet connection
        viewModel.hasNetworkConnection.observe(viewLifecycleOwner,Observer{ hasNetworkConnection->
            this.hasNetworkConnection = hasNetworkConnection
        })

        changeAccent()

        //reference views once the view has been created
        tv_new_game_name.onFocusChangeListener = UIHelper.keyboardHider
        tv_new_game_time.onFocusChangeListener = UIHelper.keyboardHider

        btn_create.setOnClickListener { createGame() }

        btn_packs.setOnClickListener{ showPacksDialog() }

        configurePacksAdapter()
    }

    private fun configurePacksAdapter(){

        var packs = mutableListOf<GamePack>()

        //TODO: make this dynamic by pulling pack names from firebase

        packs.add(GamePack(resources.getColor(R.color.colorPink),"Standard",1,"pack 1",false))
        packs.add(GamePack(resources.getColor(R.color.colorGreen),"Standard",2,"pack 2",false))
        packs.add(GamePack(resources.getColor(R.color.colorYellow),"Special",1,"special pack",false))

        rv_packs.apply{
            layoutManager = GridLayoutManager(context, 3)
            packsAdapter = PacksAdapter(packs as ArrayList<GamePack>,context!!)
            adapter = packsAdapter
            setHasFixedSize(true)
        }
    }

    private fun createGame(){

        val timeLimit = tv_new_game_time.text.toString().trim()
        val playerName = tv_new_game_name.text.toString().trim()
        //these strings will be used for queries of the firestore database for which locations to include
        val chosenPacks = packsAdapter.packs.filter {it.isSelected}.map { it.queryString } as ArrayList<String>

        when {
            chosenPacks.isEmpty() -> {Toast.makeText(context,getString(R.string.new_game_error_select_pack), Toast.LENGTH_LONG).show()
                return}

            playerName.isEmpty() -> {Toast.makeText(context, getString(R.string.new_game_string_error_name), Toast.LENGTH_LONG).show()
            return}

            playerName.length > 25 -> {Toast.makeText(context, getString(R.string.change_name_character_limit), Toast.LENGTH_LONG).show()
                return}

            timeLimit.isEmpty() || timeLimit.toInt() > 10 -> {
                Toast.makeText(context, getString(R.string.new_game_error_time_limit), Toast.LENGTH_LONG).show()
                return
            }
        }

        viewModel.currentUser = playerName
        createGame(Game("",chosenPacks,false,
            mutableListOf(playerName) as ArrayList, ArrayList(),timeLimit.toLong()))
    }

    private fun createGame(game: Game){
        var connected = false
        if(hasNetworkConnection) {

            Handler().postDelayed({
                if(!connected){
                    //if we havent connected within 8 seconds, stop trying
                    UIHelper.errorDialog(context!!).show()
                    Handler(context!!.mainLooper).post {
                        enterMode()
                    }
                    FirebaseDatabase.getInstance().purgeOutstandingWrites()
                }
            }, 8000)

            loadMode()


            viewModel.createGame(game, UUID.randomUUID().toString().substring(0, 6).toLowerCase())
                .addOnCompleteListener {
                    connected = true
                    enterMode()
                    val bundle = bundleOf("FromFragment" to "NewGameFragment")
                    navController.navigate(R.id.action_newGameFragment_to_waitingFragment, bundle)
                }
        }else{
            UIHelper.errorDialog(context!!).show()
            enterMode()
        }
    }

    fun loadMode(){
        pb_new_game.visibility = View.VISIBLE
        btn_create.isClickable = false
        btn_packs.isClickable = false
    }
    fun enterMode(){
        pb_new_game.visibility = View.INVISIBLE
        btn_create.isClickable = true
        btn_packs.isClickable = true
    }

    private fun changeAccent(){
        btn_create.background.setTint(UIHelper.accentColor)

        val drawable = resources.getDrawable(R.drawable.ic_rules).mutate()
        drawable.setColorFilter(UIHelper.accentColor, PorterDuff.Mode.SRC_ATOP)
        btn_packs.setImageDrawable(drawable)

        UIHelper.setCursorColor(tv_new_game_name,UIHelper.accentColor)

        UIHelper.setCursorColor(tv_new_game_time,UIHelper.accentColor)

        pb_new_game.indeterminateDrawable
            .setColorFilter(UIHelper.accentColor, PorterDuff.Mode.SRC_IN )

    }

    fun showPacksDialog() {

        //we also might consider a different structure for the backend where the packs are kept in on collection
        var connected = false
        Handler().postDelayed({
            if(!connected){
                Log.d("WIATING","Got here")
                //if we are not connected in 8 seconds, stop trying. Should still work with cache
                UIHelper.errorDialog(context!!).show()
                Handler(context!!.mainLooper).post {
                    enterMode()
                }
                FirebaseDatabase.getInstance().purgeOutstandingWrites()
            }

        }, 8000)

        loadMode()
        val list = mutableListOf<List<String>>()
        viewModel.getPackNames().addOnSuccessListener {
            connected = !it.isEmpty

            val packNames = it.documents.map { document -> document.id }
            var completedTasks = 0
            for (i in 0 until packNames.size) {

                viewModel.db.collection(packNames[i]).get().addOnSuccessListener { pack ->
                    list.add(pack.documents.map { location -> location.id })

                }.addOnCompleteListener {
                    completedTasks += 1
                    if (completedTasks == packNames.size) {
                        //then youre done
                        UIHelper.packsDialog(context!!, list).show()
                        enterMode()
                    }
                }
            }

        }
    }
}

