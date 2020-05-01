package com.aim2u.kotlineatitv2client.ui.foodlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2client.Adapter.MyFoodListAdapter
import com.aim2u.kotlineatitv2client.R

class FoodListFragment : Fragment() {

    private lateinit var foodListViewModel: FoodListViewModel

    var recyler_food_list : RecyclerView?=null
    var layoutAnimationController : LayoutAnimationController?=null

    var adapter : MyFoodListAdapter?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodListViewModel =
            ViewModelProviders.of(this).get(FoodListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_list, container, false)
        initViews(root)
        foodListViewModel.getMutableFoodModelListData().observe(this, Observer {
            adapter = MyFoodListAdapter(context!!, it)
            recyler_food_list!!.adapter = adapter
            recyler_food_list!!.layoutAnimation = layoutAnimationController
        })
        return root
    }

    private fun initViews(root: View?) {
        recyler_food_list = root!!.findViewById(R.id.recycler_food_list) as RecyclerView
        recyler_food_list!!.setHasFixedSize(true)
        recyler_food_list!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
    }
}