package edu.scut.generator.ui.main

import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import edu.scut.generator.R
import edu.scut.generator.databinding.FragmentDetailBinding
import edu.scut.generator.global.Constant
import edu.scut.generator.global.debug
import edu.scut.generator.global.prepareCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailFragment : Fragment() {

    private val logTag = "DetailFragment"
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

        if (this::mItem.isInitialized) viewModel.thisGeneratorItem.value = mItem
        setHasOptionsMenu(true)
        (activity!! as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        viewModel.thisGeneratorItem.observe(this, Observer { generatorItem ->
            if (generatorItem != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    dataBinding.generatorIcon.setImageResource(generatorItem.iconId)
                    (activity!! as AppCompatActivity).supportActionBar!!.title =
                        "${generatorItem.name}??????"
                }
            }
        })
        initLineChart()
        viewModel.entryDetailTime.value = SystemClock.elapsedRealtime()

        return dataBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.thisGeneratorItem.value = null
    }

    private fun initLineChart() {
        val powerLineChart = dataBinding.powerLineChart.apply {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            description.text = "?????? / s"
        }
        viewModel.powerLineChatData.value!!.clear()
        viewModel.powerLineChatData.value = ArrayList(Constant.MaxPointNumberMeanwhile)
        viewModel.powerLineChatData.observe(this, Observer { arrayList ->
            val lineDataSet = LineDataSet(arrayList, "?????? / W")
            lineDataSet.setDrawFilled(true)
            lineDataSet.fillDrawable =
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.fill_power_chart,
                    activity!!.theme
                )
            val lineData = LineData(lineDataSet)
            powerLineChart.apply {
                data = lineData
                notifyDataSetChanged()
                invalidate()
            }
        })

        val differTLineChart = dataBinding.differTLineChart.apply {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            description.text = "?????? / s"
        }
        viewModel.differTLineChatData.value!!.clear()
        viewModel.differTLineChatData.value = ArrayList(Constant.MaxPointNumberMeanwhile)
        viewModel.differTLineChatData.observe(this, Observer { arrayList ->
            val lineDataSet = LineDataSet(arrayList, "????????? / ???")
            lineDataSet.setDrawFilled(true)
            lineDataSet.fillDrawable =
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.fill_differt_chart,
                    activity!!.theme
                )
            val lineData = LineData(lineDataSet)
            differTLineChart.apply {
                data = lineData
                notifyDataSetChanged()
                invalidate()
            }
        })

        val revLineChart = dataBinding.revLineChart.apply {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            description.text = "?????? / s"
        }
        viewModel.revLineChatData.value!!.clear()
        viewModel.revLineChatData.value = ArrayList(Constant.MaxPointNumberMeanwhile)
        viewModel.revLineChatData.observe(this, Observer { arrayList ->
            val lineDataSet = LineDataSet(arrayList, "?????? / rpm")
            lineDataSet.setDrawFilled(true)
            lineDataSet.fillDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.fill_rev_chart, activity!!.theme)
            val lineData = LineData(lineDataSet)
            revLineChart.apply {
                data = lineData
                notifyDataSetChanged()
                invalidate()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> activity!!.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun log(any: Any) {
        debug(logTag, any)
        var command = viewModel.commandText.value!!
        command += any.toString() + '\n'
        viewModel.commandText.postValue(prepareCommand(command))
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