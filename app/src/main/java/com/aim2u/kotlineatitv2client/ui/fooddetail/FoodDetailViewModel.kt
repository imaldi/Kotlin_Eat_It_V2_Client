package com.aim2u.kotlineatitv2client.ui.fooddetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aim2u.kotlineatitv2client.Common.Common
import com.aim2u.kotlineatitv2client.Model.FoodModel

class FoodDetailViewModel : ViewModel() {

    private var mutableLiveDataFood:MutableLiveData<FoodModel>?=null
    fun getMutableLiveDataFood():MutableLiveData<FoodModel>{
        if (mutableLiveDataFood == null)
            mutableLiveDataFood = MutableLiveData()

        mutableLiveDataFood!!.value = Common.foodSelected
        return mutableLiveDataFood!!
    }

}