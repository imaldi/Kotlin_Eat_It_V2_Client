package com.aim2u.kotlineatitv2client.ui.fooddetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aim2u.kotlineatitv2client.Common.Common
import com.aim2u.kotlineatitv2client.Model.CommentModel
import com.aim2u.kotlineatitv2client.Model.FoodModel

class FoodDetailViewModel : ViewModel() {

    private var mutableLiveDataFood:MutableLiveData<FoodModel>?=null
    private var mutableLiveDataComment:MutableLiveData<CommentModel>?=null

    init {
        mutableLiveDataComment = MutableLiveData()
    }

    fun getMutableLiveDataFood():MutableLiveData<FoodModel>{
        if (mutableLiveDataFood == null)
            mutableLiveDataFood = MutableLiveData()

        mutableLiveDataFood!!.value = Common.foodSelected
        return mutableLiveDataFood!!
    }

    fun getMutableLiveDataComment():MutableLiveData<CommentModel>{
        if (mutableLiveDataComment == null)
            mutableLiveDataComment = MutableLiveData()

//        mutableLiveDataComment!!.value = Common
        return mutableLiveDataComment!!
    }

    fun setCommentModel(commentModel: CommentModel) {
        if(mutableLiveDataComment != null)
            mutableLiveDataComment!!.value = commentModel
    }

    fun setFoodModel(foodModel: FoodModel) {
        if (mutableLiveDataFood != null)
            mutableLiveDataFood!!.value = foodModel
    }

}