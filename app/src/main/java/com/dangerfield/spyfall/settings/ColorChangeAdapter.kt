package com.dangerfield.spyfall.settings

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dangerfield.spyfall.R
import kotlinx.android.synthetic.main.item_change_color.view.*

class ColorChangeAdapter(var colors: List<ColorButton>  ,private var context: Context?) : RecyclerView.Adapter<ColorChangeAdapter.ViewHolder>() {

    var selectedPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var background: View = view.findViewById(R.id.change_color_background)
        var hiddenText: TextView = view.findViewById(R.id.tv_change_color_type)

        init {
            context = view.context

            view.setOnClickListener {
                //first, unselect the previous
                if(selectedPosition != -1){ colors[selectedPosition].isSelected = false }
                //now select this one
                colors[adapterPosition].isSelected = true
                selectedPosition = adapterPosition

                notifyDataSetChanged()
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val singleButton = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_change_color, parent, false)
        return ViewHolder(singleButton)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(colors[position].color == Color.WHITE){
            holder.hiddenText.visibility = View.VISIBLE
            holder.hiddenText.text = context!!.resources.getString(R.string.change_theme_random_colors)
        }else{
            holder.background.setBackgroundColor(colors[position].color)
            holder.hiddenText.visibility = View.INVISIBLE
        }
        if(colors[position].isSelected){
            select(holder.itemView)
        }else{
            unselect(holder.itemView)
        }
    }

    override fun getItemCount(): Int {
        return colors.size
    }

    private fun unselect(view: View){
        //unselect
        view.color_change_filter.visibility = View.INVISIBLE
        view.color_change_check_animation.visibility = View.INVISIBLE
    }
    private fun select(view: View){
        //select
        view.color_change_filter.visibility = View.VISIBLE
        view.color_change_check_animation.speed = 2.0f
        view.color_change_check_animation.visibility = View.VISIBLE
        view.color_change_check_animation.playAnimation()
    }
}