package edu.scut.generator.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.Entry

class DetailViewModel : ViewModel() {

    var thisGeneratorItem = MutableLiveData(GeneratorItem())
    var lineCharData = MutableLiveData<ArrayList<Entry>>(arrayListOf())

    fun getStateText(): String = GeneratorItem.stateToString(thisGeneratorItem.value!!.state)
    fun getStateTextColor(): Int = GeneratorItem.getStateTextColor(thisGeneratorItem.value!!.state)
    fun getPowerText(): String = String.format("%.3f W", thisGeneratorItem.value!!.power)

}