package com.aim2u.kotlineatitv2client.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2client.Adapter.MyBestDealAdapter
import com.aim2u.kotlineatitv2client.Adapter.MyPopularCategoriesAdapter
import com.aim2u.kotlineatitv2client.R
import com.asksira.loopingviewpager.LoopingViewPager

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel


    var recylerView:RecyclerView? = null
    var viewPager:LoopingViewPager? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)


        homeViewModel.popularList.observe(viewLifecycleOwner, Observer {
            val listData = it
            val adapter = MyPopularCategoriesAdapter(context!!, listData)
            recylerView!!.adapter = adapter
        })

        homeViewModel.bestDealList.observe(viewLifecycleOwner, Observer {
            val adapter = MyBestDealAdapter(context!!,it,false)
            viewPager!!.adapter = adapter
        })
        initView(root)
        return root
    }

    private fun initView(root: View) {
        viewPager = root.findViewById(R.id.view_pager)
        recylerView = root.findViewById(R.id.recyler_popular) as RecyclerView
        recylerView!!.setHasFixedSize(true)
        recylerView!!.layoutManager = LinearLayoutManager(context,RecyclerView.HORIZONTAL,false)
    }
}