package edu.scut.generator.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    val generatorItemList = MutableLiveData<ArrayList<GeneratorItem>>(arrayListOf())

}