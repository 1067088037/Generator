package edu.scut.generator.ui.main

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
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
import java.util.*

class MainFragment : Fragment(), GeneratorRecyclerAdapter.IGeneratorRecyclerAdapter, EventObserver {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val logTag = MainFragment::class.java.name
    private lateinit var viewModel: MainViewModel
    private lateinit var dataBinding: MainFragmentBinding

    private lateinit var recyclerView: RecyclerView

    private val readBytes: Queue<Byte> = LinkedList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(activity!!).get("MainViewModel", MainViewModel::class.java)
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
            recyclerView.adapter!!.notifyDataSetChanged()
        })
        recyclerView.adapter = GeneratorRecyclerAdapter(viewModel.generatorItemList, this)
        Constant.defaultGeneratorList.forEach {
            it.iconId = R.drawable.ic_generator
        }
        viewModel.generatorItemList.value = Constant.defaultGeneratorList

        return dataBinding.root
    }

    @Observe
    override fun onRead(device: BluetoothDevice, value: ByteArray) {
        super.onRead(device, value)
        debug(
            "onRead, device = ${device.name}, " +
                    "value = ${value.toTypedArray().contentDeepToString()}"
        )
        if (device == viewModel.bluetoothConnection.value?.device) {
            value.forEach {
                readBytes.offer(it) //将收到的信息插入队列
            }
        }
        while (readBytes.isNotEmpty() && readBytes.peek() != Constant.MessageStartCode) {
            readBytes.poll() //保持队列的顶端是起始符
        }
        if (readBytes.contains(Constant.MessageStopCode)) {
            var input = ""
            while (true) {
                val poll = readBytes.poll()!! //载入从起始符到终止符之间的内容
                input += poll.toChar()
                if (poll == Constant.MessageStopCode) break
            }
            val generators = GeneratorItem.decodeGeneratorArray(input) //解码成发电机信息
            debug("收到发电机信息 = ${generators.contentDeepToString()}")
        }
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

    private fun debug(any: Any) = debug(logTag, any)

}