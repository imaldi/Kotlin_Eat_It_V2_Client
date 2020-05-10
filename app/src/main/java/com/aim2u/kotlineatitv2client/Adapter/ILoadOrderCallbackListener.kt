package com.aim2u.kotlineatitv2client.Adapter

import com.aim2u.kotlineatitv2client.Model.Order

interface ILoadOrderCallbackListener {
    fun onLoadOrderSuccess(orderList:List<Order>)
    fun onLoadOrderFailed(message:String)
}