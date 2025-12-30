#!/usr/bin/env python3
"""
FCM Test Script for Emisure
Uses service account credentials to send data-only FCM messages.

Setup:
1. Download service account JSON from Firebase Console:
   Project Settings → Service Accounts → Generate new private key
2. Save as 'service-account.json' in the same directory as this script
3. Run: python3 send_fcm.py <action> <fcm_token>

Usage:
  python3 send_fcm.py lock [fcm_token]
  python3 send_fcm.py unlock [fcm_token]
  python3 send_fcm.py disable_factory_reset [fcm_token]
  python3 send_fcm.py enable_factory_reset [fcm_token]
  python3 send_fcm.py release_device [fcm_token]
  python3 send_fcm.py status [fcm_token]

If fcm_token is not provided, uses DEFAULT_FCM_TOKEN from script.
"""

import json
import sys
import time
import urllib.request
import urllib.error
from pathlib import Path

# Try to use google-auth library, fall back to manual JWT if not available
try:
    from google.oauth2 import service_account
    from google.auth.transport.requests import Request
    HAS_GOOGLE_AUTH = True
except ImportError:
    HAS_GOOGLE_AUTH = False
    import base64
    import hashlib
    import hmac

# Configuration
SERVICE_ACCOUNT_FILE = Path(__file__).parent / "service-account.json"
FCM_SCOPES = ["https://www.googleapis.com/auth/firebase.messaging"]

# Default FCM Token - Set your device token here to avoid passing it every time
# Copy the token from the app's "Device Token" section
# DEFAULT_FCM_TOKEN = "cgiryiMtQp2bY4miVvt_uu:APA91bFhqIcq4BsmCziDuU2AJFuWvCNhUAiQRtnH4N2TRmshrjUXht5UFtCWyc_TO4_KynZn3J5hdriMZrfJzW296R3OtJIfravCCE4ngTy4TyEhdehnNC0"
DEFAULT_FCM_TOKEN = "f6cB1sMFS5CISQNSkB1HHd:APA91bEOQWzs0c8OLbV1d-ndxm8aKqo2zrQ3PMiMQ75LsaRBeBw4TVq2w-xSmY6018btH5RO2RRBi0dTlhWEAZ1wANAubkEsAwRr-4Pm29Vv-O5CszaZYYU"


def get_project_id():
    """Get project ID from service account file."""
    try:
        with open(SERVICE_ACCOUNT_FILE) as f:
            data = json.load(f)
            return data.get("project_id")
    except FileNotFoundError:
        print(f"Error: Service account file not found: {SERVICE_ACCOUNT_FILE}")
        print("\nTo create one:")
        print("1. Go to Firebase Console → Project Settings → Service Accounts")
        print("2. Click 'Generate new private key'")
        print(f"3. Save as '{SERVICE_ACCOUNT_FILE}'")
        sys.exit(1)


def get_access_token_google_auth():
    """Get access token using google-auth library."""
    credentials = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE,
        scopes=FCM_SCOPES
    )
    credentials.refresh(Request())
    return credentials.token


def base64url_encode(data):
    """Base64 URL encode."""
    if isinstance(data, str):
        data = data.encode('utf-8')
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode('utf-8')


def get_access_token_manual():
    """Get access token manually using JWT (no external dependencies)."""
    import subprocess
    
    with open(SERVICE_ACCOUNT_FILE) as f:
        sa_data = json.load(f)
    
    # Create JWT header and claims
    header = {"alg": "RS256", "typ": "JWT"}
    now = int(time.time())
    claims = {
        "iss": sa_data["client_email"],
        "scope": " ".join(FCM_SCOPES),
        "aud": "https://oauth2.googleapis.com/token",
        "iat": now,
        "exp": now + 3600
    }
    
    # Encode header and claims
    header_b64 = base64url_encode(json.dumps(header))
    claims_b64 = base64url_encode(json.dumps(claims))
    unsigned_jwt = f"{header_b64}.{claims_b64}"
    
    # Sign with private key using openssl
    private_key = sa_data["private_key"]
    
    # Write private key to temp file
    key_file = Path("/tmp/fcm_private_key.pem")
    key_file.write_text(private_key)
    
    try:
        # Use openssl to sign
        result = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", str(key_file)],
            input=unsigned_jwt.encode(),
            capture_output=True
        )
        signature = base64url_encode(result.stdout)
    finally:
        key_file.unlink()  # Delete temp file
    
    signed_jwt = f"{unsigned_jwt}.{signature}"
    
    # Exchange JWT for access token
    token_request_data = urllib.parse.urlencode({
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion": signed_jwt
    }).encode()
    
    req = urllib.request.Request(
        "https://oauth2.googleapis.com/token",
        data=token_request_data,
        headers={"Content-Type": "application/x-www-form-urlencoded"}
    )
    
    with urllib.request.urlopen(req) as response:
        token_data = json.loads(response.read())
        return token_data["access_token"]


def get_access_token():
    """Get OAuth2 access token."""
    if HAS_GOOGLE_AUTH:
        return get_access_token_google_auth()
    else:
        return get_access_token_manual()


def send_fcm_message(fcm_token, data_payload, project_id):
    """Send FCM data-only message."""
    url = f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"
    
    access_token = get_access_token()
    
    message = {
        "message": {
            "token": fcm_token,
            "data": data_payload,
            # Android specific - high priority for immediate delivery
            "android": {
                "priority": "high"
            }
        }
    }
    
    request_data = json.dumps(message).encode('utf-8')
    
    req = urllib.request.Request(
        url,
        data=request_data,
        headers={
            "Authorization": f"Bearer {access_token}",
            "Content-Type": "application/json"
        }
    )
    
    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read())
            return True, result
    except urllib.error.HTTPError as e:
        error_body = e.read().decode('utf-8')
        return False, {"error": e.reason, "details": error_body}


def get_data_payload(action):
    """Get the data payload for a given action."""
    if action == "lock":
        return {
            "action": "lock",
            "title": "Payment Required",
            "sellerName": "Waqar Ali Siyal",
            "sellerPhone": "0300-1234567",
            "amountDue": "PKR 5,000",
            "dueDate": "Jan 15, 2025",
            "message": "Please pay your outstanding balance to unlock this device."
        }
    else:
        return {"action": action}


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 send_fcm.py <action> [fcm_token]")
        print("\nIf fcm_token is not provided, uses DEFAULT_FCM_TOKEN from script.")
        print("\nActions:")
        print("  lock                  - Lock device with payment screen")
        print("  unlock                - Unlock device")
        print("  disable_factory_reset - Disable factory reset")
        print("  enable_factory_reset  - Enable factory reset")
        print("  disable_debugging     - Disable USB debugging")
        print("  enable_debugging      - Enable USB debugging")
        print("  enforce_notifications - Force enable app notifications")
        print("  release_device        - FULLY release device (customer paid all)")
        print("  status                - Get device status (check logs)")
        print("  test                  - Show toast to verify FCM is working")
        sys.exit(1)
    
    action = sys.argv[1]
    
    # Use provided token or fall back to default
    if len(sys.argv) >= 3:
        fcm_token = sys.argv[2]
    elif DEFAULT_FCM_TOKEN:
        fcm_token = DEFAULT_FCM_TOKEN
        print("Using DEFAULT_FCM_TOKEN from script")
    else:
        print("Error: No FCM token provided and DEFAULT_FCM_TOKEN is not set.")
        print("Either pass token as argument or set DEFAULT_FCM_TOKEN in the script.")
        sys.exit(1)
    
    valid_actions = [
        "lock", "unlock", 
        "disable_factory_reset", "enable_factory_reset",
        "disable_debugging", "enable_debugging",
        "enforce_notifications",
        "release_device", "status", "test"
    ]
    if action not in valid_actions:
        print(f"Error: Invalid action '{action}'")
        print(f"Valid actions: {', '.join(valid_actions)}")
        sys.exit(1)
    
    project_id = get_project_id()
    print(f"Project ID: {project_id}")
    print(f"Action: {action}")
    print(f"FCM Token: {fcm_token[:20]}...")
    print()
    
    data_payload = get_data_payload(action)
    print(f"Sending data payload: {json.dumps(data_payload, indent=2)}")
    print()
    
    success, result = send_fcm_message(fcm_token, data_payload, project_id)
    
    if success:
        print("✅ Message sent successfully!")
        print(f"Response: {json.dumps(result, indent=2)}")
    else:
        print("❌ Failed to send message")
        print(f"Error: {json.dumps(result, indent=2)}")
        sys.exit(1)


if __name__ == "__main__":
    main()
