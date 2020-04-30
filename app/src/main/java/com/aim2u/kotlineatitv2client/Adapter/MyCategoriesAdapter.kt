package com.aim2u.kotlineatitv2client.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2client.Common.Common
import com.aim2u.kotlineatitv2client.Model.CategoryModel
import com.aim2u.kotlineatitv2client.Model.PopularCategoryModel
import com.aim2u.kotlineatitv2client.R
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class MyCategoriesAdapter (internal var context: Context,
                           internal var categoriesList: List<CategoryModel>):
    RecyclerView.Adapter<MyCategoriesAdapter.MyViewHolder>() {

    inner class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var category_name: TextView?= null
        var category_image:  ImageView?= null

        init {
            category_name = itemView.findViewById(R.id.category_name) as TextView
            category_image = itemView.findViewById(R.id.category_image) as ImageView
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyCategoriesAdapter.MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_category_item,parent,false))
    }

    override fun getItemCount(): Int {
        return categoriesList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context)
            .load(categoriesList.get(position).image)
            .into(holder.category_image!!)
        holder.category_name!!.setText(categoriesList.get(position).name)
    }

    override fun getItemViewType(position: Int): Int {
        return if(categoriesList.size == 1)
            Common.DEFAULT_COLUMN_COUNT
        else {
            if(categoriesList.size % 2 == 0)
                Common.DEFAULT_COLUMN_COUNT
            else
                if(position > 1 && position == categoriesList.size-1)
                    Common.FULL_WIDTH_COLUMN
            else Common.DEFAULT_COLUMN_COUNT
        }
    }
}