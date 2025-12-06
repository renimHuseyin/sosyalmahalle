package com.renim.bitirme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseException
import com.renim.bitirme.databinding.FragmentAdminBinding
import java.util.concurrent.TimeUnit

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var verificationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnSendSms.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.isEmpty()) {
                Toast.makeText(requireContext(), "Telefon numarasını girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val formattedPhone = if (phone.startsWith("+")) phone else "+90$phone"
            sendVerificationCode(formattedPhone)
        }

        binding.btnAdminLogin.setOnClickListener {
            val mahalle = binding.etMahalle.text.toString().trim()
            val password = binding.etAdminPassword.text.toString().trim()
            val smsCode = binding.etSmsCode.text.toString().trim()

            if (mahalle.isEmpty() || password.isEmpty() || smsCode.isEmpty()) {
                Toast.makeText(requireContext(), "Tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (verificationId == null) {
                Toast.makeText(requireContext(), "Önce SMS kodu alın", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = PhoneAuthProvider.getCredential(verificationId!!, smsCode)
            signInWithPhoneAuthCredential(credential, mahalle, password)
        }

        return binding.root
    }

    private fun sendVerificationCode(formattedPhone: String) {

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Toast.makeText(requireContext(), "Otomatik doğrulandı", Toast.LENGTH_SHORT).show()
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(requireContext(), "Doğrulama hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(
                    vid: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = vid
                    Toast.makeText(requireContext(), "Kod gönderildi", Toast.LENGTH_SHORT).show()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential,
        mahalle: String,
        password: String
    ) {

        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                db.collection("admins")
                    .whereEqualTo("mahalle", mahalle)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnSuccessListener { documents ->

                        if (!documents.isEmpty) {
                            Toast.makeText(requireContext(), "Admin girişi başarılı", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Mahalle veya şifre hatalı", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Firestore hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

            } else {
                Toast.makeText(requireContext(), "SMS doğrulama hatalı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
