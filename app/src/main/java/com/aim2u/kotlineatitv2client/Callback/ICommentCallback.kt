package com.aim2u.kotlineatitv2client.Callback

import com.aim2u.kotlineatitv2client.Model.CategoryModel
import com.aim2u.kotlineatitv2client.Model.CommentModel

interface ICommentCallback {
    fun onCommentLoadSuccess(commentList:List<CommentModel>)
    fun onCommentLoadFailed(message:String)
}
