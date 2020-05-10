package com.aim2u.kotlineatitv2client.ui.view_orders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aim2u.kotlineatitv2client.Model.Order

class ViewOrderModel : ViewModel(){
    var mutableLiveDataOrderList:MutableLiveData<List<Order>>
    init {
        mutableLiveDataOrderList = MutableLiveData()
    }

    fun setMutableLiveDataOrderList(orderList: List<Order>){
        mutableLiveDataOrderList.value = orderList
    }
}