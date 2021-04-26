package edu.scut.generator.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.scut.generator.R
import edu.scut.generator.databinding.MainFragmentBinding
import edu.scut.generator.global.Constant
import edu.scut.generator.global.debug

class MainFragment : Fragment(), GeneratorRecyclerAdapter.IGeneratorRecyclerAdapter {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var dataBinding: MainFragmentBinding

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false)
        dataBinding.data = viewModel
        dataBinding.lifecycleOwner = this
        recyclerView = dataBinding.mainRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.generatorItemList.observe(this) {
            recyclerView.adapter!!.notifyDataSetChanged()
        }
        recyclerView.adapter = GeneratorRecyclerAdapter(viewModel.generatorItemList, this)
        Constant.defaultGeneratorList.forEach {
            it.iconId = R.drawable.ic_generator
        }
        viewModel.generatorItemList.value = Constant.defaultGeneratorList
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
                .addToBackStack(null).commit()
        }
    }

}