# Custom Android VPN Client (VpnService + GitHub Actions APK Builder)

This is a complete, custom-built Android VPN application project using Android's native `VpnService` API, along with an automated GitHub Actions workflow that compiles and signs a real, installable `.apk` file in the cloud.

## How to Get Your Working `.apk` File (No PC / Android Studio Required)

1. **Upload to GitHub**:
   - Create a free repository on [GitHub](https://github.com/new).
   - Upload all the files in this project folder (including the `.github/workflows/build-apk.yml` file) to your repository.
2. **Download Your Compiled APK**:
   - Click the **Actions** tab in your GitHub repository.
   - Select the **Build Android VPN APK** workflow run.
   - Once it finishes (takes ~2 minutes), scroll down to the **Artifacts** section and click **`CustomVPN-debug-apk`** to download your compiled, installable `.apk` file!

## How It Works
- **`LocalVpnService.kt`**: Implements Android's `android.net.VpnService` to establish a virtual network interface (`tun0`) on your phone, configure DNS servers (e.g., Cloudflare `1.1.1.1`), and route device packets through the tunnel.
- **`MainActivity.kt`**: Provides a clean Material UI where you can enter your VPN server endpoint/port, configure DNS, request Android VPN permission (`VpnService.prepare`), and toggle the VPN connection on or off with live connection status.
"# testapk" 
