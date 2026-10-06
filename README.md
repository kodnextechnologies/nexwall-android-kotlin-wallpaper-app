# Android Kotlin Wallpaper App using a Free Wallpaper API (NexWall)

A minimal, open-source Android wallpaper app in Kotlin and Jetpack Compose, built on the [NexWall **free wallpaper API**](https://nexwall.kodnextech.com/wallpaper-api/free-wallpaper-api). It shows wallpaper categories, a paginated grid with infinite scroll, a full-screen preview, and sets the wallpaper on the home screen, lock screen or both with Android's `WallpaperManager`.

> Step-by-step tutorial: [Android Kotlin wallpaper API guide](https://nexwall.kodnextech.com/wallpaper-api/guides/android-kotlin-wallpaper-api-guide)

## Features

- Kotlin + Jetpack Compose (Material 3, dynamic color on Android 12+)
- Retrofit + kotlinx.serialization for the REST API, OkHttp interceptor for auth headers
- Coil 3 for image loading and caching
- Category grid with cover images and wallpaper counts
- Wallpaper grid with **infinite scroll pagination** (`LazyVerticalGrid` + `snapshotFlow`)
- Sort by newest, popular, random or oldest
- **Set as wallpaper** (home, lock or both) via `WallpaperManager.setStream()`: the image is streamed, not decoded into a bitmap
- Shows your **remaining daily API quota** in the top bar
- Handles `401`, `404`, `422` and `429` (quota exceeded, with `Retry-After`); stops paging after an error until you tap Retry
- API key read from `local.properties` into `BuildConfig`, never committed
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`)
- No DI framework, no navigation library: one `ViewModel` and a small back stack

## What it does

1. **Categories**: the categories your plan can access. "All" opens every wallpaper.
2. **Wallpapers**: a 9:16 thumbnail grid that loads the next page as you scroll. Tap the sort button to change the order.
3. **Preview**: the full image over its cached thumbnail. **Set as wallpaper** asks for home, lock, or both.

Min SDK 24 (Android 7.0), target SDK 36.

## Quick start

### 1. Get a free API key

Sign up at **https://nexwall.kodnextech.com/developers/register**. The free plan needs no credit card.

### 2. Configure

```bash
git clone https://github.com/kodnextechnologies/nexwall-android-kotlin-wallpaper-app.git
cd nexwall-android-kotlin-wallpaper-app
```

Open the folder in Android Studio once (it creates `local.properties` with your `sdk.dir`), then add your key to `local.properties`:

```properties
NEXWALL_API_KEY=your_key_here
```

`local.properties` is git-ignored; `local.properties.example` shows the format. On CI you can set a `NEXWALL_API_KEY` environment variable instead.

### 3. Run

Press **Run** in Android Studio, or from the command line:

```bash
./gradlew installDebug
```

**Gradle wrapper:** this repository includes `gradle/wrapper/gradle-wrapper.properties` but not the wrapper JAR or scripts. Android Studio sets them up when it syncs. To generate them yourself, run once with a local Gradle install:

```bash
gradle wrapper
```

## Project structure

```
app/src/main/java/com/kodnex/nexwall/sample/
├── MainActivity.kt              # theme + back stack -> screens
├── data/
│   ├── Models.kt                # @Serializable API models
│   ├── NexWallApi.kt            # Retrofit service, repository, errors, quota
│   └── WallpaperSetter.kt       # download + WallpaperManager.setStream
└── ui/
    ├── WallpaperViewModel.kt    # categories, paging, sort, set wallpaper
    └── Screens.kt               # Categories, Wallpapers, Preview composables
app/build.gradle.kts             # reads NEXWALL_API_KEY into BuildConfig
gradle/libs.versions.toml        # version catalog
local.properties.example
```

## API endpoints used

Base URL: `https://nexwall.kodnextech.com/api/developer/v1`

Every request sends `Authorization: Bearer <API_KEY>` and `Accept: application/json` (added by an OkHttp interceptor).

| Endpoint | Used for |
| --- | --- |
| `GET /categories` | Category grid |
| `GET /wallpapers?page=&per_page=&category_id=&sort=&type=image` | Paginated wallpaper grid |
| `GET /categories/{categoryId}/wallpapers` | Declared in `NexWallService` for your own use |

`/wallpapers` also accepts `search` (2-100 characters, matches tags; already a parameter in `NexWallService.wallpapers`). `per_page` is 1-100 (default 50); this app uses 30. The API also has `GET /wallpapers/{id}`.

Full reference: [API docs](https://nexwall.kodnextech.com/wallpaper-api/docs) · [OpenAPI spec](https://nexwall.kodnextech.com/openapi.json) · [Sandbox](https://nexwall.kodnextech.com/wallpaper-api/sandbox)

## Rate limits & plans

| Plan | Price | Requests per day | Content |
| --- | --- | --- | --- |
| Free | Free, no credit card | 100 | Non-premium categories |
| Pro | ₹399 / $4.99 per month | 10,000 | |
| Ultra | ₹899 / $10.99 per month | 50,000 | Includes live (video) wallpapers |

- The free plan also allows up to 60 requests per minute.
- Responses include `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` and `X-Developer-Api-Plan` headers; the JSON body includes `plan` and `remaining_requests_today`.
- On **HTTP 429** the repository throws `NexWallException` with `retryAfterSeconds` from the `Retry-After` header. The UI shows it and does not retry automatically.
- State lives in the `ViewModel`, so rotating the screen or going back does not refetch.

## Production note

Values in `BuildConfig` are compiled into the APK and can be extracted by anyone who has it. That is fine for development, but for a published app put a **backend proxy** between the app and NexWall: the app calls your server, the server adds the API key from a secret and forwards the request (and can cache responses to save quota). Change `NEXWALL_BASE_URL` in `app/build.gradle.kts` to your proxy (keep the trailing `/`) and leave `NEXWALL_API_KEY` empty. With a custom base URL, `NexWallRepository` does not require a key and sends no `Authorization` header.

## Related starters

- [Flutter wallpaper app](https://github.com/kodnextechnologies/nexwall-flutter-wallpaper-app)
- [React Native / Expo wallpaper app](https://github.com/kodnextechnologies/nexwall-react-native-expo-wallpaper-app)
- [Python client and CLI with a daily wallpaper changer](https://github.com/kodnextechnologies/nexwall-python)

## License

The source code is released under the [MIT License](LICENSE).

Wallpaper images and videos returned by the API are **not** covered by the MIT license. Their use is governed by the [NexWall Developer API License](https://nexwall.kodnextech.com/wallpaper-api/license).
