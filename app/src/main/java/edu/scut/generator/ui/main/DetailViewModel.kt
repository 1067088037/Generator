package edu.scut.generator.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.Entry

class DetailViewModel : ViewModel() {

    var thisGeneratorItem = MutableLiveData(GeneratorItem())
    var lineCharData = MutableLiveData<ArrayList<Entry>>(arrayListOf())

    fun getStateString(): String = GeneratorItem.stateToString(thisGeneratorItem.value!!.state)

    fun getPowerString(): String = "${String.format("%.3f", thisGeneratorItem.value!!.power)}W"

}