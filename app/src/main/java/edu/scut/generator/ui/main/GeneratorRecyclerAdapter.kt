package edu.scut.generator.ui.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import edu.scut.generator.R
import edu.scut.generator.global.GeneratorState

class GeneratorRecyclerAdapter(
    private val dataList: MutableLiveData<List<GeneratorItem>>,
    fragment: MainFragment
) :
    RecyclerView.Adapter<GeneratorRecyclerAdapter.ViewHolder>() {

    private val mainIGeneratorRecyclerAdapter = fragment

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.recycler_generator_icon)
        val name: TextView = itemView.findViewById(R.id.recycler_generator_name)
        val state: TextView = itemView.findViewById(R.id.recycler_generator_state)
        val power: TextView = itemView.findViewById(R.id.recycler_generator_power)
        val temperatureDifference: TextView =
            itemView.findViewById(R.id.recycler_generator_temperature_difference)
        val rev: TextView = itemView.findViewById(R.id.recycler_generator_rev)
    }

    interface IGeneratorRecyclerAdapter {
        fun onItemClick(item: GeneratorItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.main_generator_item, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val generatorItem = dataList.value!![position]
        holder.icon.setImageResource(generatorItem.iconId)
        holder.name.text = generatorItem.name
        holder.state.text = GeneratorItem.stateToString(generatorItem.state)
        holder.power.text = "%.2f W".format(generatorItem.power)
        holder.temperatureDifference.text = "%.1f ℃".format(generatorItem.temperatureDifference)
        holder.rev.text = "%.1f rpm".format(generatorItem.rev)
        holder.state.setTextColor(GeneratorItem.getStateTextColor(generatorItem.state))

        holder.itemView.setOnClickListener {
            mainIGeneratorRecyclerAdapter.onItemClick(generatorItem)
        }
    }

    override fun getItemCount(): Int {
        return dataList.value!!.size
    }
}