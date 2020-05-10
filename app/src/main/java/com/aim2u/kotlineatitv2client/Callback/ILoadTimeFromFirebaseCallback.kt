package com.aim2u.kotlineatitv2client.Callback

import com.aim2u.kotlineatitv2client.Model.Order

interface ILoadTimeFromFirebaseCallback {
    fun onLoadTimeSuccess(order: Order, estimatedTimeMs:Long)
    fun onLoadTimeFailed(message:String)
}