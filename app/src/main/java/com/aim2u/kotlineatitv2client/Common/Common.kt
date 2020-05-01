package com.aim2u.kotlineatitv2client.Common

import com.aim2u.kotlineatitv2client.Model.CategoryModel
import com.aim2u.kotlineatitv2client.Model.UserModel

object Common{
    var categorySelected: CategoryModel?=null
    val CATEGORY_REF: String = "Category"
    val FULL_WIDTH_COLUMN: Int = 1
    val DEFAULT_COLUMN_COUNT: Int = 0
    val BEST_DEAL_REF: String = "BestDeals"
    val POPULAR_REF: String = "MostPopular"
    val USER_REFERENCE = "Users"
    var currentUser:UserModel?=null
}