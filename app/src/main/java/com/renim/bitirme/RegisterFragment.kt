package com.renim.bitirme

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.renim.bitirme.databinding.FragmentRegisterBinding
import org.osmdroid.config.Configuration
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: MapView
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fine || coarse) {
                enableUserLocationOnMap()
            } else {
                Toast.makeText(requireContext(), "Konum izni gerekli.", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                handlePickedDocument(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // osmdroid config
        Configuration.getInstance().load(
            requireContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Map setup
        map = binding.mapView
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        // Compass & scale
        val compass = CompassOverlay(
            requireContext(),
            InternalCompassOrientationProvider(requireContext()),
            map
        )
        compass.enableCompass()
        map.overlays.add(compass)
        map.overlays.add(ScaleBarOverlay(map))

        // Map tap listener
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    addOrMoveMarker(it)
                    binding.tvDocumentName.text =
                        "Konum: ${it.latitude.format(6)}, ${it.longitude.format(6)}"
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        })
        map.overlays.add(mapEventsOverlay)

        // Konum izinleri
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            enableUserLocationOnMap()
        }

        // Butonlar
        binding.btnSelectLocation.setOnClickListener {
            selectedPoint?.let {
                Toast.makeText(
                    requireContext(),
                    "Seçilen konum: ${it.latitude}, ${it.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
            } ?: Toast.makeText(requireContext(), "Haritaya dokunarak konum seçin.", Toast.LENGTH_SHORT)
                .show()
        }

        binding.btnUploadDocument.setOnClickListener {
            pickDocumentLauncher.launch("*/*")
        }

        binding.btnRegisterSubmit.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
            val surname = binding.etSurname.text?.toString()?.trim().orEmpty()
            val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()

            if (name.isEmpty() || surname.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Lütfen tüm alanları doldurun.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (selectedPoint == null) {
                Toast.makeText(
                    requireContext(),
                    "Lütfen haritadan konum seçin.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            Toast.makeText(
                requireContext(),
                "Form doğrulandı",
                Toast.LENGTH_LONG
            ).show()

            // ------------------------------------------------------------
            // ⭐ ADIM 4 — FIREBASE AUTH + FIRESTORE KAYIT KODU EKLENDİ
            // ------------------------------------------------------------

            val password = "123456"

            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->

                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val userMap = hashMapOf(
                        "name" to name,
                        "surname" to surname,
                        "phone" to phone,
                        "email" to email,
                        "latitude" to selectedPoint!!.latitude,
                        "longitude" to selectedPoint!!.longitude,
                        "userId" to userId
                    )

                    db.collection("users")
                        .document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Kayıt başarıyla tamamlandı!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Firestore hata: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Auth hata: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun addOrMoveMarker(p: GeoPoint) {
        if (marker == null) {
            marker = Marker(map)
            marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            try {
                marker!!.icon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_location)
            } catch (_: Exception) {
            }

            map.overlays.add(marker)
        }
        marker!!.position = p
        map.controller.animateTo(p)
        selectedPoint = p
        map.invalidate()
    }

    private fun enableUserLocationOnMap() {
        val default = GeoPoint(39.92077, 32.85411) // Ankara
        map.controller.setCenter(default)
    }

    private fun handlePickedDocument(uri: Uri) {
        val type = requireContext().contentResolver.getType(uri) ?: ""
        if (type.startsWith("image")) {
            binding.ivDocumentPreview.setImageURI(uri)
        } else {
            binding.ivDocumentPreview.setImageResource(R.drawable.ic_document)
        }

        val last = uri.lastPathSegment ?: "dokuman"
        binding.tvDocumentName.text = last
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
