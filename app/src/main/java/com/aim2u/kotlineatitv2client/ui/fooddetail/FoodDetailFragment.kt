package com.aim2u.kotlineatitv2client.ui.fooddetail

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.aim2u.kotlineatitv2client.Common.Common
import com.aim2u.kotlineatitv2client.Database.CartDataSource
import com.aim2u.kotlineatitv2client.Database.CartDatabase
import com.aim2u.kotlineatitv2client.Database.CartItem
import com.aim2u.kotlineatitv2client.Database.LocalCartDataSource
import com.aim2u.kotlineatitv2client.EventBus.CountCartEvent
import com.aim2u.kotlineatitv2client.Model.CommentModel
import com.aim2u.kotlineatitv2client.Model.FoodModel
import com.aim2u.kotlineatitv2client.R
import com.aim2u.kotlineatitv2client.ui.comment.CommentFragment
import com.andremion.counterfab.CounterFab
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.google.gson.Gson
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class FoodDetailFragment : Fragment(), TextWatcher {

    private lateinit var foodDetailViewModel: FoodDetailViewModel

    private lateinit var addOnBottomSheetDialog : BottomSheetDialog

    private var img_food:ImageView?=null
    private var btnCart:CounterFab?=null
    private var btnRating:FloatingActionButton?=null
    private var food_name:TextView?=null
    private var food_description:TextView?=null
    private var food_price:TextView?=null
    private var number_button:ElegantNumberButton?=null
    private var ratingBar:RatingBar?=null
    private var btnShowComment:Button?=null
    private var rdi_group_size:RadioGroup?=null
    private var img_add_on:ImageView?=null
    private var chip_group_user_selected_addon:ChipGroup?=null

    //Addon layout
    private var chip_group_addon:ChipGroup?=null
    private var edt_search_addon:EditText?=null

    private var waitingDialog:AlertDialog?=null

    private val compositeDisposable = CompositeDisposable()
    private lateinit var cartDataSource : CartDataSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodDetailViewModel =
            ViewModelProviders.of(this).get(FoodDetailViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_detail, container, false)

        initViews(root)
        foodDetailViewModel.getMutableLiveDataFood().observe(this, Observer {
            displayInfo(it)
        })

        foodDetailViewModel.getMutableLiveDataComment().observe(this, Observer {
            submitRatingToFirebase(it)
        })
        return root
    }

    //pt9
    private fun submitRatingToFirebase(commentModel: CommentModel?) {
        waitingDialog!!.show()

        //first we will update to Comment Ref
        FirebaseDatabase.getInstance()
            .getReference(Common.COMMENT_REF)
            .child(Common.foodSelected!!.id!!)
            .push()
            .setValue(commentModel)
            .addOnCompleteListener{task ->
                if(task.isSuccessful)
                {
                    addRatingToFood(commentModel!!.ratingValue.toDouble())
                }
                waitingDialog!!.dismiss()

            }
    }

    private fun addRatingToFood(ratingValue: Double) {
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)//Select Category
            .child(Common.categorySelected!!.menu_id!!)// Select menu in category
            .child("foods")//Select foods array
            .child(Common.foodSelected!!.key!!) //Select Key
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    waitingDialog!!.dismiss()

                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()){
                        val foodModel = dataSnapshot.getValue(FoodModel::class.java)
                        foodModel!!.key = Common.foodSelected!!.key
                        //Apply rating
                        val sumRating = foodModel.ratingValue+ratingValue
                        val ratingCount = foodModel.ratingCount+1
                        val result = sumRating/ratingCount

                        val updateData = HashMap<String, Any>()
                        updateData["ratingValue"] = result
                        updateData["ratingCount"] = ratingCount

                        //Update data in variable (pt9)
                        foodModel.ratingCount = ratingCount
                        foodModel.ratingValue = result

                        dataSnapshot.ref
                            .updateChildren(updateData)
                            .addOnCompleteListener { task ->
                                waitingDialog!!.dismiss()
                                if(task.isSuccessful){
                                    Common.foodSelected = foodModel
                                    foodDetailViewModel!!.setFoodModel(foodModel)
                                    Toast.makeText(context!!,""+"Thank You",Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }

            })
    }

    private fun displayInfo(it: FoodModel?) {
        Glide.with(context!!).load(it!!.image).into(img_food!!)
        food_name!!.text = StringBuilder(it!!.name!!)
        food_description!!.text = StringBuilder(it!!.description!!)
        food_price!!.text = StringBuilder(it!!.price!!.toString())

        ratingBar!!.rating = it!!.ratingValue.toFloat()

        //set Size
        for(sizeModel in it!!.size!!){
            val radioButton = RadioButton(context)
            radioButton.setOnCheckedChangeListener{compoundButton, b ->
                if(b)
                    Common.foodSelected!!.userSelectedSize = sizeModel
                calculateTotalPrice()
            }
            val params = LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.MATCH_PARENT,1.0f)
            radioButton.layoutParams = params
            radioButton.text = sizeModel.name
            radioButton.tag = sizeModel.price

            rdi_group_size!!.addView(radioButton)
        }

        //Default first radio button select
        if(rdi_group_size!!.childCount > 0){
            val radioButton = rdi_group_size!!.getChildAt(0) as RadioButton
            radioButton.isChecked = true
        }
    }

    private fun calculateTotalPrice() {
        var totalPrice = Common.foodSelected!!.userSelectedSize!!.price!!.toDouble()
        var displayPrice = 0.0

        //Addon (pt12)
        if (Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0){
            for(addOnModel in Common.foodSelected!!.userSelectedAddon!!)
                totalPrice += addOnModel.price.toDouble()
        }

        //Size
        totalPrice += Common.foodSelected!!.userSelectedSize!!.price!!.toDouble()

        displayPrice = totalPrice * number_button!!.number.toInt()
        displayPrice = Math.round(displayPrice * 100.0)/100.0

        food_price!!.text = StringBuilder("").append(Common.formatPrice(displayPrice)).toString()
    }

    private fun initViews(root: View?) {

        //pt15
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDao())


        addOnBottomSheetDialog = BottomSheetDialog(context!!, R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display,null)
        chip_group_addon = layout_user_selected_addon.findViewById(R.id.chip_group_addon) as ChipGroup
        edt_search_addon = layout_user_selected_addon.findViewById(R.id.edt_search) as EditText
        addOnBottomSheetDialog.setContentView(layout_user_selected_addon)

        addOnBottomSheetDialog.setOnDismissListener { dialogInterface ->
            displayUserSelectedAddon()
            calculateTotalPrice()
        }

        waitingDialog = SpotsDialog.Builder().setContext(context)
            .setCancelable(false).build()
        btnCart = root!!.findViewById(R.id.btnCart) as CounterFab
        img_food = root!!.findViewById(R.id.img_food) as ImageView
        btnRating = root!!.findViewById(R.id.btn_rating) as FloatingActionButton
        food_name = root!!.findViewById(R.id.food_name) as TextView
        food_description = root!!.findViewById(R.id.food_description) as TextView
        food_price = root!!.findViewById(R.id.food_price) as TextView
        number_button = root!!.findViewById(R.id.number_button) as ElegantNumberButton
        ratingBar = root!!.findViewById(R.id.ratingBar) as RatingBar
        btnShowComment = root!!.findViewById(R.id.btnShowComment) as Button
        rdi_group_size = root!!.findViewById(R.id.rdi_group_size) as RadioGroup
        img_add_on = root!!.findViewById(R.id.img_add_addon) as ImageView
        chip_group_user_selected_addon = root!!.findViewById(R.id.chip_group_user_selected_addon)as ChipGroup

        //Event (pt12)

        img_add_on!!.setOnClickListener {
            if(Common.foodSelected!!.addon != null){
                displayAllAddon()
                addOnBottomSheetDialog.show()
            }
        }


        //Event (pt9)
        btnRating?.setOnClickListener {
            showDialogRating()
        }

        //Event (pt10)
        btnShowComment?.setOnClickListener{
            val commentFragment = CommentFragment.getInstance()
            commentFragment.show(activity!!.supportFragmentManager,"CommentFragment")
        }

        btnCart?.setOnClickListener{
            val cartItem = CartItem()

            cartItem?.uid = Common.currentUser?.uid
            cartItem?.userPhone = Common.currentUser?.phone

            cartItem.foodId = Common.foodSelected?.id!!
            cartItem.foodName = Common.foodSelected?.name!!
            cartItem.foodImage = Common.foodSelected?.image!!
            cartItem.foodPrice = Common.foodSelected?.price!!.toDouble()
            cartItem.foodQuantity = number_button!!.number.toInt()
            cartItem.foodExtraPrice = Common.calculateExtraPrice(Common.foodSelected!!.userSelectedSize, Common.foodSelected!!.userSelectedAddon)
            if(Common.foodSelected!!.userSelectedAddon != null)
                cartItem.foodAddon = Gson().toJson(Common.foodSelected!!.userSelectedAddon)
            else
                cartItem.foodAddon = "Default"

            if(Common.foodSelected!!.userSelectedSize != null)
                cartItem.foodSize = Gson().toJson(Common.foodSelected!!.userSelectedSize)
            else
                cartItem.foodSize = "Default"


            cartDataSource.getItemWithAllOptionsInCart(Common.currentUser!!.uid!!,
                cartItem.foodId,
                cartItem.foodSize!!,
                cartItem.foodAddon!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe (object : SingleObserver<CartItem> {
                    override fun onSuccess(cartItemFromDB: CartItem) {
                        if (cartItemFromDB.equals(cartItem)){
                            //If Item Already in database, just update
                            cartItemFromDB.foodExtraPrice = cartItem.foodExtraPrice
                            cartItemFromDB.foodAddon = cartItem.foodAddon
                            cartItemFromDB.foodSize = cartItem.foodSize
                            cartItemFromDB.foodQuantity = cartItemFromDB.foodQuantity + cartItem.foodQuantity

                            cartDataSource.updateCart(cartItemFromDB)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe (object : SingleObserver<Int> {
                                    override fun onSuccess(t: Int) {
                                        Toast.makeText(context,"Update Cart Success",Toast.LENGTH_SHORT).show()
                                        EventBus.getDefault().postSticky(CountCartEvent(true))
                                    }

                                    override fun onSubscribe(d: Disposable) {

                                    }

                                    override fun onError(e: Throwable) {
                                        Toast.makeText(context,"UPDATE CART"+e.message,Toast.LENGTH_SHORT).show()
                                    }

                                })
                        } else {
                            //if item is not available in database, just insert
                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    Toast.makeText(context,"Add to cart success",Toast.LENGTH_SHORT).show()
                                    //Here we wi send a notify to MainActivity to update counterFab
                                    EventBus.getDefault().postSticky(
                                        CountCartEvent(true)
                                    )
                                },{
                                        t: Throwable? -> Toast.makeText(context,"[INSERT CART]"+t!!.message,Toast.LENGTH_SHORT).show()
                                }))
                        }
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        if (e.message!!.contains("empty")){
                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    Toast.makeText(context,"Add to cart success",Toast.LENGTH_SHORT).show()
                                    //Here we wi send a notify to MainActivity to update counterFab
                                    EventBus.getDefault().postSticky(
                                        CountCartEvent(true)
                                    )
                                },{
                                        t: Throwable? -> Toast.makeText(context,"[INSERT CART]"+t!!.message,Toast.LENGTH_SHORT).show()
                                }))
                        } else {
                            Toast.makeText(context,"[CART ERROR]"+e.message,Toast.LENGTH_SHORT).show()
                        }
                    }


                })
        }

    }

    private fun displayAllAddon() {
        if(Common.foodSelected!!.addon!!.size > 0){
            chip_group_addon!!.clearCheck()
            chip_group_addon!!.removeAllViews()

            edt_search_addon!!.addTextChangedListener(this)

            for(addOnModel in Common.foodSelected!!.addon!!){
                val chip = layoutInflater.inflate(R.layout.layout_chip,null,false)as Chip
                chip.text = StringBuilder(addOnModel.name!!).append("(+$").append(addOnModel.price).append(")").toString()
                chip.setOnCheckedChangeListener { compoundButton, b ->
                    if(b){
                        if(Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                        Common.foodSelected!!.userSelectedAddon!!.add(addOnModel)
                    }
                }
                chip_group_addon!!.addView(chip)
            }
        }
    }

    private fun displayUserSelectedAddon() {
        if(Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0){
            chip_group_user_selected_addon!!.removeAllViews()
            for (addonModel in Common.foodSelected!!.userSelectedAddon!!){
                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete,null,false) as Chip
                chip.text = StringBuilder(addonModel!!.name!!).append("$+").append(addonModel.price).append(")").toString()
                chip.isClickable = false
                chip.setOnCloseIconClickListener { view ->
                    chip_group_user_selected_addon!!.removeView(view)
                    Common.foodSelected!!.userSelectedAddon!!.remove(addonModel)
                    calculateTotalPrice()
                }
                chip_group_user_selected_addon!!.addView(chip)
            }
        } else if(Common.foodSelected!!.userSelectedAddon!!.size == 0)
            chip_group_user_selected_addon!!.removeAllViews()
    }

    private fun showDialogRating() {
        var builder = AlertDialog.Builder(context!!)
        builder.setTitle("Rating Food")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_rating_comment,null)

        val ratingBar = itemView.findViewById<RatingBar>(R.id.rating_bar)
        val edt_comment = itemView.findViewById<EditText>(R.id.edt_comment)

        builder.setView(itemView)
        builder.setNegativeButton("CANCEL"){dialogInterface, i -> dialogInterface.dismiss() }
        builder.setPositiveButton("OK"){dialogInterface, i ->
            val commentModel = CommentModel()
            commentModel.name = Common.currentUser!!.name
            commentModel.uid = Common.currentUser!!.uid
            commentModel.comment = edt_comment.text.toString()
            commentModel.ratingValue = ratingBar.rating
            val serverTimeStamp = HashMap<String,Any>()
            serverTimeStamp["timeStamp"] = ServerValue.TIMESTAMP
            commentModel.commentTimeStamp = serverTimeStamp

            foodDetailViewModel.setCommentModel(commentModel)
        }

        val dialog = builder.create()
        dialog.show()
    }

    override fun afterTextChanged(p0: Editable?) {
        TODO("Not yet implemented")
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        TODO("Not yet implemented")
    }

    override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
        chip_group_addon!!.clearCheck()
        chip_group_addon!!.removeAllViews()
        for(addOnModel in Common.foodSelected!!.addon!!){
            if (addOnModel.name!!.toLowerCase().contains(charSequence.toString().toLowerCase())){
                    val chip = layoutInflater.inflate(R.layout.layout_chip,null,false)as Chip
                    chip.text = StringBuilder(addOnModel.name!!).append("(+$").append(addOnModel.price).append(")").toString()
                    chip.setOnCheckedChangeListener { compoundButton, b ->
                        if(b){
                            if(Common.foodSelected!!.userSelectedAddon == null)
                                Common.foodSelected!!.userSelectedAddon = ArrayList()
                            Common.foodSelected!!.userSelectedAddon!!.add(addOnModel)
                        }
                    }
                    chip_group_addon!!.addView(chip)
                }

        }

    }
}