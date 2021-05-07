package edu.scut.generator.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import edu.scut.generator.R
import edu.scut.generator.databinding.FragmentDetailBinding

class DetailFragment : Fragment() {

    private val logTag = DetailFragment::class.java.name
    private lateinit var viewModel: MainViewModel
    private lateinit var dataBinding: FragmentDetailBinding

    lateinit var mItem: GeneratorItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(
            activity!!,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        ).get(MainViewModel::class.java)
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_detail, container, false)
        dataBinding.data = viewModel
        dataBinding.lifecycleOwner = this

        setHasOptionsMenu(true)
        (activity!! as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        (activity!! as AppCompatActivity).supportActionBar!!.title = "${mItem.name}详情"

        viewModel.thisGeneratorItem.observe(this, Observer {
            dataBinding.generatorIcon.setImageResource(it.iconId)
        })
        viewModel.thisGeneratorItem.value = mItem
        initLineChart()

        return dataBinding.root
    }

    private fun initLineChart() {
        viewModel.lineCharData.observe(this, Observer {
            dataBinding.powerLineChart.data = LineData(LineDataSet(it, "功率"))
        })
        dataBinding.powerLineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        dataBinding.powerLineChart.description.text = "发电机功率"
        dataBinding.powerLineChart.axisLeft.axisMinimum = 0f
        dataBinding.powerLineChart.axisLeft.axisMaximum = 30f
        dataBinding.powerLineChart.axisRight.isEnabled = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> activity!!.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun debug(any: Any) = edu.scut.generator.global.debug(logTag, any)

    companion object {
        @JvmStatic
        fun newInstance(item: GeneratorItem): DetailFragment {
            return DetailFragment().apply {
                mItem = item
            }
        }
    }
}