package com.omeratilgan.kotlininstagram.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.omeratilgan.kotlininstagram.R
import com.omeratilgan.kotlininstagram.adapter.FeedRecyclerAdapter
import com.omeratilgan.kotlininstagram.databinding.ActivityFeedBinding
import com.omeratilgan.kotlininstagram.model.Post

class FeedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedBinding
    private lateinit var auth: FirebaseAuth //signOutu kullanmak için
    private lateinit var db: FirebaseFirestore //koyduğumuz verileri çekmek için bunu kullanıyoruz
    private lateinit var  postArrayList : ArrayList<Post>
    private lateinit var feedAdapter : FeedRecyclerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth=Firebase.auth
        db=Firebase.firestore
        postArrayList=ArrayList<Post>()
        getData()

        binding.recyclerView.layoutManager=LinearLayoutManager(this)
        feedAdapter= FeedRecyclerAdapter(postArrayList)
        binding.recyclerView.adapter=feedAdapter
    }
    private fun getData(){//verileri almak için
        db.collection("Posts").orderBy("date",Query.Direction.DESCENDING).addSnapshotListener { value, error ->  //Ya hata veriyor ya da değerleri veriyor
            if (error != null){
                Toast.makeText(this,error.localizedMessage,Toast.LENGTH_LONG).show()
            }else{
                if (value!=null){
                    if (!value.isEmpty){  //değerler boş değilse kontrolü

                        val documents =value.documents //documentsleri liste haline getiriyoruz

                        postArrayList.clear()

                        for (document in documents){
                            val comment=document.get("comment") as String //normalde any! geliyordu comment biz her türlü stringe çevirdik
                            val userEmail=document.get("userEmail") as String
                            val downloadUrl=document.get("downloadUrl") as String
                            //yukarıda aldığımız verileri dizi haline getirmeliyiz .RecyclerView için

                            val post= Post(userEmail,comment,downloadUrl)
                            postArrayList.add(post)
                        }
                        feedAdapter.notifyDataSetChanged() // veriler kendini düzenledi kendine çeki düzen ver

                    }
                }
            }

        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater=menuInflater
        menuInflater.inflate(R.menu.insta_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_post){
            val intent=Intent(this, UploadActivity::class.java)
            startActivity(intent)

        }else if (item.itemId == R.id.signout){
          auth.signOut()
            val intent=Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}