package com.ricsdev.uconnect.presentation.setupScreen

//@Composable
//actual fun SetupScreen() {
//    val viewModel: PairingViewModel = koinViewModel()
//
//    val connectionState by viewModel.connectionState.collectAsState()
//    val currentFrame by viewModel.currentFrame.collectAsState()
//    val cameraMode by viewModel.cameraMode.collectAsState()
//    val cameraRotation by viewModel.cameraRotation.collectAsState()
//    val flipHorizontal by viewModel.flipHorizontal.collectAsState()
//    val flipVertical by viewModel.flipVertical.collectAsState()
//
//    LaunchedEffect(cameraMode, flipHorizontal, flipVertical, cameraRotation) {
//        viewModel.updateCameraOrientation()
//    }
//
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        val serverUrl = viewModel.getServerUrl()
//        val painter = rememberQrKitPainter(data = serverUrl) {
//            shapes = QrKitShapes(
//                ballShape = getSelectedQrBall(QrBallType.SquareQrBall()),
//                darkPixelShape = getSelectedPixel(QrPixelType.SquarePixel()),
//                frameShape = getSelectedFrameShape(QrFrameType.SquareFrame()),
//                codeShape = getSelectedPattern(PatternType.SquarePattern),
//            )
//        }
//
//        Image(
//            painter = painter,
//            contentDescription = "Server QR Code",
//            modifier = Modifier.size(200.dp)
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Text("Server URL: $serverUrl")
//        Text("Status: ${connectionState::class.simpleName}")
//
//        if (connectionState is ConnectionState.Connected) {
////            currentFrame?.let { imageBitmap ->
////                Image(
////                    bitmap = imageBitmap,
////                    contentDescription = "Camera Feed",
////                    modifier = Modifier
////                        .fillMaxWidth()
////                        .height(500.dp)
////                        .graphicsLayer {
////                            rotationZ = if (cameraMode == "Back") cameraRotation + 270f else cameraRotation + 90f
////                            scaleX = if (flipVertical) -1f else 1f
////                            scaleY = if (flipHorizontal) -1f else 1f
////                        }
////                )
////            }
//        }
//    }
//}