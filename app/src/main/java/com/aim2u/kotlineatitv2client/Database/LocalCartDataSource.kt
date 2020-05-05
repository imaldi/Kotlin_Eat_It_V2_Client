package com.aim2u.kotlineatitv2client.Database

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

class LocalCartDataSource (private val cartDAO: CartDAO):CartDataSource{
    override fun getAllCart(uid: String): Flowable<List<CartItem>> {
        return cartDAO.getAllCart(uid)
    }

    override fun countItemInCart(uid: String): Single<Int> {
        return cartDAO.countItemInCart(uid)
    }

    override fun sumPrice(uid: String): Single<Long> {
        return cartDAO.sumPrice(uid)
    }

    override fun getItemInCart(foodId: String, uid: String): Single<CartItem> {
        return cartDAO.getItemInCart(foodId,uid)
    }

    override fun insertOrReplaceAll(vararg cartItem: CartItem): Completable {
        return cartDAO.insertOrReplaceAll(*cartItem)
    }

    override fun updateCart(cartItem: CartItem): Single<Int> {
        return cartDAO.updateCart(cartItem)
    }

    override fun deleteCart(cartItem: CartItem): Single<Int> {
        return cartDAO.deleteCart(cartItem)
    }

    override fun cleanCart(uid: String): Single<Int> {
        return cartDAO.cleanCart(uid)
    }

}