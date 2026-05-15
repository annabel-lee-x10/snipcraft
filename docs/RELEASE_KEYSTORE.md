# Release Keystore — Snipcraft

> **TODO (user): generate release keystore.**
> Run the keytool command below, fill in `keystore.properties` at project root,
> back up both off-machine. Required before `./gradlew bundleRelease` can produce
> a signed AAB for Play Store upload.

---

## ⚠️  Read this first

The release keystore is the cryptographic identity that proves every future Snipcraft
update is from you. **If you lose the keystore file or forget the passwords, you can
never update the app on Play Store.** The existing install becomes orphaned and you
would have to publish a new app under a new package name.

Back up immediately after generating:
- Copy `app/keystore/snipcraft-release.keystore` to an offline location (USB drive,
  encrypted password manager file attachment, or similar).
- Store `storePassword` and `keyPassword` in your password manager.

---

## Step 1: Create the keystore directory

```bash
mkdir -p app/keystore
```

The directory is gitignored via `*.keystore` in `.gitignore` — the file inside it
will never be committed.

## Step 2: Generate the keystore

Run from the project root:

```bash
keytool -genkeypair \
  -keystore app/keystore/snipcraft-release.keystore \
  -alias snipcraft \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Snipcraft, OU=Personal, O=a10101100, L=Unknown, S=Unknown, C=US" \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD
```

Replace `YOUR_STORE_PASSWORD` and `YOUR_KEY_PASSWORD` with strong passwords you will
store in your password manager. They can be the same value if you prefer.

`-validity 10000` is ~27 years — long enough that expiry is not a concern for a
personal project.

## Step 3: Create `keystore.properties`

Create a file called `keystore.properties` **at the project root** (it is gitignored):

```properties
storeFile=app/keystore/snipcraft-release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=snipcraft
keyPassword=YOUR_KEY_PASSWORD
```

Fill in the same passwords you used in the keytool command above.
`storeFile` is a path relative to the project root.

## Step 4: Get the SHA-1 certificate fingerprint (for Play Console)

```bash
keytool -list -v \
  -keystore app/keystore/snipcraft-release.keystore \
  -alias snipcraft \
  -storepass YOUR_STORE_PASSWORD
```

Copy the `SHA1:` fingerprint from the output. Play Console may ask for it in the
App Signing settings — paste it there.

## Step 5: Build the signed AAB

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
  PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew bundleRelease
```

Output file: `app/build/outputs/bundle/release/app-release.aab`

Upload this file to Play Console → Internal Testing → Create new release.

## Step 6: Back up NOW

Before doing anything else:

1. Copy `app/keystore/snipcraft-release.keystore` to at least two offline locations.
2. Confirm your password manager has `storePassword` and `keyPassword` saved.
3. Never commit the keystore or `keystore.properties` to git. The `.gitignore` already
   prevents this, but double-check with `git status` if in doubt.
