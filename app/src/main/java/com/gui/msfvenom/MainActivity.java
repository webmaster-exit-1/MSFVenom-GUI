package com.gui.msfvenom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

	private Spinner payloadSpinner;
	private EditText lhostEditText, lportEditText, serverUrlEditText;
	private Button generateButton, testConnectionButton;
	private TextView outputTextView;
	private static final int PERMISSION_REQUEST_CODE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Check and request permissions
		checkPermissions();

		// Initialize UI components
		payloadSpinner = findViewById(R.id.payloadSpinner);
		lhostEditText = findViewById(R.id.lhostEditText);
		lportEditText = findViewById(R.id.lportEditText);
		serverUrlEditText = findViewById(R.id.serverUrlEditText);
		generateButton = findViewById(R.id.generateButton);
		testConnectionButton = findViewById(R.id.testConnectionButton);
		outputTextView = findViewById(R.id.outputTextView);

		// --- Populate Spinner with APK-focused payloads ---
		String[] payloads = new String[] {
			"android/meterpreter/reverse_tcp",
			"android/meterpreter/reverse_https",
			"android/shell/reverse_tcp",
			"android/shell/reverse_https"
		};
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
				payloads);
		payloadSpinner.setAdapter(adapter);

		// Set default server URL (localhost for testing)
		serverUrlEditText.setText("http://localhost:8080");

		// --- Set On-Click Listeners ---
		generateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				generatePayload();
			}
		});

		testConnectionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				testServerConnection();
			}
		});
	}

	private void checkPermissions() {
		String[] permissions = {
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.INTERNET
		};

		boolean allPermissionsGranted = true;
		for (String permission : permissions) {
			if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
				allPermissionsGranted = false;
				break;
			}
		}

		if (!allPermissionsGranted) {
			ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
		}
	}

	private void testServerConnection() {
		String serverUrl = serverUrlEditText.getText().toString().trim();
		if (serverUrl.isEmpty()) {
			Toast.makeText(this, "Please enter server URL", Toast.LENGTH_SHORT).show();
			return;
		}

		new TestConnectionTask().execute(serverUrl);
	}

	private void generatePayload() {
		// Get user input
		String selectedPayload = payloadSpinner.getSelectedItem().toString();
		String lhost = lhostEditText.getText().toString().trim();
		String lport = lportEditText.getText().toString().trim();
		String serverUrl = serverUrlEditText.getText().toString().trim();

		// Basic input validation
		if (lhost.isEmpty() || lport.isEmpty()) {
			outputTextView.setText("Error: LHOST and LPORT cannot be empty.");
			return;
		}

		if (serverUrl.isEmpty()) {
			outputTextView.setText("Error: Server URL cannot be empty.");
			return;
		}

		// Generate APK payload via server
		new GeneratePayloadTask().execute(selectedPayload, lhost, lport, serverUrl);
	}

	// AsyncTask for testing server connection
	private class TestConnectionTask extends AsyncTask<String, Void, String> {
		@Override
		protected void onPreExecute() {
			outputTextView.setText("Testing server connection...");
		}

		@Override
		protected String doInBackground(String... params) {
			String serverUrl = params[0];
			try {
				URL url = new URL(serverUrl + "/status");
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(5000);

				int responseCode = connection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					return "Connection successful! Server is reachable.";
				} else {
					return "Server responded with code: " + responseCode;
				}
			} catch (Exception e) {
				return "Connection failed: " + e.getMessage();
			}
		}

		@Override
		protected void onPostExecute(String result) {
			outputTextView.setText(result);
		}
	}

	// AsyncTask for generating payload via server
	private class GeneratePayloadTask extends AsyncTask<String, Void, String> {
		@Override
		protected void onPreExecute() {
			outputTextView.setText("Generating APK payload...\nThis may take a few minutes.");
			generateButton.setEnabled(false);
		}

		@Override
		protected String doInBackground(String... params) {
			String payload = params[0];
			String lhost = params[1];
			String lport = params[2];
			String serverUrl = params[3];

			try {
				// Construct the request URL
				String requestUrl = serverUrl + "/generate?" +
					"payload=" + payload +
					"&lhost=" + lhost +
					"&lport=" + lport +
					"&format=apk";

				URL url = new URL(requestUrl);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(30000); // 30 seconds
				connection.setReadTimeout(60000);    // 60 seconds

				int responseCode = connection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					// Save the APK file
					String fileName = "msfvenom_payload_" + System.currentTimeMillis() + ".apk";
					File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
					File outputFile = new File(downloadsDir, fileName);

					try (OutputStream outputStream = new FileOutputStream(outputFile)) {
						byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = connection.getInputStream().read(buffer)) != -1) {
							outputStream.write(buffer, 0, bytesRead);
						}
					}

					return "APK payload generated successfully!\nSaved to: " + outputFile.getAbsolutePath();
				} else {
					return "Server error: " + responseCode + "\n" + connection.getResponseMessage();
				}
			} catch (Exception e) {
				return "Error generating payload: " + e.getMessage();
			}
		}

		@Override
		protected void onPostExecute(String result) {
			outputTextView.setText(result);
			generateButton.setEnabled(true);
		}
	}
}
