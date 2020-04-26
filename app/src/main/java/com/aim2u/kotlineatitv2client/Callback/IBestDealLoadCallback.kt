package com.aim2u.kotlineatitv2client.Callback

import com.aim2u.kotlineatitv2client.Model.BestDealModel
import com.aim2u.kotlineatitv2client.Model.PopularCategoryModel

interface IBestDealLoadCallback {
    fun onBestDealSuccess(bestDealList:List<BestDealModel>)
    fun onBestDealFailed(message:String)
}