package edu.scut.generator.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.LineChart
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
        viewModel.lineCharData.value =
            arrayListOf(Entry(10f, 0f), Entry(20f, 5f), Entry(30f, 8f), Entry(40f, 12f))
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