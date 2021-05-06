package edu.scut.generator.ui.main

import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import edu.scut.generator.R
import edu.scut.generator.databinding.FragmentDetailBinding
import edu.scut.generator.global.debug

class DetailFragment : Fragment() {

    lateinit var viewModel: DetailViewModel
    lateinit var databinding: FragmentDetailBinding

    lateinit var mItem: GeneratorItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(DetailViewModel::class.java)
        databinding = DataBindingUtil.inflate(inflater, R.layout.fragment_detail, container, false)
        databinding.data = viewModel
        databinding.lifecycleOwner = this

        setHasOptionsMenu(true)
        (activity!! as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        (activity!! as AppCompatActivity).supportActionBar!!.title = "${mItem.name}详情"

        viewModel.thisGeneratorItem.observe(this) {
            databinding.generatorIcon.setImageResource(it.iconId)
        }
        viewModel.thisGeneratorItem.value = mItem
        initLineChart()

        return databinding.root
    }

    private fun initLineChart() {
        viewModel.lineCharData.observe(this) {
            databinding.powerLineChart.data = LineData(LineDataSet(it, "功率"))
        }
        databinding.powerLineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        databinding.powerLineChart.description.text = "发电机功率"
        databinding.powerLineChart.axisLeft.axisMinimum = 0f
        databinding.powerLineChart.axisLeft.axisMaximum = 30f
        databinding.powerLineChart.axisRight.isEnabled = false
        Thread {
//            val stTime = SystemClock.elapsedRealtime()
//            while (lifecycle.currentState != Lifecycle.State.DESTROYED) {
//                val data = viewModel.lineCharData.value!!
//                val rand = (Math.random() - 0.5).toFloat()
//                val entry = Entry(
//                    (SystemClock.elapsedRealtime() - stTime) / 1000f,
//                    18f + rand * 5
//                )
//                data.add(entry)
//                viewModel.thisGeneratorItem.postValue(viewModel.thisGeneratorItem.value!!.apply {
//                    power = entry.y.toDouble()
//                    temperatureDifference = 8.5 + rand
//                    rev = 600 + (rand * 50).toInt()
//                })
//                if (data.size > 10) data.removeAt(0)
//                viewModel.lineCharData.postValue(data)
//                databinding.powerLineChart.notifyDataSetChanged()
//                databinding.powerLineChart.invalidate()
//                Thread.sleep(200)
//            }
//            debug("线程结束")
        }.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> activity!!.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmStatic
        fun newInstance(item: GeneratorItem): DetailFragment {
            return DetailFragment().apply {
                mItem = item
            }
        }
    }
}