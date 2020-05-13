package com.aim2u.kotlineatitv2client

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.NavController
import com.aim2u.kotlineatitv2client.Adapter.BestDealItemClick
import com.aim2u.kotlineatitv2client.Adapter.PopularFoodItemClick
import com.aim2u.kotlineatitv2client.Database.CartDataSource
import com.aim2u.kotlineatitv2client.Database.CartDatabase
import com.aim2u.kotlineatitv2client.Database.LocalCartDataSource
import com.aim2u.kotlineatitv2client.R
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.aim2u.kotlineatitv2client.Common.Common
import com.aim2u.kotlineatitv2client.EventBus.*
import com.aim2u.kotlineatitv2client.Model.BestDealModel
import com.aim2u.kotlineatitv2client.Model.CategoryModel
import com.aim2u.kotlineatitv2client.Model.FoodModel
import com.aim2u.kotlineatitv2client.Model.PopularCategoryModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.app_bar_home.*
import kotlinx.android.synthetic.main.nav_header_home.*

class HomeActivity : AppCompatActivity() {

    private lateinit var cartDataSource: CartDataSource
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController:NavController
    private var drawerLayout: DrawerLayout?=null
    private var dialog: AlertDialog?=null

    private var menuItemClick =-1

    override fun onResume() {
        super.onResume()
        countCartItem()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(this).cartDao())
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { _ ->
            navController.navigate(R.id.nav_cart)
        }
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_menu,
                R.id.nav_food_detail,
                R.id.nav_cart
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        var headerView = navView.getHeaderView(0)
        var txtUser = headerView.findViewById<TextView>(R.id.txt_user)
        Common.setSpanString("Hey, ",Common.currentUser!!.name,txtUser)

        navView.setNavigationItemSelectedListener(object :NavigationView.OnNavigationItemSelectedListener{
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                item.isChecked = true
                drawerLayout!!.closeDrawers()
                if(item.itemId == R.id.nav_sign_out){
                    Toast.makeText(this@HomeActivity,"OK TEST",Toast.LENGTH_SHORT).show()
                    signOut()
                } else if(item.itemId == R.id.nav_home){
                    if (menuItemClick != item.itemId)
                        navController.navigate(R.id.nav_home)
                } else if(item.itemId == R.id.nav_cart){
                    if (menuItemClick != item.itemId)
                        navController.navigate(R.id.nav_cart)
                } else if(item.itemId == R.id.nav_menu){
                    if (menuItemClick != item.itemId)
                        navController.navigate(R.id.nav_menu)
                } else if(item.itemId == R.id.nav_view_order){
                    if (menuItemClick != item.itemId)
                        navController.navigate(R.id.nav_view_order)
                }

                menuItemClick = item!!.itemId
                return true
            }

        })

        countCartItem()
    }

    private fun signOut() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Sign Out")
            .setMessage("Do you really want to Exit?")
            .setNegativeButton("CANCEL",{dialogInterface, _ -> dialogInterface.dismiss() })
            .setPositiveButton("OK"){_, _ ->
                Common.foodSelected = null
                Common.categorySelected = null
                Common.currentUser = null
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this@HomeActivity,MainActivity::class.java)
                startActivity(intent)
                finish()
            }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategorySelected(event:CategoryClick){
        if(event.isSuccess){
            //Toast.makeText(this, "Click to"+event.category.name, Toast.LENGTH_SHORT).show()
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_list)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onFoodSelected(event: FoodItemClick){
        if(event.isSuccess){
            //Toast.makeText(this, "Click to"+event.category.name, Toast.LENGTH_SHORT).show()
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_detail)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCountCartEvent(event: CountCartEvent){
        if(event.isSuccess){
            countCartItem()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onPopularFoodItemClick(event: PopularFoodItemClick){
        if(event.popularCategoryModel != null){
            dialog?.show()
            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.popularCategoryModel.menu_id!!)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if(p0.exists()){
                            Common.categorySelected = p0.getValue(CategoryModel::class.java)
                            Common.categorySelected!!.menu_id = p0.key

                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.popularCategoryModel.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.popularCategoryModel.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object : ValueEventListener{
                                    override fun onCancelled(p0: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onDataChange(p0: DataSnapshot) {
                                        if (p0.exists()){
                                            for(foodSnapshot in p0.children)
                                            {
                                                Common.foodSelected = foodSnapshot.getValue(FoodModel::class.java)
                                                Common.foodSelected!!.key = foodSnapshot.key
                                            }
                                            navController.navigate(R.id.nav_food_detail)
                                        } else {

                                            Toast.makeText(this@HomeActivity,"Item doesn't exist",Toast.LENGTH_SHORT).show()
                                        }
                                        dialog!!.dismiss()
                                    }

                                })

                            //Load food
                        }else{
                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity,"Item doesn't exist",Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onBestDealFoodItemClick(event: BestDealItemClick){
        if(event.model != null){
            dialog?.show()
            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.model.menu_id!!)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if(p0.exists()){
                            Common.categorySelected = p0.getValue(CategoryModel::class.java)
                            Common.categorySelected!!.menu_id = p0.key

                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.model.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.model.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object : ValueEventListener{
                                    override fun onCancelled(p0: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onDataChange(p0: DataSnapshot) {
                                        if (p0.exists()){
                                            for(foodSnapshot in p0.children)
                                            {
                                                Common.foodSelected = foodSnapshot.getValue(FoodModel::class.java)
                                                Common.foodSelected!!.key = foodSnapshot.key
                                            }
                                            navController.navigate(R.id.nav_food_detail)
                                        } else {

                                            Toast.makeText(this@HomeActivity,"Item doesn't exist",Toast.LENGTH_SHORT).show()
                                        }
                                        dialog!!.dismiss()
                                    }

                                })

                            //Load food
                        }else{
                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity,"Item doesn't exist",Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onHideFABEvent(event: HideFABCart){
        if(event.isHide){
            fab.hide()
        } else {
            fab.show()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onMenuItemBack(event:MenuItemBack){
        menuItemClick = -1
        if (supportFragmentManager.backStackEntryCount > 0){
            supportFragmentManager.popBackStack()
        }
    }

    private fun countCartItem() {
        cartDataSource.countItemInCart(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object :SingleObserver<Int>{
                override fun onSuccess(t: Int) {
                    fab.count = t
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if(!e.message!!.contains("Query returned empty"))
                        sequenceOf(Toast.makeText(this@HomeActivity, "[COUNT CART]" + e.message, Toast.LENGTH_SHORT).show())
                    else
                        fab.count = 0
                }

            })
    }
}
