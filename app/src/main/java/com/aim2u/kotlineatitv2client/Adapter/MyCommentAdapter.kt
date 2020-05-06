package com.aim2u.kotlineatitv2client.Adapter


import android.content.Context
import android.text.format.DateUtils
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2client.Model.CommentModel
import com.aim2u.kotlineatitv2client.R
import kotlinx.android.synthetic.main.layout_comment_item.view.*

class MyCommentAdapter(internal var commentList: List<CommentModel>):RecyclerView.Adapter<MyCommentAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_comment_item,parent,false))
    }

    override fun getItemCount(): Int = commentList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) = holder.bind(commentList[position])
//    {
//        val timeStamp = commentList[position].commentTimeStamp.get("timeStamp")?.toString()?.toLong()
//        holder.txt_comment_date!!.text =  DateUtils.getRelativeTimeSpanString(timeStamp)
//        holder.txt_comment_name!!.text = commentList.get(position).name
//        holder.txt_comment!!.text = commentList.get(position).comment
//        holder.rating_bar!!.rating = commentList.get(position).ratingValue
//    }

    inner class MyViewHolder(itemView:View):RecyclerView.ViewHolder(itemView) {
        fun bind(commentList: CommentModel){
            with(itemView){
                val timeStamp = commentList.commentTimeStamp?.get("timeStamp")?.toString()?.toLong()
                timeStamp?.let {
                    txt_comment_date.text = DateUtils.getRelativeTimeSpanString(it)
                }
                txt_comment_name.text = commentList.name
                txt_comment.text = commentList.comment
                rating_bar.rating = commentList.ratingValue
            }
        }

    }





}