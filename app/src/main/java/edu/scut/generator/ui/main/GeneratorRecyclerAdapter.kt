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
    private val dataList: MutableLiveData<ArrayList<GeneratorItem>>,
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
        holder.power.text = String.format("%.3f W", generatorItem.power)
        holder.temperatureDifference.text =
            String.format("%.1f ℃", generatorItem.temperatureDifference)
        holder.rev.text = "${generatorItem.rev} rpm"
        holder.state.setTextColor(
            when (generatorItem.state) {
                GeneratorState.Running -> Color.rgb(48, 175, 56)
                GeneratorState.Paused -> Color.rgb(136, 136, 136)
                GeneratorState.Disabled -> Color.rgb(233, 30, 99)
                GeneratorState.Disconnected -> Color.rgb(232, 138, 0)
                GeneratorState.Unknown -> Color.rgb(3, 169, 244)
            }
        )

        holder.itemView.setOnClickListener {
            mainIGeneratorRecyclerAdapter.onItemClick(generatorItem)
//            AlertDialog.Builder(holder.itemView.context)
//                .setTitle(generatorItem.name)
//                .setMessage(
//                    "ID: ${generatorItem.id}\n" +
//                            "名称: ${generatorItem.name}\n" +
//                            "状态: ${GeneratorItem.stateToString(generatorItem.state)}\n" +
//                            "功率: ${generatorItem.power} W\n" +
//                            "温差: ${generatorItem.temperatureDifference} ℃\n" +
//                            "转速: ${generatorItem.rev} rpm\n\n" +
//                            "监测曲线 TODO" // TODO: 2021/4/26
//                )
//                .setPositiveButton("关闭", null)
//                .show()
        }
    }

    override fun getItemCount(): Int {
        return dataList.value!!.size
    }
}