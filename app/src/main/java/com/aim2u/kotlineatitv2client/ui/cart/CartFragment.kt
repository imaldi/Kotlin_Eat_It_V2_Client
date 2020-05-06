package com.aim2u.kotlineatitv2client.ui.cart

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2client.Adapter.MyCartAdapter
import com.aim2u.kotlineatitv2client.Database.CartDataSource
import com.aim2u.kotlineatitv2client.Database.CartDatabase
import com.aim2u.kotlineatitv2client.Database.LocalCartDataSource
import com.aim2u.kotlineatitv2client.EventBus.HideFABCart
import com.aim2u.kotlineatitv2client.EventBus.UpdateItemInCart
import com.aim2u.kotlineatitv2client.R
import com.aim2u.kotlineatitv2client.Common.Common
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_cart.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.StringBuilder

class CartFragment : Fragment() {

    private var cartDataSource:CartDataSource?=null
    private var compositeDisposable:CompositeDisposable = CompositeDisposable()
    private var recyclerViewState:Parcelable?=null
    private var recyclerCart:RecyclerView?= null
    private lateinit var cartViewModel: CartViewModel

    override fun onResume() {
        super.onResume()
        calculateTotalPrice()
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        EventBus.getDefault().postSticky(
            HideFABCart(
                true
            )
        )
        cartViewModel =
            ViewModelProviders.of(this).get(CartViewModel::class.java)

        cartViewModel.initCartDataSource(context!!)
        val root = inflater.inflate(R.layout.fragment_cart, container, false)
        initViews(root)

        cartViewModel.getMutableLiveDataCartItem().observe(this, Observer {
            if (it == null || it.isEmpty()){
                recycler_cart.visibility = View.GONE
                group_place_holder.visibility = View.GONE
                txt_empty_cart.visibility = View.VISIBLE
            } else{
                recycler_cart.visibility = View.VISIBLE
                group_place_holder.visibility = View.VISIBLE
                txt_empty_cart.visibility = View.GONE
            }

            val adapter = MyCartAdapter(context!!,it)
            recycler_cart!!.adapter = adapter

        })
//        val textView: TextView = root.findViewById(R.id.text_tools)
//        cartViewModel.text.observe(this, Observer {
//            textView.text = it
//        })
        return root
    }

    private fun initViews(root: View?) {
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDao())
        recyclerCart = root!!.findViewById(R.id.recycler_cart) as RecyclerView
        recyclerCart!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        recyclerCart!!.layoutManager = layoutManager
        recyclerCart!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        cartViewModel!!.onStop()
        compositeDisposable.clear()
        EventBus.getDefault().postSticky(HideFABCart(false))
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onUpdateItemInCart(event:UpdateItemInCart){
        if (event.cartItem != null){
            recyclerViewState = recycler_cart!!.layoutManager!!.onSaveInstanceState()
            cartDataSource!!.updateCart(event.cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                        calculateTotalPrice();
                        recycler_cart.layoutManager!!.onRestoreInstanceState(recyclerViewState)
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context,"[UPDATE CART]"+e.message,Toast.LENGTH_SHORT).show()
                    }

                })
        }
    }

    private fun calculateTotalPrice() {
        cartDataSource!!.sumPrice(Common.currentUser?.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double>{
                override fun onSuccess(t: Double) {
                    txt_total_price.text = StringBuilder("Total: ")
                        .append(Common.formatPrice(t))
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    Toast.makeText(context,"[SUM CART]"+e.message,Toast.LENGTH_SHORT).show()
                }

            })

        //var totalPrice = Common.foodSelected!!.price.toDouble()
        //        var displayPrice = 0.0
        //
        //        //Addon
        //        if (Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected?.userSelectedAddon!!.size > 0){
        //            for (addonModel in Common.foodSelected?.userSelectedAddon!!){
        //                totalPrice += addonModel.price!!.toDouble()
        //            }
        //        }
        //
        //        //Show
        //        totalPrice += Common.foodSelected!!.userSelectedSize!!.price!!.toDouble()
        //
        //        displayPrice = totalPrice * number_button.number
    }
}