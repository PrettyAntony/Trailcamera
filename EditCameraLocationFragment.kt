class EditCameraLocationFragment : BaseFragment(), LocationProvider.LocationUpdateListener {

    private lateinit var binding: FragmentEditCameraLocationBinding
    private lateinit var commonToolbarBinding: LayoutCommonToolbarBinding
//    private lateinit var cameraLocationVM: CameraLocationViewModel

    private val cameraLocationVM: CameraLocationViewModel by viewModels()
    private var childProgressbarBinding: ProgressLayoutBinding? = null

    private var cameraInfo: CameraBasicInfo? = null
    private var isSecondTimeMapLoad = 0
    private var nMap: GoogleMap? = null
    private var mapViewType: Int = GoogleMap.MAP_TYPE_HYBRID
    private var newLocationLatLong: LatLng? = null
    private var isMapViewTypeChanged = false

    private lateinit var clusterManager: ClusterManager<LocationItem>

    private var locationProvider: LocationProvider? = null
    private val gpsReceiver = GpsLocationReceiver()
    private var newLocation: Location? = null


    companion object {

        @JvmStatic
        fun newInstance(bundle: Bundle? = null) = EditCameraLocationFragment()
            .apply {
                arguments = bundle
            }

        var TAG = EditCameraLocationFragment::class.java.simpleName.toString()
        const val CAMERA_DETAILS = "camera_details"
        const val SCREEN_MODE = "screen_mode"
    }

    override fun initToolbar() {
        super.initToolbar()
        commonToolbarBinding = binding.toolbar
        commonToolbarBinding.ivBack.visibility = View.VISIBLE
        commonToolbarBinding.tvToolbarTitle.text = resources.getText(R.string.edit_camera_location)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditCameraLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isAdded && activity != null) setMapView()
    }

    private fun setMapView() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun init() {
        //  initViewModel()
        setClickListeners()

        locationProvider = LocationProvider(requireContext(), this)
    }

    override fun setObservers() {
        setProgressObserver()
        setMessageObserver()
        setCameraSelectedObserver()
        postCameraSettingsObserver()
        setCameraLocationsObserver()
    }

    /* private fun initViewModel() {
         cameraLocationVM = ViewModelProvider(requireActivity()).get(
             CameraLocationViewModel::class.java
         )
     }*/

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        googleMap?.let {
            googleMap.mapType = mapViewType
            nMap = googleMap

            if (!isMapViewTypeChanged) {

                val camera = getCameraLocationDetails()
                camera?.let {
                    val latitude = if (camera.latitude != null) camera.latitude!! else 0.0
                    val longitude = if (camera.longitude != null) camera.longitude!! else 0.0

                    var cameraLocation = LatLng(latitude, longitude)

                    if (cameraLocationVM.isNewLocationSet) {
                        newLocationLatLong?.let {
                            cameraLocation = it
                        }
                    } else {
                        cameraLocation = LatLng(latitude, longitude)
                    }
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            cameraLocation, EDIT_MAP_ZOOM_LEVEL
                        )
                    )
                    cameraInfo = camera
                    getCameraSelected(camera)

                    googleMap.setOnCameraMoveListener {
                        if (isSecondTimeMapLoad == 2) {
                            binding.lyLocationTitle.visibility = View.GONE
                            binding.cvSwitchMapType.visibility = View.GONE
                        }

                    }
                    googleMap.setOnCameraIdleListener {
                        if (isSecondTimeMapLoad >= 2) {
                            val newLatLng = googleMap.cameraPosition.target
                            newLocationLatLong = newLatLng
                            cameraLocationVM.isNewLocationSet = true
                            cameraInfo!!.latitude = newLatLng.latitude
                            cameraInfo!!.longitude = newLatLng.longitude
                            if (cameraInfo!!.latitude != null && cameraInfo!!.longitude != null) {
                                binding.lyLocationTitle.visibility = View.VISIBLE
                                binding.cvSwitchMapType.visibility = View.VISIBLE
                                binding.tvNameTitle.text = cameraInfo!!.cameraName
                                binding.tvLocationName.text =
                                    getString(R.string.fetching_camera_location_address)
                                getCameraSelected(cameraInfo!!)
                            } else {
                                binding.lyLocationTitle.visibility = View.GONE
                                binding.cvSwitchMapType.visibility = View.GONE
                            }
                        } else {
                            getCameraSelected(cameraInfo!!)
                        }
                    }
                }
            } else {
                isMapViewTypeChanged = false
            }
        }
    }

    private fun getCameraLocationDetails(): CameraBasicInfo {
        /**  V1.0.6 fixes**/
        var cameraBasicInfo = CameraBasicInfo()
        if (arguments?.getInt(SCREEN_MODE) == UPDATE_LOCATION) {
            cameraBasicInfo = arguments?.getSerializable(CAMERA_DETAILS) as CameraBasicInfo
        } else {
            val cameraSettingObj = arguments?.getSerializable(CAMERA_DETAILS) as CameraSetting
            cameraBasicInfo.cameraID = cameraSettingObj.cameraObj?.cameraID
            cameraBasicInfo.cameraName = cameraSettingObj.cameraConfiguration?.name
            cameraBasicInfo.active = cameraSettingObj.cameraObj?.active
            cameraBasicInfo.latitude = cameraSettingObj.cameraConfiguration?.latitude
            cameraBasicInfo.longitude = cameraSettingObj.cameraConfiguration?.longitude

        }

        if (arguments?.getInt(SCREEN_MODE) == EDIT_LOCATION || arguments?.getInt(SCREEN_MODE) == UPDATE_LOCATION) {
            binding.btnUpdateLocation.text = resources.getText(R.string.camera_confirm_location)
        } else if (arguments?.getInt(SCREEN_MODE) == CONFIRM_LOCATION) {
            if (cameraBasicInfo.latitude.toString()
                    .isEmpty() || cameraBasicInfo.longitude.toString().isEmpty()
            )
                binding.btnUpdateLocation.text = resources.getText(R.string.camera_add_location)
            else binding.btnUpdateLocation.text =
                resources.getText(R.string.camera_confirm_location)
        }

        return cameraBasicInfo
    }

    private fun getCameraSelected(selectedCamera: CameraBasicInfo) {

        if (isAdded && activity != null) {
            if (NetworkUtil.isNetworkConnected(requireActivity())) {
                cameraLocationVM.getCameraSelected(context, selectedCamera)
            } else {
                cameraLocationVM.setMessage(
                    false,
                    WebConstant.WARNING_NO_NETWORK,
                    showAsInLayoutMessage = true
                )
            }
        }

    }

    private fun setProgressObserver() {

        cameraLocationVM.progressStatus.observe(viewLifecycleOwner, EventObserver {

            if (cameraLocationVM.isUpdatingLocationOperationInProgress) {
                if (it) {
                    DialogUtil.showProgressDialog(
                        requireActivity(), getString(R.string.updating_camera_location)
                    )
                } else {
                    DialogUtil.dismissProgressDialog()
                }
            }

        })

    }

    private fun setMessageObserver() {

        cameraLocationVM.message.observe(viewLifecycleOwner, EventObserver {

            if (!it.isSuccessMessage && it.messageCode != null) {

                if (it.showAsInLayoutMessage) {

                    when (it.messageCode) {

                        WebConstant.RESPONSE_EMPTY_ARRAY -> {

                            childProgressbarBinding?.clInLayoutProgressLoading?.visibility =
                                View.GONE
                        }
                        else -> {
                            childProgressbarBinding?.inLayoutProgressBar?.visibility =
                                View.GONE
                            childProgressbarBinding?.clInLayoutProgressLoading?.visibility =
                                View.VISIBLE
                            childProgressbarBinding?.tvLoading?.text =
                                CommonUtil.getMessageText(requireActivity(), it.messageCode)
                        }
                    }

                } else {

                    DialogUtil.showSingleActionButtonAlert(requireActivity(), object :
                        SingleButtonDialogCallback {
                        override fun onConfirmationDialogPositiveButtonClicked(mDialogID: Int) {
                            /**No specific implementation**/
                        }
                    }, CommonUtil.getMessageText(requireActivity(), it.messageCode))
                }
            }
        })

    }

    private fun setCameraSelectedObserver() {
        cameraLocationVM.cameraAddressEditScreenLiveData.observe(viewLifecycleOwner, EventObserver {
            if (it == "${cameraInfo?.latitude.toString()}, ${cameraInfo?.longitude.toString()}") {
                ++isSecondTimeMapLoad
                binding.tvNameTitle.text = cameraInfo?.cameraName
                binding.tvLocationName.text = getString(R.string.no_valid_address_available)
                binding.lyLocationTitle.visibility = View.VISIBLE
                binding.cvSwitchMapType.visibility = View.VISIBLE
                binding.tvLatitudeValue.text = cameraInfo?.latitude.toString()
                binding.tvLongitudeValue.text = cameraInfo?.longitude.toString()
            } else {
                if (cameraInfo != null) {
                    setCameraNameAndAddress(cameraInfo!!, it)
                }
            }
        })
    }

    private fun postCameraSettingsObserver() {

        cameraLocationVM.isCameraSettingPosted.observe(this, Observer {

            it.let {

                if ((null != cameraInfo?.latitude) && (null != cameraInfo?.longitude)) {
                    val result = Bundle().apply {

                        putDouble("newLatitude", cameraInfo?.latitude!!)
                        putDouble("newLongitude", cameraInfo?.longitude!!)
                        putInt("cameraId", cameraInfo?.cameraID!!)
                        putString("newAddress", binding.tvLocationName.text.toString())
                    }
                    if (arguments?.getInt(SCREEN_MODE) == EDIT_LOCATION) {
                        result.putString(
                            "data",
                            CameraSettingsConstant.CAMERA_SETTINGS_EDIT_LOCATION
                        )
                        setFragmentResult(
                            CameraSettingsConstant.RESULT_CAMERA_SETTINGS_FILTER,
                            result
                        )
                    } else if (arguments?.getInt(SCREEN_MODE) == UPDATE_LOCATION) {
                        result.putString("data", CAMERA_EDIT_LOCATION_CONFIRM)
                        setFragmentResult(RESULT_CAMERA_EDIT_LOCATION, result)
                    } else {
                        result.putString("data", CAMERA_EDIT_LOCATION_CONFIRM)
                        setFragmentResult(RESULT_CAMERA_EDIT_LOCATION, result)
                    }
                }
            }
        })
    }

    private fun setCameraLocationsObserver() {

        cameraLocationVM.getAllCameraLocationInfoFromDatabase()
            ?.observe(this, Observer { cameraList ->

                if (!cameraList.isNullOrEmpty()) {

                    if (cameraList.size > 1) {
                        if (nMap != null) {
                            setUpCluster(nMap!!, cameraList)
                        }
                    }
                }

            })
    }

    private fun setUpCluster(map: GoogleMap, cameraList: List<CameraBasicInfo>) {
        map.clear()

        clusterManager = ClusterManager(requireActivity(), map)
        clusterManager.renderer = MarkerClusterRenderer(
            requireActivity(),
            map,
            clusterManager,
            true
        )

        map.setOnCameraIdleListener(clusterManager)
        clusterManager.setOnClusterItemClickListener {
            var selectedCamera = CameraBasicInfo()
            for (i in cameraList.indices) {
                if (it.snippet.equals("Camera $i")) {
                    selectedCamera = cameraList[i]
                    break
                }
            }
            getCameraSelected(selectedCamera)

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

        addLocations(cameraList)
    }

    private fun addLocations(cameraList: List<CameraBasicInfo>) {
        for (i in cameraList.indices) {

            if (cameraInfo?.cameraID != cameraList[i].cameraID) {

                val offset = i / 800.0
                val lat = cameraList[i].latitude!! + offset
                val lon = cameraList[i].longitude!! + offset
                val offsetItem =
                    LocationItem(
                        lat,
                        lon,
                        cameraList[i].cameraName!!,
                        "Camera $i"
                    )
                clusterManager.addItem(offsetItem)

            }
        }
        Handler(Looper.getMainLooper()).postDelayed({

            if (isAdded) {
                mapViewType = GoogleMap.MAP_TYPE_HYBRID
                setMapView()
            }

        }, 2000)
    }


    private fun setCameraNameAndAddress(camera: CameraBasicInfo, addressText: String) {
        ++isSecondTimeMapLoad
        binding.tvNameTitle.text = camera.cameraName
        binding.tvLocationName.text = addressText
        binding.lyLocationTitle.visibility = View.VISIBLE
        binding.cvSwitchMapType.visibility = View.VISIBLE
        binding.tvLatitudeValue.text = camera.latitude.toString()
        binding.tvLongitudeValue.text = camera.longitude.toString()
    }

    private fun setClickListeners() {
        binding.btnUpdateLocation.setOnClickListener {
            if (arguments?.getInt(SCREEN_MODE) == UPDATE_LOCATION) {
                val cameraBasicInfo = arguments?.getSerializable(CAMERA_DETAILS) as CameraBasicInfo
                updateLocationByCoordinates(cameraBasicInfo.cameraID!!)
            } else {
                updateLocationByCameraDetails()
            }

        }

        binding.ivMyLocation.setOnClickListener {
            cameraLocationVM.isMyLocationClicked = true
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                //nMap?.isMyLocationEnabled = true
                //nMap?.uiSettings?.isMyLocationButtonEnabled = false
                if (checkLocationDisabled())
                    checkLocationPermission()
                else {
                    initWebServiceCallAfterChecks()
                }
            } else {
                checkLocationPermission()
            }
        }

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
    }

    private fun triggerSetCameraSettingAPICall(
        cameraId: Int,
        cameraConfiguration: CameraConfigurationDTO
    ) {

        if (NetworkUtil.isNetworkConnected(requireActivity()) && isAdded && activity != null) {
            cameraLocationVM.isAPICallPendingDueToConnectivity = false
            cameraLocationVM.setCameraSettings(cameraId, cameraConfiguration)
        } else {
            cameraLocationVM.isAPICallPendingDueToConnectivity = true
            cameraLocationVM.setMessage(
                false,
                WebConstant.WARNING_NO_NETWORK,
                showAsInLayoutMessage = true
            )
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
            cameraInfo != null && newLocation != null && newLocation?.latitude != null && newLocation?.longitude != null) {

            val myLocation = LatLng(newLocation?.latitude!!, newLocation?.longitude!!)
            newLocationLatLong = myLocation
            cameraLocationVM.isNewLocationSet = true
            nMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    myLocation, EDIT_MAP_ZOOM_LEVEL
                )
            )
            cameraInfo!!.latitude = newLocation!!.latitude
            cameraInfo!!.longitude = newLocation!!.longitude
            binding.lyLocationTitle.visibility = View.VISIBLE
            binding.cvSwitchMapType.visibility = View.VISIBLE
            binding.tvNameTitle.text = cameraInfo!!.cameraName
            binding.tvLocationName.text = getString(R.string.fetching_camera_location_address)
            getCameraSelected(cameraInfo!!)
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

    private fun updateLocationByCoordinates(cameraId: Int) {

        if (NetworkUtil.isNetworkConnected(requireActivity()) && isAdded && activity != null) {
            cameraLocationVM.isAPICallPendingDueToConnectivity = false
            cameraLocationVM.setCameraLocation(
                cameraId,
                UpdateLocationRequestDTO(
                    latitude = cameraInfo!!.latitude!!,
                    longitude = cameraInfo!!.longitude!!
                )
            )
        } else {
            cameraLocationVM.isAPICallPendingDueToConnectivity = true
            cameraLocationVM.setMessage(
                false,
                WebConstant.WARNING_NO_NETWORK,
                showAsInLayoutMessage = true
            )
        }
    }

    private fun updateLocationByCameraDetails() {

        val cameraSettingObj =
            arguments?.getSerializable(CAMERA_DETAILS) as CameraSetting

        cameraSettingObj.cameraConfiguration?.let {config ->

            if (cameraSettingObj.cameraConfiguration != null && cameraSettingObj.cameraObj != null && cameraSettingObj.cameraObj?.cameraID != null) {

                triggerSetCameraSettingAPICall(
                    cameraSettingObj.cameraObj?.cameraID!!, CameraConfigurationDTO(
                        serialnumber = config.serialnumber,
                        captureCount = config.captureCount,
                        captureMode = config.captureMode!!,
                        embedGPS = config.embedGPS,
                        fieldScan = config.fieldScan,
                        flashMode = config.flashMode,
                        imageFormat = config.imageFormat,
                        imageResolution = config.imageResolution,
                        latitude = cameraInfo!!.latitude!!,
                        ledIntensity = config.ledIntensity,
                        longitude = cameraInfo!!.longitude!!,
                        movieLength = config.movieLength,
                        movieResolution = config.movieResolution,
                        name = if(config.name.isNullOrEmpty()) ""
                        else config.name!!,
                        overwrite = config.overwrite,
                        pirDelay = config.pirDelay,
                        pirMode = config.pirMode,
                        pirSensitivity = config.pirSensitivity!!,
                        pirTimes = PirTimesDTO(
                            config.pirTimes?.mF,
                            config.pirTimes?.sAT,
                            config.pirTimes?.sUN
                        ),
                        reportInterval = config.reportInterval,
                        scan1End = config.scan1End!!,
                        scan1Start = config.scan1Start!!,
                        scan2End = config.scan2End!!,
                        scan2Interval = config.scan2Interval!!,
                        scan2Start = config.scan2Start!!,
                        scanInterval = config.scanInterval,
                        //                        sendsize=config.
                        showTimestamp = config.showTimestamp,
                        shutterSpeed = config.shutterSpeed,
                        trackingMode = config.trackingMode,
                        wirelessEnabled = config.wirelessEnabled,
                        workMode = config.workMode

                    )
                )
            }
        }
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

        if (isAdded) {

            AlertDialog.Builder(requireContext())
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission, please accept to use location functionality")
                .setPositiveButton(
                    "OK"
                ) { _, _ ->
                    Intent(
                        ACTION_APPLICATION_DETAILS_SETTINGS,
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

        if (isAdded && activity != null) {

            if (NetworkUtil.isNetworkConnected(requireActivity())) {
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
        } else {

        }

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