package com.renim.bitirme

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.renim.bitirme.databinding.FragmentRegisterBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import android.util.Base64

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: MapView
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null

    private var selectedDocumentUri: Uri? = null

    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private var requestingLocationUpdates = false
    private lateinit var locationCallback: com.google.android.gms.location.LocationCallback

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fine || coarse) enableUserLocationOnMap()
            else Toast.makeText(requireContext(), "Konum izni gerekli.", Toast.LENGTH_SHORT).show()
        }

    private val pickDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handlePickedDocument(it) }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(
            requireContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient =
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireContext())

        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                val loc = locationResult.lastLocation
                loc?.let {
                    moveToLocationAndMark(GeoPoint(it.latitude, it.longitude))
                    stopLocationUpdates()
                }
            }
        }

        map = binding.mapView
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        val compass = CompassOverlay(requireContext(), InternalCompassOrientationProvider(requireContext()), map)
        compass.enableCompass()
        map.overlays.add(compass)
        map.overlays.add(ScaleBarOverlay(map))

        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    addOrMoveMarker(it)
                    binding.tvDocumentName.text = "Konum: ${it.latitude.format(6)}, ${it.longitude.format(6)}"
                }
                return true
            }
            override fun longPressHelper(p: GeoPoint?) = false
        })
        map.overlays.add(mapEventsOverlay)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else enableUserLocationOnMap()

        // Şehir spinner adapter
        val spinner: Spinner = binding.spinnerCity
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.cities, // strings.xml içindeki array
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        binding.btnSelectLocation.setOnClickListener {
            selectedPoint?.let {
                Toast.makeText(requireContext(), "Seçilen konum: ${it.latitude}, ${it.longitude}", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(requireContext(), "Haritaya dokunarak konum seçin.", Toast.LENGTH_SHORT).show()
        }

        binding.btnUploadDocument.setOnClickListener { pickDocumentLauncher.launch("*/*") }

        binding.btnRegisterSubmit.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
            val surname = binding.etSurname.text?.toString()?.trim().orEmpty()
            val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()
            val city = binding.spinnerCity.selectedItem?.toString().orEmpty()

            if (name.isEmpty() || surname.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || city.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedPoint == null) {
                Toast.makeText(requireContext(), "Lütfen haritadan konum seçin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedDocumentUri == null) {
                Toast.makeText(requireContext(), "Lütfen bir dosya yükleyin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val auth = FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    try {
                        val inputStream = requireContext().contentResolver.openInputStream(selectedDocumentUri!!)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (bytes == null) {
                            Toast.makeText(requireContext(), "Dosya okunamadı.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

                        val db = FirebaseFirestore.getInstance()
                        val userMap = hashMapOf(
                            "name" to name,
                            "surname" to surname,
                            "phone" to phone,
                            "email" to email,
                            "password" to password,
                            "city" to city,
                            "latitude" to selectedPoint!!.latitude,
                            "longitude" to selectedPoint!!.longitude,
                            "userId" to userId,
                            "documentBase64" to base64String,
                            "documentName" to (selectedDocumentUri?.lastPathSegment ?: "dokuman")
                        )

                        db.collection("users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Kayıt tamamlandı ve Firestore'a yazıldı!", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Firestore hata: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Dosya okuma hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Auth hata: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun addOrMoveMarker(p: GeoPoint) {
        if (marker == null) {
            marker = Marker(map).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                try { icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location) } catch (_: Exception) {}
            }
            map.overlays.add(marker)
        }
        marker!!.position = p
        map.controller.animateTo(p)
        selectedPoint = p
        map.invalidate()
    }

    private fun enableUserLocationOnMap() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { moveToLocationAndMark(GeoPoint(it.latitude, it.longitude)) } ?: startLocationUpdates()
            }.addOnFailureListener { map.controller.setCenter(GeoPoint(39.92077, 32.85411)) }
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    private fun moveToLocationAndMark(point: GeoPoint) {
        map.controller.animateTo(point)
        map.controller.setZoom(18.0)
        addOrMoveMarker(point)
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
            interval = 3000L
            fastestInterval = 1000L
            numUpdates = 1
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        requestingLocationUpdates = true
    }

    private fun stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            requestingLocationUpdates = false
        }
    }

    private fun handlePickedDocument(uri: Uri) {
        selectedDocumentUri = uri
        val type = requireContext().contentResolver.getType(uri) ?: ""
        if (type.startsWith("image")) binding.ivDocumentPreview.setImageURI(uri)
        else binding.ivDocumentPreview.setImageResource(R.drawable.ic_document)
        binding.tvDocumentName.text = uri.lastPathSegment ?: "dokuman"
    }

    override fun onResume() { super.onResume(); map.onResume(); if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) if (selectedPoint == null) enableUserLocationOnMap() }

    override fun onPause() { super.onPause(); map.onPause(); stopLocationUpdates() }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
