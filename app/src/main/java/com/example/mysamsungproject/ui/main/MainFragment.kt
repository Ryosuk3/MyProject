package com.example.mysamsungproject.ui.main

import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mysamsungproject.MyListAdapter
import com.example.mysamsungproject.R
import com.example.mysamsungproject.Settings
import com.example.mysamsungproject.Widgets
import com.example.mysamsungproject.databinding.FragmentMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.GsonBuilder


class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private lateinit var widgets: List<Widgets>
    private lateinit var rec_view: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        auth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                binding.navigationView.menu.findItem(R.id.nav_item1).isEnabled = false
                binding.navigationView.menu.findItem(R.id.nav_item1).title = user.email
                binding.navigationView.menu.findItem(R.id.nav_item3).isEnabled = true
            } else {
                binding.navigationView.menu.findItem(R.id.nav_item1).isEnabled = true
                binding.navigationView.menu.findItem(R.id.nav_item1).title = "Login"
                binding.navigationView.menu.findItem(R.id.nav_item3).isEnabled = false
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        val view = binding.root

        rec_view = binding.recyclerView
        val result = requireActivity().assets.open("widgets.json").bufferedReader().use { it.readText() }
        Log.d("RRR", result.toString())

        val gson = GsonBuilder().create()
        widgets = gson.fromJson(result, Array<Widgets>::class.java).toList()
        Log.d("RRR", widgets.toString())

        rec_view.layoutManager = GridLayoutManager(requireContext(), 2)
        val adapter = MyListAdapter(widgets)
        rec_view.adapter = adapter
        adapter.setOnItemClickListener(object : MyListAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                if (widgets[position].name == "Фото") {
                    findNavController().navigate(R.id.action_mainFragment_to_photoWidgetMainFragment3)
                }
            }
        })

        binding.menu.setOnClickListener {

            binding.drawerLayout.openDrawer(GravityCompat.START)
        }


        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_item1 -> {
                    findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
                    true
                }
                R.id.nav_item2 -> {
                    val photoWidgetViewModel = ViewModelProvider(requireActivity()).get(PhotoWidgetMainViewModel::class.java)
                    photoWidgetViewModel.loadSettingsFromFirebase()
                    true
                }
                R.id.nav_item3 -> {
                    auth.signOut()
                    true
                }
                else -> false
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }


}