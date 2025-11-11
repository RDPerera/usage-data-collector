#!/usr/bin/env python3
"""
Simple test script to verify that our local test server can receive data.
This simulates what the WSO2 MI usage data collector would send.
"""

import requests
import json
import datetime

def test_publisher():
    url = "http://localhost:8080/receiver"
    
    # Sample usage data that mimics what TransactionUsageData would send
    test_data = {
        "id": "test-transaction-001",
        "totalCount": 42,
        "hourStartTime": 1699603200000,  # Example timestamp
        "hourEndTime": 1699606800000,    # Example timestamp
        "recordedTime": datetime.datetime.now().isoformat(),
        "timestamp": datetime.datetime.now().isoformat()
    }
    
    headers = {
        "Content-Type": "application/json"
    }
    
    try:
        print("📤 Sending test data to local server...")
        print(f"🎯 URL: {url}")
        print(f"📦 Data: {json.dumps(test_data, indent=2)}")
        
        response = requests.post(url, json=test_data, headers=headers, timeout=10)
        
        if response.status_code == 200:
            print("✅ SUCCESS: Data sent successfully!")
            print(f"📄 Response: {response.text}")
        else:
            print(f"❌ ERROR: Server returned status code {response.status_code}")
            print(f"📄 Response: {response.text}")
            
    except requests.exceptions.ConnectionError:
        print("❌ ERROR: Could not connect to server. Is the test server running?")
    except requests.exceptions.Timeout:
        print("❌ ERROR: Request timed out")
    except Exception as e:
        print(f"❌ ERROR: {str(e)}")

if __name__ == "__main__":
    print("🧪 Testing WSO2 Usage Data Publisher Connection")
    print("=" * 50)
    test_publisher()