package com.aim2u.kotlineatitv2client.Callback

import com.aim2u.kotlineatitv2client.Model.PopularCategoryModel

interface IPopularLoadCallback {
    fun onPopularLoadSuccess(popularModelList:List<PopularCategoryModel>)
    fun onPopularLoadFailed(message:String)
}