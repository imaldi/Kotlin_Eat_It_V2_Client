package com.aim2u.kotlineatitv2client.ui.cart

import android.app.AlertDialog
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.util.Log
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
import com.aim2u.kotlineatitv2client.Callback.ILoadTimeFromFirebaseCallback
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
import com.aim2u.kotlineatitv2client.Model.Order
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_cart.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class CartFragment : Fragment(), ILoadTimeFromFirebaseCallback {

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


    lateinit var listener:ILoadTimeFromFirebaseCallback
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


        listener = this
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
                        .addOnFailureListener{e ->
                            txtAddress.visibility = View.GONE
                            Toast.makeText(context!!, ""+e.message,Toast.LENGTH_SHORT).show()}
                        .addOnCompleteListener{task ->
                            val coordinates = StringBuilder()
                                .append(task.result!!.latitude)
                                .append("/")
                                .append(task.result!!.longitude)
                                .toString()

                            val singleAddress = Single.just(getAddressFromLatLng(task.result!!.latitude,task.result!!.longitude))

                            val disposable = singleAddress.subscribeWith(object : DisposableSingleObserver<String>(){
                                override fun onSuccess(t: String) {
                                    edtAddress.setText(coordinates)
                                    txtAddress.visibility = View.VISIBLE
                                    txtAddress.setText(t)
                                }

                                override fun onError(e: Throwable) {
                                    edtAddress.setText(coordinates)
                                    txtAddress.visibility = View.VISIBLE
                                    txtAddress.setText("Implement Late with google API")
                                }
                            })

                        }
                }
            }

            builder.setView(view)
            builder.setNegativeButton("NO",{dialogInterface, _ -> dialogInterface.dismiss() })
                .setPositiveButton("YES",{_, _ ->
                    if(rdiCOD.isChecked)
                        paymentCOD(edtAddress.text.toString(),edtComment.text.toString())
                })

            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun paymentCOD(address: String, comment: String) {
        compositeDisposable.add(cartDataSource!!.getAllCart(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({cartItemList ->
                //When we have all cartItems, we will get total price
                cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<Double>{
                        override fun onSuccess(totalPrice: Double) {
                            val finalPrice = totalPrice
                            val order = Order()
                            order.userId = Common.currentUser!!.uid
                            order.userName = Common.currentUser!!.name
                            order.userPhone = Common.currentUser!!.phone
                            order.shippingAddress = address
                            order.comment = comment

//                            if (currentLocation != null)
//                            {
                                order.lat = currentLocation.latitude
                                order.lng = currentLocation.longitude
//                            }

                            order.cartItemList = cartItemList
                            order.totalPayment = totalPrice
                            order.finalPayment = finalPrice
                            order.discount = 0
                            order.isCod = true
                            order.transactionId = "Cash On Delivery"

                            //Submit to firebase
//                            writeOrderToFirebase(order)
                            syncLocalTimeWithServerTime(order)
                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(context!!, ""+e.message,Toast.LENGTH_SHORT).show()
                        }
                    })
            },{throwable ->
                Toast.makeText(context!!, ""+throwable.message,Toast.LENGTH_SHORT).show()
            }))
    }

    private fun syncLocalTimeWithServerTime(order: Order) {
        var offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object :  ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                listener.onLoadTimeFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                val offset = p0.getValue(Long::class.java)
                val estimatedServerTimeInMs = System.currentTimeMillis()+offset!! //Add missing offset to your current time
                val sdf = SimpleDateFormat("MMM dd yyyy, HH:mm")
                val date = Date(estimatedServerTimeInMs)
                Log.d("EDMT_DEV",""+sdf.format(date))
                listener.onLoadTimeSuccess(order,estimatedServerTimeInMs)
            }

        })
    }

    private fun writeOrderToFirebase(order: Order) {
        FirebaseDatabase.getInstance()
            .getReference(Common.ORDER_REF)
            .child(Common.createOrderNumber())
            .setValue(order)
            .addOnFailureListener {e ->
                Toast.makeText(context!!,""+e.message, Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { task ->
                cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object :SingleObserver<Int>{
                        override fun onSuccess(t: Int) {
                            Toast.makeText(context!!, "Order placed successfuly",Toast.LENGTH_SHORT).show()
                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(context!!, ""+e.message,Toast.LENGTH_SHORT).show()
                        }
                    })
            }
    }

    private fun getAddressFromLatLng(latitude: Double, longitude: Double): String{
        val geocoder = Geocoder(context!!, Locale.getDefault())
        var result:String? = null

        try {
            val addressList = geocoder.getFromLocation(latitude,longitude,1)
            if(addressList != null && addressList.size >0){
                val address = addressList[0]
                val sb = StringBuilder(address.getAddressLine(0))
                result = sb.toString()
            } else{
                result = "Address not found!"
            }
            return  result
        } catch (e:IOException){
            return e.message!!
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

    override fun onLoadTimeSuccess(order: Order, estimatedTimeMs: Long) {
        order.createDate = estimatedTimeMs
        writeOrderToFirebase(order)
    }

    override fun onLoadTimeFailed(message: String) {
        Toast.makeText(context!!,message,Toast.LENGTH_SHORT).show()
    }
}