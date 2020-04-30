package com.aim2u.kotlineatitv2client.Callback

import com.aim2u.kotlineatitv2client.Model.CategoryModel

interface ICategoryCallbackListener {
    fun onCategoryLoadSuccess(categoriesList:List<CategoryModel>)
    fun onCategoryLoadFailed(message:String)
}
