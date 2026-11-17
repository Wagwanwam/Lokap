"""Reset only the local demo-lokap-social emulator project for physical-device QA.

Auth 127.0.0.1:19099; Firestore 127.0.0.1:18080. Never uses production credentials.
"""
import datetime
import json
import urllib.request

PROJECT = "demo-lokap-social"
AUTH = "http://127.0.0.1:19099"
FIRESTORE = "http://127.0.0.1:18080"


def call(base, path, data=None, method="POST"):
    assert base in (AUTH, FIRESTORE) and PROJECT in path
    request = urllib.request.Request(
        base + path,
        data=None if data is None else json.dumps(data).encode(),
        headers={"Authorization": "Bearer owner", "Content-Type": "application/json"},
        method=method,
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read() or "{}")


if __name__ == "__main__":
    call(AUTH, f"/emulator/v1/projects/{PROJECT}/accounts", method="DELETE")
    call(FIRESTORE, f"/emulator/v1/projects/{PROJECT}/databases/(default)/documents", method="DELETE")
    now = datetime.datetime.now(datetime.timezone.utc).isoformat()
    for suffix in ("a", "b"):
        uid = f"qasocial{suffix}"
        call(AUTH, f"/identitytoolkit.googleapis.com/v1/projects/{PROJECT}/accounts", {
            "localId": uid, "email": f"qa-{suffix}@example.test", "password": "TestSocial2026!",
            "emailVerified": True, "displayName": f"QA Social {suffix.upper()}",
        })
        profile = {
            "username": {"stringValue": uid},
            "displayName": {"stringValue": f"QA Social {suffix.upper()}"},
            "searchName": {"stringValue": f"qa social {suffix}"},
            "avatarUrl": {"nullValue": None}, "bio": {"stringValue": "Local emulator fixture"},
            "friendCount": {"integerValue": "0"}, "createdAt": {"timestampValue": now},
        }
        prefix = f"/v1/projects/{PROJECT}/databases/(default)/documents"
        call(FIRESTORE, f"{prefix}/users/{uid}", {"fields": profile}, method="PATCH")
        call(FIRESTORE, f"{prefix}/usernames/{uid}", {"fields": {"uid": {"stringValue": uid}}}, method="PATCH")
        print(f"Prepared local verified fixture: {uid}")
