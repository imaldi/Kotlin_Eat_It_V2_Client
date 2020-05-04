package com.aim2u.kotlineatitv2client.Model

class FoodModel{
    var key: String?=null
    var name: String?= null
    var image: String?= null
    var id: String?= null
    var description: String?= null
    var price: Long   = 0
    var addon: List<AddonModel>? = ArrayList<AddonModel>()
    var size: List<SizeModel>?  = ArrayList<SizeModel>()

    //pt9
    var ratingValue: Double = 0.toDouble()
    var ratingCount: Long = 0.toLong()

    //pt11
    var userSelectedAddon:MutableList<AddonModel>?=null
    var userSelectedSize:SizeModel?=null
}
