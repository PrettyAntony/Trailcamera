class AppUpdateCheckForegroundService : Service() {
   
    private var job: Job? = null
    val imageRepo = ImageRepo()

    inner class LocalBinder : Binder() {
        fun getService(): AppUpdateCheckForegroundService = this@AppUpdateCheckForegroundService
    }

    // Create the instance on the service.
    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()

        /*createNotificationChannel()
        val notificationIntent = Intent(this, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.checking_for_updates))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentText(getString(R.string.fetching_data))
            .setSmallIcon(getNotificationIcon())
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(getString(R.string.fetching_data))
            )
            .setContentIntent(pendingIntent)
            .build()*/
        //startForeground(onGoingNotificationID, notification)


        /*try {

            if (NetworkUtil.isNetworkConnected(this)) {
                /* createNotificationChannel()
                 val notificationIntent = Intent(this, SplashActivity::class.java)
                 val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
                 val notification = NotificationCompat.Builder(this, channelId)
                     .setContentTitle(getString(R.string.checking_for_updates))
                     .setPriority(NotificationCompat.PRIORITY_LOW)
                     .setContentText(getString(R.string.fetching_data))
                     .setSmallIcon(getNotificationIcon())
                     .setStyle(
                         NotificationCompat.BigTextStyle().bigText(getString(R.string.fetching_data))
                     )
                     .setContentIntent(pendingIntent)
                     .build()
                 startForeground(onGoingNotificationID, notification)*/

                job = GlobalScope.launch(Dispatchers.IO) {

                    imageRepo.readFileWithoutDownload().collect {
                        it?.let {
                            checkForUpdate(it)
                            AppState.config.prefForceUdpatedStatus = true
                        } ?: kotlin.run {
                            AppState.config.prefForceUdpatedStatus = false
                        }
                        stopForeground(true)
                        stopSelf()
                        AppConstant.isAppUpdateCheckServiceRunning = false

                    }
                }

            } else {
                AppState.config.prefForceUdpatedStatus = false
                stopForeground(true)
                stopSelf()
                AppConstant.isAppUpdateCheckServiceRunning = false
            }

        }catch (e:java.lang.Exception){
            AppState.config.prefForceUdpatedStatus = false
            AppConstant.isAppUpdateCheckServiceRunning = false
        }*/

    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        /*if (NetworkUtil.isNetworkConnected(this)) {
            createNotificationChannel()
            val notificationIntent = Intent(this, SplashActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.checking_for_updates))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentText(getString(R.string.fetching_data))
                .setSmallIcon(getNotificationIcon())
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(getString(R.string.fetching_data))
                )
                .setContentIntent(pendingIntent)
                .build()
            startForeground(onGoingNotificationID, notification)
        } else {
            AppState.config.prefForceUdpatedStatus = false
        }*/

        AppConstant.isAppUpdateCheckServiceRunning = true

        try {

            if (NetworkUtil.isNetworkConnected(this)) {
                /* createNotificationChannel()
                 val notificationIntent = Intent(this, SplashActivity::class.java)
                 val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
                 val notification = NotificationCompat.Builder(this, channelId)
                     .setContentTitle(getString(R.string.checking_for_updates))
                     .setPriority(NotificationCompat.PRIORITY_LOW)
                     .setContentText(getString(R.string.fetching_data))
                     .setSmallIcon(getNotificationIcon())
                     .setStyle(
                         NotificationCompat.BigTextStyle().bigText(getString(R.string.fetching_data))
                     )
                     .setContentIntent(pendingIntent)
                     .build()
                 startForeground(onGoingNotificationID, notification)*/

                job = GlobalScope.launch(Dispatchers.IO) {

                    imageRepo.readFileWithoutDownload().collect {
                        it?.let {
                            checkForUpdate(it)
                            AppState.config.prefForceUdpatedStatus = true
                        } ?: kotlin.run {
                            AppState.config.prefForceUdpatedStatus = false
                        }
                        AppState.config.lastUpdateCheckTimeStamp= Calendar.getInstance().timeInMillis
                        stopForeground(true)
                        stopSelf()
                        AppConstant.isAppUpdateCheckServiceRunning = false
                    }
                }

            } else {
                AppState.config.prefForceUdpatedStatus = false
                stopForeground(true)
                stopSelf()
                AppConstant.isAppUpdateCheckServiceRunning = false
            }

        }catch (e:java.lang.Exception){
            AppState.config.prefForceUdpatedStatus = false
            AppConstant.isAppUpdateCheckServiceRunning = false
            AppState.config.lastUpdateCheckTimeStamp= Calendar.getInstance().timeInMillis
        }

        return START_NOT_STICKY
    }

    private fun getNotificationIcon(): Int {
        val useWhiteIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        return if (useWhiteIcon) R.drawable.ic_bushnell_round else R.drawable.app_rate_logo
    }

    private fun checkForUpdate(fileText: String) {
        val fileTextArray = fileText.lines()
        AppLog.errorLog("$fileTextArray")
        if (fileTextArray.size >= 2) {
            val latestVersion = splitWithEqualSignRetrieveValue(fileTextArray[0]) ?: ""
            val isForceUpdate = splitWithEqualSignRetrieveValue(fileTextArray[1]).toBoolean()
            val downloadSize = "0"

            try {
                AppState.config.appVersionInServer = latestVersion
                AppState.config.needForceUpdate = isForceUpdate
                AppState.config.downloadSize = downloadSize ?: ""
                AppState.config.skipUpdate=false
            } catch (e: Exception) {

            }

            EventBus.getDefault().postSticky(
                UpdateMessageEvent(
                    latestVersion = latestVersion,
                    isForceUpdate = isForceUpdate,
                    downloadSize = downloadSize
                )
            )

        }
    }

    private fun splitWithEqualSignRetrieveValue(text: String): String? {
        val splitArray = text.split("=")
        if (splitArray.size == 2) {
            return splitArray[1]
        }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.setSound(null, null)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        AppConstant.isAppUpdateCheckServiceRunning = false
    }
}