package com.example.mysamsungproject.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mysamsungproject.MyListAdapter
import com.example.mysamsungproject.R
import com.example.mysamsungproject.Widgets
import com.example.mysamsungproject.databinding.FragmentMainBinding
import com.google.gson.GsonBuilder


class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private lateinit var widgets: List<Widgets>
    private lateinit var rec_view: RecyclerView

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        val view = binding.root

        rec_view = binding.recyclerView
        val result = requireActivity().assets.open("widgets.json").bufferedReader().use { it.readText() }
        Log.d("RRR",result.toString())

        val gson = GsonBuilder().create()
        widgets = gson.fromJson(result,Array<Widgets>::class.java).toList()
        Log.d("RRR",widgets.toString())

        rec_view.layoutManager = GridLayoutManager(requireContext(),2)
        val adapter=MyListAdapter(widgets)
        rec_view.adapter = adapter
        adapter.setOnItemClickListener(object : MyListAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                if (widgets[position].name == "Фото") {

                    findNavController().navigate(R.id.action_mainFragment_to_photoWidgetMainFragment3)
                }

            }
        })

        return view
    }


}