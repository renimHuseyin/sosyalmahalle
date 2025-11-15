package com.renim.bitirme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.renim.bitirme.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Kayıt ekranına yönlendirme
        binding.btnRegister.setOnClickListener {
            kotlin.runCatching {
                findNavController().navigate(R.id.action_login_to_register)
            }.onFailure {
                Snackbar.make(requireView(), "Kayıt ekranına gidilemiyor.", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Admin girişi
        binding.btnAdminLogin.setOnClickListener {
            kotlin.runCatching {
                findNavController().navigate(R.id.action_login_to_admin)
            }.onFailure {
                Snackbar.make(requireView(), "Admin ekranına gidilemiyor.", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Şifremi unuttum tıklaması
        binding.tvForgotPassword.setOnClickListener {
            Snackbar.make(binding.root, "Şifremi unuttum seçildi", Snackbar.LENGTH_SHORT).show()
        }

        // Giriş Yap butonu
        binding.btnLogin.setOnClickListener {
            val identifier = binding.etIdentifier.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (identifier.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.root, "Lütfen tüm alanları doldurun.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (identifier.contains("@")) {
                // Email ile giriş
                signInWithEmail(identifier, password)
            } else {
                // Telefon numarası ile giriş -> Firestore’dan e-mail bul
                db.collection("users")
                    .whereEqualTo("phone", identifier)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            val userEmail = snapshot.documents[0].getString("email")
                            if (userEmail != null) {
                                signInWithEmail(userEmail, password)
                            } else {
                                Snackbar.make(binding.root, "Telefon numarası kayıtlı değil.", Snackbar.LENGTH_SHORT).show()
                            }
                        } else {
                            Snackbar.make(binding.root, "Kullanıcı bulunamadı.", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Snackbar.make(binding.root, "Giriş hatası: ${it.message}", Snackbar.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Giriş başarılı, HomeFragment'e git
                    findNavController().navigate(R.id.action_login_to_home)
                } else {
                    Snackbar.make(binding.root, "Kullanıcı adı/şifre yanlış.", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
