package com.aim2u.kotlineatitv2client.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2client.Model.PopularCategoryModel
import com.aim2u.kotlineatitv2client.R
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.layout_popular_categories_item.view.*

class MyPopularCategoriesAdapter (internal var context:Context,
internal var popularCategoryModel: List<PopularCategoryModel>):RecyclerView.Adapter<MyPopularCategoriesAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_popular_categories_item,parent,false))
    }

    override fun getItemCount(): Int {
        return popularCategoryModel.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context)
            .load(popularCategoryModel.get(position).image)
            .into(holder.category_image!!)
        holder.category_name!!.setText(popularCategoryModel.get(position).name)

    }

    inner class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var category_name:TextView?= null
        var category_image: CircleImageView?= null

        init {
            category_name = itemView.findViewById(R.id.txt_category_name) as TextView
            category_image = itemView.findViewById(R.id.category_image  ) as CircleImageView
        }
    }
}