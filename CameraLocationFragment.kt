class CameraLocationFragment : BaseFragment(), LocationProvider.LocationUpdateListener {

    private lateinit var binding: FragmentCameraLocationBinding
    private lateinit var commonToolbarBinding: LayoutCommonToolbarBinding

 //   private lateinit var cameraLocationVM: CameraLocationViewModel
    private val cameraLocationVM: CameraLocationViewModel by viewModels()

    private lateinit var cameraList: ArrayList<CameraBasicInfo>
    private var selectedCameraObj: CameraBasicInfo? = null
    private var screenTitle: String = ""
    private var mapViewType: Int = GoogleMap.MAP_TYPE_HYBRID
    private var isMapViewTypeChanged = false

    private lateinit var clusterManager: ClusterManager<LocationItem>

    private var locationProvider: LocationProvider? = null
    private val gpsReceiver = GpsLocationReceiver()
    private var newLocation: Location? = null
    private var nMap: GoogleMap? = null

    companion object {

        @JvmStatic
        fun newInstance(bundle: Bundle? = null) = CameraLocationFragment()
            .apply {
                arguments = bundle
            }

        const val BUNDLE_EXTRA_CAMERA_LIST = "camera_list"
        const val BUNDLE_EXTRA_TITLE = "title"
        var TAG = CameraLocationFragment::class.java.simpleName.toString()
    }

    override fun initToolbar() {
        super.initToolbar()
        commonToolbarBinding = binding.toolbar
        commonToolbarBinding.ivBack.visibility = View.VISIBLE
        commonToolbarBinding.tvToolbarTitle.text = screenTitle
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCameraLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isAdded && activity != null) setMapView()

        cameraList =
            arguments?.getSerializable(BUNDLE_EXTRA_CAMERA_LIST) as ArrayList<CameraBasicInfo>
        screenTitle = arguments?.getString(BUNDLE_EXTRA_TITLE).toString()
        initToolbar()
        if (cameraList.size <= 1) setCameraLocationsObserver()
    }

    private fun setMapView() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun init() {
      //  initViewModel()
        setClickListeners()

        locationProvider = LocationProvider(requireContext(), this)

        setFragmentResultListener(CameraConstants.RESULT_CAMERA_EDIT_LOCATION) { key, bundle ->

            if (bundle.getString("data") == CameraConstants.CAMERA_EDIT_LOCATION_CONFIRM) {
                cameraLocationVM.isLocationEdited = true
                removeEditLocationFragment()
                cameraLocationVM.editedCameraId = bundle.getInt("cameraId", -1)
                AppLog.errorLog("" + cameraLocationVM.editedCameraId)
                if (cameraList.size >= 1) setMapView()
            }
        }
    }

    override fun setObservers() {
        setCameraSelectedObserver()
        setProgressObserver()
    }

   /* private fun initViewModel() {
        cameraLocationVM = ViewModelProvider(requireActivity()).get(
            CameraLocationViewModel::class.java
        )
    }*/

    private fun setClickListeners() {
        binding.tvStreet.setOnClickListener {
            binding.tvStreet.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.app_orange_95_trans
                )
            )
            binding.tvHybrid.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_color_light_black_dark_white
                )
            )
            binding.tvSatellite.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_color_light_black_dark_white
                )
            )
            mapViewType = GoogleMap.MAP_TYPE_TERRAIN
            isMapViewTypeChanged = true
            setMapView()
        }
        binding.tvHybrid.setOnClickListener {
            binding.tvStreet.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_color_light_black_dark_white
                )
            )
            binding.tvHybrid.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.app_orange_95_trans
                )
            )
            binding.tvSatellite.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_color_light_black_dark_white
                )
            )
            mapViewType = GoogleMap.MAP_TYPE_HYBRID
            isMapViewTypeChanged = true
            setMapView()
        }
        binding.tvSatellite.setOnClickListener {
            binding.tvStreet.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_color_light_black_dark_white
                )
            )
            binding.tvHybrid.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_color_light_black_dark_white
                )
            )
            binding.tvSatellite.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.app_orange_95_trans
                )
            )
            mapViewType = GoogleMap.MAP_TYPE_SATELLITE
            isMapViewTypeChanged = true
            setMapView()
        }

        binding.ivMyLocation.setOnClickListener {
            cameraLocationVM.isMyLocationClicked = true
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (checkLocationDisabled())
                    checkLocationPermission()
                else {
                    initWebServiceCallAfterChecks()
                }
            } else {
                checkLocationPermission()
            }
        }
        binding.tvEdit.setOnClickListener {
            showCameraLocationEditScreen()
        }

        binding.lyLocationTitle.setOnClickListener{

        }
    }

    private val callback = OnMapReadyCallback { googleMap ->

        googleMap.mapType = mapViewType
        googleMap.uiSettings.isMapToolbarEnabled =false
        nMap = googleMap

        if(!isMapViewTypeChanged) {

            if (cameraList.size > 0) {
                if (cameraList.size > 1) {
                    binding.cvSwitchMapType.visibility = View.VISIBLE
                    setUpCluster(googleMap, cameraList, false)

                } else {
                    val orangeMarkerBMapDescriptor =
                        BitmapDescriptorFactory.fromResource((R.drawable.ic_map_marker_orange))
                    val camera = cameraList[0]
                    val cameraLocation = LatLng(camera.latitude!!, camera.longitude!!)
                    val cameraName = camera.cameraName
                    googleMap.addMarker(
                        MarkerOptions().position(cameraLocation).title(cameraName)
                            .icon(orangeMarkerBMapDescriptor)
                    )?.tag = camera.cameraID
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            cameraLocation, MAP_ZOOM_LEVEL
                        )
                    )
                    selectedCameraObj = camera
                    getCameraSelected(camera)
                }
            }
        }else {
            isMapViewTypeChanged = false
        }
    }

    private val anchorClick = GoogleMap.OnMarkerClickListener { marker ->
        val selectedAnchorTag = marker.tag
        val selectedCamera = cameraList.filter { it.cameraID == selectedAnchorTag }[0]
        selectedCameraObj = selectedCamera
        getCameraSelected(selectedCamera)

        false
    }

    private fun getCameraSelected(selectedCamera: CameraBasicInfo) {
        if (isAdded && activity!=null && NetworkUtil.isNetworkConnected(requireActivity())) {
            cameraLocationVM.getCameraSelected(context, selectedCamera,0)
        } else {
            cameraLocationVM.setMessage(
                false,
                WebConstant.WARNING_NO_NETWORK,
                showAsInLayoutMessage = true
            )
        }
    }

    private fun setCameraSelectedObserver() {
        cameraLocationVM.cameraAddressLiveData.observe(viewLifecycleOwner, EventObserver {
            if (it.isNotEmpty()) {
                setCameraNameAndAddress(selectedCameraObj?.cameraName, it)
            }
        })
    }

    private fun setCameraNameAndAddress(cameraName: String?, addressText: String) {
        binding.tvNameTitle.text = cameraName
        binding.tvLocationName.text = addressText
        binding.lyLocationTitle.visibility = View.VISIBLE
        binding.cvSwitchMapType.visibility = View.VISIBLE
        if(selectedCameraObj?.active!=null&&selectedCameraObj?.active!!)
            binding.tvEdit.visibility = View.VISIBLE
        else
            binding.tvEdit.visibility = View.GONE
    }

    private fun setProgressObserver() {

        cameraLocationVM.progressStatus.observe(viewLifecycleOwner, EventObserver {

            if (cameraLocationVM.isBlockingOperationInProgress) {

                if (it) {
                    DialogUtil.showProgressDialog(
                        requireActivity(),
                        getString(R.string.fetching_camera_address)
                    )
                } else {
                    DialogUtil.dismissProgressDialog()
                }
            }
        })
    }

    private fun setCameraLocationsObserver() {

        cameraLocationVM.getAllCameraLocationInfoFromDatabase()
            ?.observe(viewLifecycleOwner, Observer { list ->

                if (!list.isNullOrEmpty()) {

                    if (list.size > 1) {
                        if (nMap != null) {
                            setUpCluster(nMap!!, list as ArrayList<CameraBasicInfo>, true)
                        }
                    }
                }
            })
    }

    private fun setUpCluster(
        map: GoogleMap,
        list: ArrayList<CameraBasicInfo>,
        isFromObserver: Boolean
    ) {
        map.clear()
        if (!isFromObserver) {

            for(i in 0 until cameraList.size){
                if(cameraList[i].latitude!! != 0.0  && cameraList[i].longitude!! != 0.0 ){
                    selectedCameraObj = cameraList[i]
                    break
                }
            }

            if (cameraLocationVM.isLocationEdited && cameraLocationVM.editedCameraId >= 0) {

                val lastItemIndex =
                    cameraList.indexOfLast { it.cameraID == cameraLocationVM.editedCameraId }
                selectedCameraObj = cameraList[lastItemIndex]
                getCameraSelected(selectedCameraObj!!)
                cameraLocationVM.isLocationEdited = false
            }
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        selectedCameraObj?.latitude!!,
                        selectedCameraObj?.longitude!!
                    ), LOCATIONS_ZOOM_LEVEL
                )
            )
        }

        clusterManager = ClusterManager(requireActivity(), map)
        clusterManager.renderer = MarkerClusterRenderer(
            requireActivity(),
            map,
            clusterManager, isFromObserver
        )

        map.setOnCameraIdleListener(clusterManager)
        clusterManager.setOnClusterItemClickListener {
            var selectedCamera = CameraBasicInfo()
            for (i in 0 until list.size) {
                if (it.snippet.equals("Camera $i")) {
                    selectedCamera = list[i]
                    break
                }
            }
            if (!isFromObserver) {
                selectedCameraObj = selectedCamera
                getCameraSelected(selectedCamera)
            }

            false
        }

        clusterManager.setOnClusterClickListener {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    it.position,
                    (floor((map.cameraPosition.zoom + 1).toDouble()).toFloat())
                ), 300, null
            )
            true
        }

        addLocations(isFromObserver, list)
    }

    private fun addLocations(isFromObserver: Boolean, list: ArrayList<CameraBasicInfo>) {
        for (i in 0 until list.size) {
            if (isFromObserver && selectedCameraObj?.cameraID != list[i].cameraID) {
                val offset = i / 800.0
                val lat = list[i].latitude!! + offset
                val lon = list[i].longitude!! + offset
                val offsetItem =
                    LocationItem(
                        lat,
                        lon,
                        list[i].cameraName!!,
                        "Camera $i"
                    )
                clusterManager.addItem(offsetItem)
            } else if (!isFromObserver) {
                val offset = i / 800.0
                val lat = list[i].latitude!! + offset
                val lon = list[i].longitude!! + offset
                val offsetItem =
                    LocationItem(
                        lat,
                        lon,
                        list[i].cameraName!!,
                        "Camera $i"
                    )
                clusterManager.addItem(offsetItem)
            }
        }
        if (isFromObserver) {
            Handler(Looper.getMainLooper()).postDelayed({

                if (isAdded) {
                    mapViewType = GoogleMap.MAP_TYPE_HYBRID
                    setMapView()
                }

            }, 2000)
        }
    }

    private fun checkLocationDisabled(): Boolean {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun handleNewLocation(location: Location?) {
        if (location != null) {
            newLocation = location
            if (cameraLocationVM.isMyLocationClicked) {
                cameraLocationVM.isMyLocationClicked = false
                setMyLocationOnFocus()
            }
        } else {
            locationProvider?.getSingleLocationUpdate(checkIfLastLocationAvailable = true)
        }
    }

    private fun setMyLocationOnFocus() {
        if (activity!= null && isAdded && !requireActivity().isFinishing &&
            newLocation?.latitude != null && newLocation?.longitude != null) {

            val myLocation = LatLng(newLocation?.latitude!!, newLocation?.longitude!!)
            val locationMarkerBMapDescriptor =
                BitmapDescriptorFactory.fromResource((R.drawable.ic_current_location))
            selectedCameraObj!!.latitude = newLocation!!.latitude
            selectedCameraObj!!.longitude = newLocation!!.longitude
            selectedCameraObj!!.cameraName = "My Location"

            nMap?.addMarker(
                MarkerOptions().position(myLocation).title(selectedCameraObj!!.cameraName)
                    .icon(locationMarkerBMapDescriptor)
            )
            nMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    myLocation, AppConstant.EDIT_MAP_ZOOM_LEVEL
                )
            )

            binding.lyLocationTitle.visibility = View.VISIBLE
            binding.cvSwitchMapType.visibility = View.VISIBLE
            binding.tvNameTitle.text = selectedCameraObj!!.cameraName
            binding.tvLocationName.text = getString(R.string.fetching_camera_location_address)
            getCameraSelected(selectedCameraObj!!)
        } else {
            AppLog.errorLog("get last location : setMyLocationOnFocus")
            locationProvider?.getSingleLocationUpdate(checkIfLastLocationAvailable = true)
        }
    }

    override fun locationUpdateTimeout() {
        AppLog.debugLog(AppConstant.NOT_YET_IMPLEMENTED)
    }

    override fun locationSettingError(error: String) {
        AppLog.debugLog(AppConstant.NOT_YET_IMPLEMENTED)
    }

    override fun locationDisabled() {
        AppLog.debugLog(AppConstant.NOT_YET_IMPLEMENTED)
    }

    @SuppressLint("MissingPermission")
    private fun checkLocationPermission() {
        if (PermissionUtil().checkIfPermissionGranted(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            //nMap?.isMyLocationEnabled = true
            //nMap?.uiSettings?.isMyLocationButtonEnabled = false
            initWebServiceCallAfterChecks()
        } else {
            PermissionUtil().requestPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                object : PermissionResultListener {
                    override fun onPermissionGranted() {
                        //nMap?.isMyLocationEnabled = true
                        //nMap?.uiSettings?.isMyLocationButtonEnabled = false
                        initWebServiceCallAfterChecks()
                    }

                    override fun onPermissionDenied() {
                        if (cameraLocationVM.isMyLocationClicked) showGoToSettingsDialog()
                        cameraLocationVM.isLocationDisabled = true
                    }

                    override fun onPermissionRationaleShouldBeShown() {
                        /**
                         * //No specific implementation.
                         */
                    }
                }
            )
        }
    }

    private fun showGoToSettingsDialog() {

        if (isAdded){
            AlertDialog.Builder(requireContext())
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission, please accept to use location functionality")
                .setPositiveButton(
                    "OK"
                ) { _, _ ->
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${requireActivity().packageName}")
                    ).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        resultLauncher.launch(this)
                    }
                }
                .setNegativeButton("cancel", null)
                .create()
                .show()
        }

    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                AppLog.errorLog("intent$data")
            }
        }

    private fun initWebServiceCallAfterChecks() {

        if (NetworkUtil.isNetworkConnected(requireActivity()) && isAdded && activity != null) {

            val locationManager =
                requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

            cameraLocationVM.isLocationDisabled =
                !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            locationProvider?.let {

                handleOnLocationEnabled()
            }

        } else {
            cameraLocationVM.setMessage(
                false,
                WebConstant.WARNING_NO_NETWORK,
                showAsInLayoutMessage = true
            )
        }
    }


    @SuppressLint("MissingPermission")
    private fun handleOnLocationEnabled() = if (newLocation == null) {
        locationProvider?.getSingleLocationUpdate(checkIfLastLocationAvailable = true)
    } else {
        if (NetworkUtil.isNetworkConnected(requireActivity()) && isAdded && activity != null) {
            //nMap?.isMyLocationEnabled = true
            //nMap?.uiSettings?.isMyLocationButtonEnabled = false
            setMyLocationOnFocus()
        } else {
            cameraLocationVM.setMessage(
                false,
                WebConstant.WARNING_NO_NETWORK,
                showAsInLayoutMessage = true
            )
        }
    }

    private fun showCameraLocationEditScreen() {

        val bundle = Bundle()
        bundle.putSerializable(
            EditCameraLocationFragment.CAMERA_DETAILS,
            selectedCameraObj
        )
        bundle.putInt(EditCameraLocationFragment.SCREEN_MODE, CameraConstants.UPDATE_LOCATION)

        val cameraSectionBaseFragment: CameraSectionBaseFragment =
            parentFragment as CameraSectionBaseFragment
        cameraSectionBaseFragment.showCameraLocationEditScreen(bundle)
    }

    private fun removeEditLocationFragment() {
        (this.parentFragment as CameraSectionBaseFragment).removeEditCameraLocation()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: LocationStatusChangeEvent) {

        if (cameraLocationVM.isLocationDisabled) {

            Handler(Looper.getMainLooper()).postDelayed({

                if (isAdded) {
                    try {

                        val locationManager =
                            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

                        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                            checkLocationPermission()
                        }

                    } catch (e: Exception) {
                        AppLog.errorLog(e.message.toString())
                    }

                }

            }, 1000)
        }
    }

    override fun onStart() {
        super.onStart()

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        requireActivity().registerReceiver(
            gpsReceiver,
            IntentFilter("android.location.PROVIDERS_CHANGED")
        )
    }

    override fun onStop() {
        super.onStop()
        cameraLocationVM.isNewLocationSet = false

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        requireActivity().unregisterReceiver(gpsReceiver)

    }

}
