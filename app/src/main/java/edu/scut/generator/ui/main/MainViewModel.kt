package edu.scut.generator.ui.main

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.wandersnail.bluetooth.BTManager
import cn.wandersnail.bluetooth.ConnectCallback
import cn.wandersnail.bluetooth.Connection
import cn.wandersnail.bluetooth.DiscoveryListener
import com.github.mikephil.charting.data.Entry
import edu.scut.generator.MainActivity
import edu.scut.generator.global.debug
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val bluetoothConnection = MutableLiveData<Connection>(null)
    val lastReadBluetoothTime = MutableLiveData(SystemClock.elapsedRealtime())

    val bluetoothDiscovering = MutableLiveData(View.INVISIBLE)
    val generatorItemList = MutableLiveData<List<GeneratorItem>>(arrayListOf())

    var thisGeneratorItem = MutableLiveData(GeneratorItem())
    var lineCharData = MutableLiveData<ArrayList<Entry>>(arrayListOf())
    fun getStateText(): String = GeneratorItem.stateToString(thisGeneratorItem.value!!.state)
    fun getStateTextColor(): Int = GeneratorItem.getStateTextColor(thisGeneratorItem.value!!.state)

    val discoveryListener: MutableLiveData<DiscoveryListener> = MutableLiveData()

}