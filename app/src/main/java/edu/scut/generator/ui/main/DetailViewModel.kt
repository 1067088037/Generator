package edu.scut.generator.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.Entry

class DetailViewModel : ViewModel() {

    var lineCharData = MutableLiveData<ArrayList<Entry>>(arrayListOf())

}