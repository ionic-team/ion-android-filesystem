ion-android-filesystem

The `ion-android-filesystem` is a library built using `Kotlin` that allows management of files on Android applications.

All file system methods are accessible via the `IONFILEController` class.

## Index

- [Motivation](#motivation)
- [Usage](#usage)
- [Methods](#methods)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## Motivation

This library is used by both [Capacitor](https://github.com/ionic-team/capacitor-filesystem) and [Cordova](https://github.com/ionic-team/cordova-outsystems-file) plugins for Filesystem

## Usage

In your app-level gradle file, import the `ion-android-filesystem` library like so:

```
dependencies {
    implementation("io.ionic.libs:ionfilesystem-android:1.0.0")
}
```

Most methods that are `suspend` and run outside the main thread, so they should be called from a coroutine scope, e.g.:

```kotlin
val controller = IONFILEController(context)
val coroutineScope = CoroutineScope(Dispatchers.Main)
val filePathUri = IONFILEUri.Unresolved("<yourCompleteFilePathGoesHere>")
val options = IONFILECreateOptions(recursive = true)

controller.createFile(filePathUri, options)
    .onSuccess {
        // handle success here
    }
   .onError {
       // handle error here
   }

// call this line when you no longer need to access any filesystem methods
coroutineScope.cancel()
```

Other methods can return a kotlin `Flow` to allow emitting multiple values. They also run outside the main thread. See the example below:

```kotlin
val controller = IONFILEController(context)
val coroutineScope = CoroutineScope(Dispatchers.Main)
val filePathUri = IONFILEUri.Unresolved("<yourCompleteFilePathGoesHere>")
val options = IONFILEReadInChunksOptions(
    encoding = IONFILEEncoding.Base64,
    chunkSize = 1024 * 256 // chunks of around 256 KB  
)

controller.readFileInChunks(path, options)
    .onEach {
        // handle receiving chunks here
    }
    .catch {
        // handle errors here
    }
    .onCompletion { error: Throwable? ->
        if (error == null) {
            // handle file finished read successfully here
        }
    }
    .launchIn(coroutineScope)
```

## Methods

<!-- Dokka generates 100+ documentation files, that can occupy several overall MBs, hence they're not included in the repo -->

Documentation on the methods is generated with [Dokka](https://kotlinlang.org/docs/dokka-introduction.html).

<!-- TODO - in a future PR - publish the docs somewhere, such as Github pages? -->

Clone the repository and run `./gradlew dokkaHtml` or `./gradlew dokkaGfm`. Documentation will be generated in `dokka_docs` folder.

## Troubleshooting

1. Failed to read/write to or from external storage
   - If device is Android 10 or below, make sure you are requesting `READ_EXTERNAL_STORAGE` and/or `WRITE_EXTERNAL_STORAGE` permissions. This library does not handle any permission requests/checks.
   - For Android 10, you may want to add `android:requestLegacyExternalStorage="true` to your application in `AndroidManifest.xml`.
   - For Android 11 and above, for directories like Documents, you can only access files that your app created.
   - General external storage directories will NOT be accessible on Android 11 and above, unless your app declares a special `MANAGE_EXTERNAL_STORAGE` permission. Note that if you publish your app on Google Play, your app may be rejected if they find your app doesn't really need that permission. Refer to the [Android documentation](https://developer.android.com/training/data-storage/manage-all-files) for more information.
2. I obtained an `OutOfMemoryError` when reading my file.
   - Use `readFileInChunks` instead of `readFile` if you're trying to read large files that may not fit in memory (e.g. videos).
   - Make sure you don't use a very large value for `chunkSize` - stick to a few MB at most.
   - Alternatively you can use the `offset` + `length` parameters in `readFile`.
3. The chunk received in `readFileInChunks` is not the same as specified in `IONFILEReadInChunksOptions#chunkSize`
   - The library may change the `chunkSize` internally to better improve the file reading. 
   - The `chunkSize` specifies the total amount of bytes to be read at a time, however the number of bytes returned may vary according to the provided `encoding`.

## Contributing

1. Fork the repository
2. Go to branch development (`git switch development`)
3. Create your feature branch (`git checkout -b feature/amazing-feature`)
4. Commit your changes (`git commit -m 'Add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request to `development`

## License

`ion-android-filesystem` is available under the MIT license. See the [LICENSE](LICENSE) file for more info.

## Support

- Report issues on our [Issue Tracker](https://github.com/ionic-team/ion-android-filesystem/issues)