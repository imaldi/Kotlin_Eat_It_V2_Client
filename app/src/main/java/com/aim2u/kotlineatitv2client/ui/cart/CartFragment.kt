package com.aim2u.kotlineatitv2client.ui.cart

import android.app.AlertDialog
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.view.*
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2client.Adapter.MyCartAdapter
import com.aim2u.kotlineatitv2client.Callback.IMyButtonCallback
import com.aim2u.kotlineatitv2client.Database.CartDataSource
import com.aim2u.kotlineatitv2client.Database.CartDatabase
import com.aim2u.kotlineatitv2client.Database.LocalCartDataSource
import com.aim2u.kotlineatitv2client.EventBus.HideFABCart
import com.aim2u.kotlineatitv2client.EventBus.UpdateItemInCart
import com.aim2u.kotlineatitv2client.R
import com.aim2u.kotlineatitv2client.Common.Common
import com.aim2u.kotlineatitv2client.Common.MySwipeHelper
import com.aim2u.kotlineatitv2client.EventBus.CountCartEvent
import com.google.android.gms.location.*
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_cart.*
import kotlinx.android.synthetic.main.layout_place_order.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.StringBuilder

class CartFragment : Fragment() {

    private var cartDataSource:CartDataSource?=null
    private var compositeDisposable:CompositeDisposable = CompositeDisposable()
    private var recyclerViewState:Parcelable?=null
    private var recyclerCart:RecyclerView?= null
    private var adapter:MyCartAdapter?=null
    private lateinit var cartViewModel: CartViewModel

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    override fun onResume() {
        super.onResume()
        calculateTotalPrice()
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,
                Looper.getMainLooper())
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
        initLocation()

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

            adapter = MyCartAdapter(context!!,it)
            recycler_cart!!.adapter = adapter

        })
//        val textView: TextView = root.findViewById(R.id.text_tools)
//        cartViewModel.text.observe(this, Observer {
//            textView.text = it
//        })
        return root
    }

    private fun initLocation() {
        buildLocationRequest()
        buildLocationCallback()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper())
    }

    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                currentLocation = p0!!.lastLocation
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(5000)
        locationRequest.setFastestInterval(2000)
        locationRequest.setSmallestDisplacement(10f)
    }

    private fun initViews(root: View?) {

        setHasOptionsMenu(true)//Important to inflate menu

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
        cartViewModel.onStop()
        compositeDisposable.clear()
        EventBus.getDefault().postSticky(HideFABCart(false))
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()

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
                    txt_total_price.text = StringBuilder("Total: $")
                        .append(Common.formatPrice(t))
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if(!e.message!!.contains("Query returned empty"))
                        Toast.makeText(context,"[SUM CART]"+e.message,Toast.LENGTH_SHORT).show()
                }

            })

        val swipe = object : MySwipeHelper(context!!,recyclerCart!!,200){
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(MyButton(context!!,
                "Delete",
                30,
                0,
                Color.parseColor("#FF3c30"),
                object : IMyButtonCallback{
                    override fun onClick(pos: Int) {
                        Toast.makeText(context,"Delete Item",Toast.LENGTH_SHORT).show()

                        val deleteItem = adapter!!.getItemAtPosition(pos)
                        cartDataSource!!.deleteCart(deleteItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(object : SingleObserver<Int>{
                                override fun onSuccess(t: Int) {
                                    adapter!!.notifyItemRemoved(pos)
                                    sumCart()
                                    EventBus.getDefault().postSticky(CountCartEvent(true))
                                    Toast.makeText(context,"Delete item Success",Toast.LENGTH_SHORT).show()
                                }

                                override fun onSubscribe(d: Disposable) {

                                }

                                override fun onError(e: Throwable) {
                                    Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                                }

                            })

                    }
                }))
            }
        }

        btn_place_order.setOnClickListener{
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("One more step!")

            val view = LayoutInflater.from(context).inflate(R.layout.layout_place_order,null)

            val edtAddress = view.findViewById<View>(R.id.edt_address) as EditText
            val edtComment = view.findViewById<View>(R.id.edt_comment) as EditText
            val txtAddress = view.findViewById<View>(R.id.txt_address_detail) as TextView
            val rdiHome = view.findViewById<View>(R.id.rdi_home_address) as RadioButton
            val rdiOtherAddress = view.findViewById<View>(R.id.rdi_other_address) as RadioButton
            val rdiShipThis = view.findViewById<View>(R.id.rdi_ship_this_address) as RadioButton
            val rdiCOD = view.findViewById<View>(R.id.rdi_cod) as RadioButton
            val rdiBraintree = view.findViewById<View>(R.id.rdi_braintree) as RadioButton
            //Data
            edtAddress.setText(Common.currentUser!!.address!!)

            //Event
            rdiHome.setOnCheckedChangeListener { _, b ->
                if(b){
                    edtAddress.setText(Common.currentUser!!.address!!)
                }
            }
            rdiOtherAddress.setOnCheckedChangeListener { _, b ->
                if(b){
                    edtAddress.setText("")
                    edtAddress.setHint("Enter your address")
                }
            }
            rdiShipThis.setOnCheckedChangeListener { _, b ->
                if(b){
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener{e -> Toast.makeText(context!!, ""+e.message,Toast.LENGTH_SHORT).show()}
                        .addOnCompleteListener{task ->
                            val coordinates = StringBuilder()
                                .append(task.result!!.latitude)
                                .append("/")
                                .append(task.result!!.longitude)
                                .toString()

                            edtAddress.setText(coordinates)
                        }
                }
            }

            builder.setView(view)
            builder.setNegativeButton("NO",{dialogInterface, _ -> dialogInterface.dismiss() })
                .setPositiveButton("YES",{_, _ -> Toast.makeText(context!!,"Implement Late",Toast.LENGTH_SHORT).show()  })

            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun sumCart() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double>{
                override fun onSuccess(t: Double) {
                    txt_total_price.text = StringBuilder("Total")
                        .append(t)
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if(!e.message!!.contains("Query returned empty"))
                        Toast.makeText(context,""+e.message!!,Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_settings).setVisible(false) // hide setting menu while in cart
        super.onPrepareOptionsMenu(menu)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.cart_menu,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_clear_cart) {
            cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                        Toast.makeText(context,"Clear Cart Success",Toast.LENGTH_SHORT).show()
                        EventBus.getDefault().postSticky(CountCartEvent(true))
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                    }

                })
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}