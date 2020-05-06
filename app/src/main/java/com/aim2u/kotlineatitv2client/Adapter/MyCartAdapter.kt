package com.aim2u.kotlineatitv2client.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2client.Database.CartDataSource
import com.aim2u.kotlineatitv2client.Database.CartDatabase
import com.aim2u.kotlineatitv2client.Database.CartItem
import com.aim2u.kotlineatitv2client.Database.LocalCartDataSource
import com.aim2u.kotlineatitv2client.EventBus.UpdateItemInCart
import com.aim2u.kotlineatitv2client.R
import com.bumptech.glide.Glide
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.layout_cart_item.view.*
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class MyCartAdapter (internal val context : Context,
                     internal val cartItems: List<CartItem>
):RecyclerView.Adapter<MyCartAdapter.MyViewHolder>(){

    internal var compositeDisposable:CompositeDisposable
    internal var cartDataSource:CartDataSource

    init {
        compositeDisposable = CompositeDisposable()
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDao())
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(cartItem: CartItem){
            with(itemView){
                Glide.with(context).load(cartItem.foodImage).into(img_cart)
                txt_food_name_cart.text = cartItem.foodName?.let { StringBuilder(it) }
                txt_food_price_cart.text = cartItem.foodPrice.let{ StringBuilder((it+cartItem.foodExtraPrice).toString())}
                number_button.number= cartItem.foodQuantity.toString()

                number_button.setOnValueChangeListener { _, _, newValue ->
                    cartItem.foodQuantity = newValue
                    EventBus.getDefault().postSticky(
                        UpdateItemInCart(
                            cartItem
                        )
                    )
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_cart_item,parent,false))
    }

    override fun getItemCount(): Int {
        return cartItems.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) = holder.bind(cartItems[position])
}