package com.dangerfield.spyfall.WaitingActivity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.dangerfield.spyfall.GameActivity
import com.dangerfield.spyfall.MainActivity
import com.dangerfield.spyfall.NewGameActivity
import com.dangerfield.spyfall.R
import com.dangerfield.spyfall.data.Player
import kotlinx.android.synthetic.main.activity_waiting_game.*
import com.google.firebase.database.*
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class WaitingGame : AppCompatActivity() {

    var playerList = ArrayList<String>()
    var playerObjectList = ArrayList<Player>()
    var db = FirebaseFirestore.getInstance()
    private var TAG = "Waiting Game"
    lateinit var adapter: PlayerAdapter
    var roles = ArrayList<String>()
    private lateinit var randomPack: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_game)

        ACCESS_CODE = intent.getStringExtra("ACCESS_CODE")
        playerName = (intent.getStringExtra("PLAYER_NAME"))
        var fromActivity = intent.getStringExtra("FROM_ACTIVITY")


        tv_acess_code.text = ACCESS_CODE

        adapter = PlayerAdapter(playerName, playerList, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        displayUsers()
        if(fromActivity.equals("NEW_GAME_ACTIVITY"))
        {
        getLocation()
        }

    }


    fun onStartClick(view: View){
        loadPlayerObjects()
    }

    fun onLeaveClick(view:View){
        //called when user clicks leave game
        val gameRef = db.collection("games").document("$ACCESS_CODE")

        //remove player
        gameRef.update("playerList", FieldValue.arrayRemove("$playerName"))

        //if all players left, delete the document
        gameRef.get()
            .addOnSuccessListener { game ->
                if((game["playerList"] as ArrayList<String>).isEmpty()){ gameRef.delete() }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

    }

    fun displayUsers(){
        val gameRef = db.collection("games").document(ACCESS_CODE)
            gameRef.addSnapshotListener(EventListener<DocumentSnapshot>{ game ,e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@EventListener
                }

                if (game != null && game.exists()) {
                    if(game["isStarted"]== true){
                        val intent = newIntent(this,ACCESS_CODE,playerName)
                        startActivity(intent)
                        finish()
                    }
                    Log.d(TAG, "Current game data: ${game.data}")
                    playerList.clear()
                    Log.d(TAG,"Game[playerList] = ${game["playerList"]}")
                    playerList.addAll(game["playerList"] as ArrayList<String>)

                    adapter.notifyDataSetChanged()
                } else {
                    Log.d(TAG, "Current data: null")
                }
            })

    }

    fun getLocation() {

        val gameRef = db.collection("games").document("$ACCESS_CODE")

        gameRef.get().addOnSuccessListener {
            var chosenPacks = it.get("chosenPacks") as ArrayList<String>


            //selects one of the packs at random
            randomPack = chosenPacks[Random().nextInt(chosenPacks.size)]
            val collectionRef = db.collection(randomPack)
            collectionRef.get().addOnSuccessListener { documents ->

                //get a random location and add it to the game node
                var index = Random().nextInt(documents.toList().size)
                var randomLocation = documents.toList()[index]

                var location = HashMap<String,String>()
                location["chosenLocation"] = randomLocation.id
                gameRef.set(location, SetOptions.merge())

            }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }

    }


    fun loadPlayerObjects(){

        //creates player objects with a name a role from the chosen location
        //pushes those object to the game node and flips the isStarted Boolean, triggering an intent
        //in the displayUsers() function to the game screen

        val gameRef = db.collection("games").document(ACCESS_CODE)

        gameRef.get().addOnSuccessListener {
            var chosenPacks = it.get("chosenPacks") as ArrayList<String>
            var chosenLocation = it.get("chosenLocation") as String?

            //TODO get all location packs and select a random 30 unless the size is 1, then just 20

            for( i in 0 until chosenPacks.size){
                db.collection(chosenPacks[i]).whereEqualTo("location",chosenLocation)
                   .get().addOnSuccessListener {locationInfo->


                        if(locationInfo.documents.size == 1) {
                            //only one document should be found matching the location
                            roles.clear()
                            roles = locationInfo.documents[0]["roles"] as ArrayList<String>

                            //so now we have all the roles
                            //get playerlist, create an object for each playerlist and assign a random role
                            shuffleAndAssign(gameRef)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting roles: ", exception)
                    }
            }

        }

    }

    private fun shuffleAndAssign(gameRef: DocumentReference) {
        roles.shuffle()
        playerList.shuffle()

        for (i in 0 until playerList.size - 1) {
            //we can guarentee that i will never be out of index for roles as an 8 player max is enforced
            //i is between 0-6

            playerObjectList.add(Player(roles[i], playerList[i], 0))

        }
        //so we shuffled players and roles and assigned everyone except one a role in order
        //now we assign the lst one as the spy
        playerObjectList.add(Player("The Spy!", playerList.last(), 0))

        //now push to database
        var playerObjects = HashMap<String, Any?>()
        playerObjects["playerObjectList"] = playerObjectList
        gameRef.set(playerObjects, SetOptions.merge())
        //this line starts the game by triggering the intent in displayUsers()
        gameRef.update("isStarted", true)
    }

    companion object {


        private lateinit var playerName: String
        private lateinit var ACCESS_CODE: String

        fun changeName(newName: String,oldName: String,playerList: ArrayList<String>){
            playerName = newName
            playerList[playerList.indexOf(oldName)] = newName
            var db = FirebaseFirestore.getInstance()
            var gameRef = db.collection("games").document(ACCESS_CODE)
            gameRef.update("playerList", playerList)

        }

        fun newIntent(context: Context,ACCESS_CODE: String, playerName: String?): Intent {
        val intent = Intent(context, GameActivity::class.java)
        intent.putExtra("ACCESS_CODE", ACCESS_CODE)
        intent.putExtra("PLAYER_NAME", playerName)
            return intent
        }

    }

}



