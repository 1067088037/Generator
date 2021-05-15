package edu.scut.generator.ui.main

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.wandersnail.bluetooth.EventObserver
import cn.wandersnail.commons.observer.Observe
import edu.scut.generator.R
import edu.scut.generator.databinding.MainFragmentBinding
import edu.scut.generator.global.Constant
import edu.scut.generator.global.debug
import edu.scut.generator.global.prepareCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.log

class MainFragment : Fragment(), GeneratorRecyclerAdapter.IGeneratorRecyclerAdapter {

    private val logTag = "MainFragment"
    private lateinit var viewModel: MainViewModel
    private lateinit var dataBinding: MainFragmentBinding

    private lateinit var recyclerView: RecyclerView
    private var lastNotifyDataTime = SystemClock.elapsedRealtime()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(
            activity!!,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        ).get(MainViewModel::class.java)
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false)
        dataBinding.data = viewModel
        dataBinding.lifecycleOwner = this

        (activity!! as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        (activity!! as AppCompatActivity).supportActionBar!!.title = "发电机控制器"

        viewModel.bluetoothDiscovering.observe(this, Observer {
            dataBinding.mainDiscovering.visibility = it
        })
        recyclerView = dataBinding.mainRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.generatorItemList.observe(this, Observer {
            if (SystemClock.elapsedRealtime() - lastNotifyDataTime >= 500L || viewModel.mustNotifyDataSetChanged.value!!) {
                recyclerView.adapter!!.notifyDataSetChanged()
                lastNotifyDataTime = SystemClock.elapsedRealtime()
                viewModel.mustNotifyDataSetChanged.value = false
            }
        })
        recyclerView.adapter = GeneratorRecyclerAdapter(viewModel.generatorItemList, this)
//        Constant.defaultGeneratorList.forEach {
//            it.iconId = R.drawable.ic_generator
//        }
//        viewModel.generatorItemList.value = Constant.defaultGeneratorList.toMutableList()

        return dataBinding.root
    }

    override fun onItemClick(item: GeneratorItem) {
        if (activity != null) {
            activity!!.supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.from_right_in,
                    R.anim.from_right_out,
                    R.anim.from_right_in,
                    R.anim.from_right_out
                )
                .replace(R.id.container, DetailFragment.newInstance(item))
                .addToBackStack("MainFragment").commit()
        }
    }

    private fun log(any: Any) {
        debug(logTag, any)
        var command = viewModel.commandText.value!!
        command += any.toString() + '\n'
        viewModel.commandText.postValue(prepareCommand(command))
    }

    companion object {
        fun newInstance() = MainFragment()
    }
}