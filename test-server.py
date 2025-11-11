#!/usr/bin/env python3
"""
Simple HTTP server to receive and display usage data from WSO2 Usage Data Collector.
This server listens on port 8080 and logs all incoming data.
"""

import http.server
import socketserver
import json
import datetime
from urllib.parse import urlparse, parse_qs

class UsageDataHandler(http.server.BaseHTTPRequestHandler):
    
    def do_POST(self):
        """Handle POST requests with usage data"""
        try:
            # Get the content length
            content_length = int(self.headers.get('Content-Length', 0))
            
            # Read the request body
            post_data = self.rfile.read(content_length)
            
            # Parse JSON data
            try:
                json_data = json.loads(post_data.decode('utf-8'))
                formatted_data = json.dumps(json_data, indent=2)
            except json.JSONDecodeError:
                formatted_data = post_data.decode('utf-8')
            
            # Log the received data
            timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            print(f"\n{'='*60}")
            print(f"📊 USAGE DATA RECEIVED at {timestamp}")
            print(f"{'='*60}")
            print(f"🌐 From: {self.client_address[0]}:{self.client_address[1]}")
            print(f"📍 Path: {self.path}")
            print(f"📋 Headers:")
            for header, value in self.headers.items():
                print(f"   {header}: {value}")
            print(f"📦 Data:")
            print(formatted_data)
            print(f"{'='*60}\n")
            
            # Send success response
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            response = {
                "status": "success",
                "message": "Usage data received successfully",
                "timestamp": timestamp,
                "data_size": len(post_data)
            }
            
            self.wfile.write(json.dumps(response).encode('utf-8'))
            
        except Exception as e:
            print(f"❌ Error processing request: {str(e)}")
            self.send_response(500)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            error_response = {
                "status": "error",
                "message": str(e)
            }
            self.wfile.write(json.dumps(error_response).encode('utf-8'))
    
    def do_GET(self):
        """Handle GET requests - show server status"""
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        
        html_content = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>WSO2 Usage Data Collector Test Server</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 40px; }
                .status { color: green; font-weight: bold; }
                .endpoint { background: #f0f0f0; padding: 10px; border-radius: 5px; }
                .info { background: #e7f3ff; padding: 15px; border-radius: 5px; margin: 10px 0; }
            </style>
        </head>
        <body>
            <h1>🚀 WSO2 Usage Data Collector Test Server</h1>
            <p class="status">✅ Server is running and ready to receive data!</p>
            
            <div class="info">
                <h3>📡 Server Details:</h3>
                <ul>
                    <li><strong>Port:</strong> 8080</li>
                    <li><strong>Endpoint:</strong> <code class="endpoint">http://localhost:8080/receiver</code></li>
                    <li><strong>Status:</strong> Active</li>
                    <li><strong>Time:</strong> """ + datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S") + """</li>
                </ul>
            </div>
            
            <div class="info">
                <h3>🔧 How to Test:</h3>
                <ol>
                    <li>Update your HttpPublisher URL to: <code>http://localhost:8080/receiver</code></li>
                    <li>Deploy your usage data collector to MI</li>
                    <li>Generate some transactions in MI</li>
                    <li>Watch the server console for incoming data</li>
                </ol>
            </div>
            
            <div class="info">
                <h3>📝 What You'll See:</h3>
                <p>When usage data is sent, you'll see detailed logs in the server console including:</p>
                <ul>
                    <li>Timestamp of data reception</li>
                    <li>Source IP and port</li>
                    <li>HTTP headers</li>
                    <li>Complete JSON payload with transaction data</li>
                </ul>
            </div>
        </body>
        </html>
        """
        
        self.wfile.write(html_content.encode('utf-8'))
    
    def log_message(self, format, *args):
        """Override to customize logging"""
        return  # Suppress default HTTP server logs

def main():
    PORT = 8080
    
    print("🚀 Starting WSO2 Usage Data Collector Test Server...")
    print(f"📡 Server will listen on port {PORT}")
    print(f"🌐 Access server status at: http://localhost:{PORT}")
    print(f"📊 Data endpoint: http://localhost:{PORT}/receiver")
    print("=" * 60)
    print("💡 To test, update your HttpPublisher URL to:")
    print(f"   http://localhost:{PORT}/receiver")
    print("=" * 60)
    print("🔍 Waiting for usage data... (Press Ctrl+C to stop)")
    print()
    
    try:
        with socketserver.TCPServer(("", PORT), UsageDataHandler) as httpd:
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Server stopped by user")
    except Exception as e:
        print(f"❌ Server error: {str(e)}")

if __name__ == "__main__":
    main()