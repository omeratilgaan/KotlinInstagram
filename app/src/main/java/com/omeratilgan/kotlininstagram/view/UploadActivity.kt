package com.omeratilgan.kotlininstagram.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.omeratilgan.kotlininstagram.databinding.ActivityUploadBinding
import java.util.*

class UploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>//Galeriye gitmek için
    private lateinit var permissionLauncher: ActivityResultLauncher<String>//izin koontrolü
    var selectedPicture : Uri? = null //farklı yerlerdende erişmek isteyecez
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore : FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        registerLauncher()

        auth= Firebase.auth
        firestore=Firebase.firestore
        storage=Firebase.storage


        binding.imageView.setOnClickListener {
            selectImage()
        }
        binding.button3.setOnClickListener {
            upload()
        }
    }
    fun upload(){
        //ilk aşama görselleri storageye kaydediyoruz daha sonra firestorageyi kullanıp yorumu tarihi alıyoruz
        //1.aşama storageye kaydetme
        val uudi=UUID.randomUUID() //resimleri üst üste birbirinin yerine kaydetmesini engelliyor
        val imageName="$uudi.jpg"
        val reference=storage.reference //storageye kaydediyoruz
        val imageReference= reference.child("images").child(imageName)//images klasörü aç içine imageName dosyası koy

        if (selectedPicture!=null){ //seçeceğim dosyayı kontrol et İlk bunu yazdık

            imageReference.putFile(selectedPicture!!).addOnSuccessListener {
              //upload edilip edilmediğini kontrol edecez

             //2.aşama downolad url-> firestore//upload ettiğimiz görselin referansıyla download url yi al veritabanına yaz
                val uploadPictureReference=storage.reference.child("images").child(imageName)
                uploadPictureReference.downloadUrl.addOnSuccessListener {
                    val downloadUrl=it.toString() //download urlyi aldık veri tabanınıa yazacaz

                    //fireStorenin getStardı(FİRESTOREYA KOYUYORUZ)
                    val postMap = hashMapOf<String,Any>()
                    postMap.put("downloadUrl",downloadUrl)
                    postMap.put("userEmail",auth.currentUser!!.email!!)
                    postMap.put("comment",binding.commentText.text.toString())
                    postMap.put("date",Timestamp.now())

                    firestore.collection("Posts").add(postMap).addOnSuccessListener {
                               finish()
                        //Veri tabanına verileri kaydettik


                    }.addOnFailureListener {
                        Toast.makeText(this@UploadActivity,it.localizedMessage,Toast.LENGTH_LONG).show()
                    }

                }

            }.addOnFailureListener{
                Toast.makeText(this,it.localizedMessage,Toast.LENGTH_LONG).show()
            }
        }

    }
    fun selectImage(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            //ANDROİD 33+ READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_IMAGES)!=PackageManager.PERMISSION_GRANTED){
                //izin verilmedi
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_MEDIA_IMAGES)){
                    //RATİONALE
                    //iznin mantığını göstereyim mi (mantığı gösterdikten sonra isteyeecez)
                    Snackbar.make(binding.imageView.rootView,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                        //request permission(İZİN İSTEME)
                        permissionLauncher.launch((Manifest.permission.READ_MEDIA_IMAGES))
                    }).show()

                }else{
                    //request permision(izin isteme)(mantığı göstermeden isteyecez)
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }else{
                //izin verildi -galeriye gidilecek
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }
        }else{
            //ANDROİD 32 -> READ_EXTANAL_STORAGE
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                //izin verilmedi
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //RATİONALE
                    //iznin mantığını göstereyim mi (mantığı gösterdikten sonra isteyeecez)
                    Snackbar.make(binding.imageView.rootView,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                        //request permission
                        permissionLauncher.launch((Manifest.permission.READ_EXTERNAL_STORAGE))
                    }).show()

                }else{
                    //request permision(izin isteme)(mantığı göstermeden isteyecez)
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else{
                //izin verildi -galeriye gidilecek
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }


        }

    }
    private fun registerLauncher(){ // bu olmadadan permission launcher çalışmaz
        println("burada")
        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){result->
            if (result){
                //permission granted (izin verildi)
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }else{
                //permission denied
                Toast.makeText(this@UploadActivity,"Permission needed",Toast.LENGTH_LONG).show()
            }

        }
        activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
          if (result.resultCode== RESULT_OK){
              val intentFromResult=result.data
              if (intentFromResult!=null){
                selectedPicture =  intentFromResult.data
                  selectedPicture?.let {
                      binding.imageView.setImageURI(it)
                  }

              }
          }


        }
    }
}