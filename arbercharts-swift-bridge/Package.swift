// swift-tools-version: 5.9
import PackageDescription
import Foundation

let nativePath = ProcessInfo.processInfo.environment["ARBER_NATIVE_PATH"] ?? ""
var cFlags: [String] = []
var linkerFlags: [String] = []

if !nativePath.isEmpty {
    cFlags.append("-I\(nativePath)")
    linkerFlags.append(contentsOf: [
        "-L\(nativePath)",
        "-larbercharts-core",
        "-Wl,-rpath,\(nativePath)"
    ])
}

let package = Package(
    name: "ArberCharts",
    platforms: [
        .macOS(.v13),
        .iOS(.v16)
    ],
    products: [
        .library(name: "ArberCharts", targets: ["ArberCharts"])
    ],
    targets: [
        .target(
            name: "ArberChartsC",
            path: "Sources/ArberChartsC",
            publicHeadersPath: "include",
            cSettings: cFlags.isEmpty ? [] : [.unsafeFlags(cFlags)]
        ),
        .target(
            name: "ArberCharts",
            dependencies: ["ArberChartsC"],
            path: "Sources/ArberCharts",
            linkerSettings: linkerFlags.isEmpty ? [] : [.unsafeFlags(linkerFlags)]
        )
    ]
)
