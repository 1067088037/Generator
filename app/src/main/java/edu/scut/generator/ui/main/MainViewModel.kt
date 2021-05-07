package edu.scut.generator.ui.main

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.wandersnail.bluetooth.Connection
import com.github.mikephil.charting.data.Entry

class MainViewModel : ViewModel() {

    val bluetoothConnection = MutableLiveData<Connection>(null)

    val bluetoothDiscovering = MutableLiveData(View.INVISIBLE)
    val generatorItemList = MutableLiveData<ArrayList<GeneratorItem>>(arrayListOf())

    var thisGeneratorItem = MutableLiveData(GeneratorItem())
    var lineCharData = MutableLiveData<ArrayList<Entry>>(arrayListOf())
    fun getStateText(): String = GeneratorItem.stateToString(thisGeneratorItem.value!!.state)
    fun getStateTextColor(): Int = GeneratorItem.getStateTextColor(thisGeneratorItem.value!!.state)
    fun getPowerText(): String = String.format("%.3f W", thisGeneratorItem.value!!.power)

}