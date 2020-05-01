package com.aim2u.kotlineatitv2client.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2client.Callback.IRecyclerItemClickListener
import com.aim2u.kotlineatitv2client.Common.Common
import com.aim2u.kotlineatitv2client.Model.FoodModel
import com.aim2u.kotlineatitv2client.R
import com.bumptech.glide.Glide
import org.greenrobot.eventbus.EventBus

class MyFoodListAdapter (internal var context: Context,
                           internal var foodList: List<FoodModel>):
    RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>() {

    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var txt_food_name: TextView?= null
        var txt_food_price: TextView?= null
        var img_food_image:  ImageView?= null
        var img_food_fav:  ImageView?= null
        var img_food_cart:  ImageView?= null

        init {
            txt_food_name = itemView.findViewById(R.id.txt_food_name) as TextView
            txt_food_price = itemView.findViewById(R.id.txt_food_price) as TextView
            img_food_image = itemView.findViewById(R.id.img_food_image) as ImageView
            img_food_fav = itemView.findViewById(R.id.img_fav) as ImageView
            img_food_cart = itemView.findViewById(R.id.img_quick_cart) as ImageView

        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyFoodListAdapter.MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_food_item,parent,false))
    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    override fun onBindViewHolder(holder: MyFoodListAdapter.MyViewHolder, position: Int) {
        Glide.with(context)
            .load(foodList.get(position).image)
            .into(holder.img_food_image!!)
        holder.txt_food_name!!.setText(foodList.get(position).name)
        holder.txt_food_price!!.setText(foodList.get(position).price.toString())

    }


}