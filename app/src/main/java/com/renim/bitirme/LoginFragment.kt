package com.renim.bitirme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.renim.bitirme.databinding.FragmentLoginBinding

/**
 * Kullanıcı girişi için bir ekran sunan [Fragment].
 * Bu fragment, kullanıcının kimlik bilgilerini girmesine olanak tanır ve
 * kayıt olma veya yönetici olarak giriş yapma gibi gezinme eylemlerini yönetir.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    // Bu özellik yalnızca onCreateView ve onDestroyView arasında geçerlidir.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    }

    /**
     * Fragment'in görünümü yok edildiğinde çağrılır.
     * Bellek sızıntılarını önlemek için binding referansını temizler.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
