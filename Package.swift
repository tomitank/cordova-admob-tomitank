import PackageDescription

let package = Package(
    name: "cordova-admob-tomitank",
    platforms: [.iOS(.v13)],
    products: [
        .library(
			name: "cordova-admob-tomitank",
			targets: ["cordova-admob-tomitank"]
		)
    ],
    dependencies: [
		/*.package(
			name: "cordova-ios",
            url: "https://github.com/apache/cordova-ios.git",
            from: "8.0.0"
        ),*/
		.package(
			name: "GoogleMobileAds",
			url: "https://github.com/googleads/swift-package-manager-google-mobile-ads.git",
			from: "13.2.0"
		)
	],
    targets: [
        .target(
            name: "cordova-admob-tomitank",
            path: "src/ios",
            resources: [],
            publicHeadersPath: ".",
            dependencies: [
                //.product(name: "Cordova", package: "cordova-ios"),
                .product(name: "GoogleMobileAds", package: "GoogleMobileAds")
            ]
        )
    ]
)
