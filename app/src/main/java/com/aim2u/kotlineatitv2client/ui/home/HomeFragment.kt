package com.aim2u.kotlineatitv2client.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
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

    var layoutAnimationController:LayoutAnimationController? = null



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
            val adapter =
                MyPopularCategoriesAdapter(
                    requireContext(),
                    listData
                )
            recylerView!!.adapter = adapter
            recylerView!!.layoutAnimation = layoutAnimationController
        })

        homeViewModel.bestDealList.observe(viewLifecycleOwner, Observer {
            val adapter = MyBestDealAdapter(requireContext(),it,false)
            viewPager!!.adapter = adapter
        })
        initView(root)
        return root
    }

    private fun initView(root: View) {
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
        viewPager = root.findViewById(R.id.view_pager)
        recylerView = root.findViewById(R.id.recyler_popular) as RecyclerView
        recylerView!!.setHasFixedSize(true)
        recylerView!!.layoutManager = LinearLayoutManager(context,RecyclerView.HORIZONTAL,false)
    }
}