# Guía paso a paso: SDK Android multi-módulo con Clean Architecture y MVVM

**Objetivo.** Mantener la funcionalidad del SDK, separar su dominio y datos, documentar cada archivo y demostrar la capa de presentación con MVVM en una app sample que consume el SDK.

## Antes de empezar

- Proyecto activo: `02-shared-android-sdk`. Abre una terminal en la raíz de esta carpeta.
- Requisitos: JDK 17 y Android SDK Platform 36. Usa el wrapper incluido (`./gradlew`, o `gradlew.bat` en Windows).
- En cada subpaso se indica proyecto, ruta exacta, explicación y código completo. Crea la ruta si falta, pega el bloque y guarda antes de pasar al siguiente archivo.
- Conserva los binarios del Gradle Wrapper. No pegues secretos ni `local.properties` en Git.
- Compatible con Events/Explore: `RedBullSdk.create()` conserva la URL por defecto y `getProducts(limit)` conserva su firma/efecto; se mantiene alias Kotlin para el paquete `com.redbull.lab.domain.Product`.

## Paso 1. Configura la build Gradle multimódulo

Aquí defines cuatro módulos: `domain` será Kotlin/JVM puro, `data` implementará la integración remota, `sdk` expondrá el entry point público y `sample` será la app demostrativa con su propia UI/MVVM. Pega primero los archivos de build para que Gradle conozca la estructura antes de añadir código.

### Archivo settings.gradle.kts

En el proyecto `02-shared-android-sdk`, en la ruta `settings.gradle.kts`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Declara la build independiente y sus cuatro módulos Gradle.

````kotlin
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); mavenLocal() }
}
rootProject.name = "shared-android-sdk"
include(":domain", ":data", ":sdk", ":sample")
````

### Archivo build.gradle.kts

En el proyecto `02-shared-android-sdk`, en la ruta `build.gradle.kts`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Registra los plugins que usan los módulos sin aplicarlos al root.

````kotlin
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}
````

### Archivo gradle.properties

En el proyecto `02-shared-android-sdk`, en la ruta `gradle.properties`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Configura opciones generales de Gradle y AndroidX.

````properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
````

### Archivo gradle/wrapper/gradle-wrapper.properties

En el proyecto `02-shared-android-sdk`, en la ruta `gradle/wrapper/gradle-wrapper.properties`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Fija la distribución de Gradle 8.13 que descarga y usa el wrapper del proyecto.

````properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
````

### Archivo gradle/libs.versions.toml

En el proyecto `02-shared-android-sdk`, en la ruta `gradle/libs.versions.toml`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Centraliza versiones compatibles de plugins y bibliotecas.

````toml
[versions]
agp = "8.13.2"
kotlin = "2.3.21"
retrofit = "3.0.0"
moshi = "1.15.2"
coroutines = "1.11.0"
activity = "1.13.0"
lifecycle = "2.11.0"

[plugins]
android-library = { id = "com.android.library", version.ref = "agp" }
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
maven-publish = { id = "maven-publish" }

[libraries]
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-moshi = { module = "com.squareup.retrofit2:converter-moshi", version.ref = "retrofit" }
moshi-kotlin = { module = "com.squareup.moshi:moshi-kotlin", version.ref = "moshi" }
coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
activity = { module = "androidx.activity:activity", version.ref = "activity" }
lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
lifecycle-runtime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
junit = { module = "junit:junit", version = "4.13.2" }
````

### Archivos buildSrc para coordenadas y versión del SDK

En el proyecto `02-shared-android-sdk`, crea primero `buildSrc/build.gradle.kts` y luego `buildSrc/src/main/kotlin/LibraryCoordinates.kt`. `buildSrc` es código de build compartido automáticamente por todos los scripts de Gradle de este repositorio. Así el group, los nombres de los tres artefactos y la versión local por defecto se mantienen en un solo lugar.

**Archivo `buildSrc/build.gradle.kts`**

````kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}
````

**Archivo `buildSrc/src/main/kotlin/LibraryCoordinates.kt`**

````kotlin
package com.redbull.lab.build

/** Coordinates shared by every published artifact in this SDK repository. */
object LibraryCoordinates {
    const val GROUP_ID = "com.redbull.lab"
    const val ANDROID_SDK_ARTIFACT_ID = "android-sdk"
    const val ANDROID_DATA_ARTIFACT_ID = "android-data"
    const val ANDROID_DOMAIN_ARTIFACT_ID = "android-domain"

    /** Local and Maven Local default; CI may override it with -PreleaseVersion. */
    const val VERSION_NAME = "0.2.0"
}
````

Para una nueva versión local cambia solamente `VERSION_NAME`. Los scripts de publicación de `domain`, `data` y `sdk` usan ese default; si Gradle recibe `-PreleaseVersion=...`, ese valor tiene precedencia. El workflow de GitHub deriva ese parámetro del tag `vX.Y.Z`, de modo que los tres artefactos reciben exactamente la misma versión del tag.

### Archivo domain/build.gradle.kts

En el proyecto `02-shared-android-sdk`, en la ruta `domain/build.gradle.kts`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Configura domain como biblioteca Kotlin/JVM pura y publica su JAR.

````kotlin
import com.redbull.lab.build.LibraryCoordinates

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

kotlin { jvmToolchain(17) }

java { withSourcesJar() }

dependencies {
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            groupId = LibraryCoordinates.GROUP_ID
            artifactId = LibraryCoordinates.ANDROID_DOMAIN_ARTIFACT_ID
            version = providers.gradleProperty("releaseVersion").orElse(LibraryCoordinates.VERSION_NAME).get()
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPOSITORY"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}
````

### Archivo data/build.gradle.kts

En el proyecto `02-shared-android-sdk`, en la ruta `data/build.gradle.kts`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Configura Android data y sus dependencias Retrofit/Moshi y contrato domain.

````kotlin
import com.redbull.lab.build.LibraryCoordinates

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.redbull.lab.data"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing { singleVariant("release") { withSourcesJar() } }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":domain"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = LibraryCoordinates.GROUP_ID
            artifactId = LibraryCoordinates.ANDROID_DATA_ARTIFACT_ID
            version = providers.gradleProperty("releaseVersion").orElse(LibraryCoordinates.VERSION_NAME).get()
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPOSITORY"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("release") {
        from(components["release"])
    }
}
````

### Archivo sdk/build.gradle.kts

En el proyecto `02-shared-android-sdk`, en la ruta `sdk/build.gradle.kts`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Configura el AAR de la fachada pública y sus módulos internos.

````kotlin
import com.redbull.lab.build.LibraryCoordinates

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.redbull.lab.sdk"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing { singleVariant("release") { withSourcesJar() } }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":domain"))
    implementation(project(":data"))
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = LibraryCoordinates.GROUP_ID
            artifactId = LibraryCoordinates.ANDROID_SDK_ARTIFACT_ID
            version = providers.gradleProperty("releaseVersion").orElse(LibraryCoordinates.VERSION_NAME).get()
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPOSITORY"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("release") {
        from(components["release"])
    }
}
````

### Archivo sample/build.gradle.kts

En el proyecto `02-shared-android-sdk`, en la ruta `sample/build.gradle.kts`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Configura la app Android que sirve como ejemplo ejecutable de MVVM.

````kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.redbull.lab.sample"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.redbull.lab.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":sdk"))
    implementation(libs.activity)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.coroutines.android)
}
````

### Archivo data/src/main/AndroidManifest.xml

En el proyecto `02-shared-android-sdk`, en la ruta `data/src/main/AndroidManifest.xml`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Manifiesto mínimo para el módulo Android de data.

````xml
<manifest />
````

### Archivo sdk/src/main/AndroidManifest.xml

En el proyecto `02-shared-android-sdk`, en la ruta `sdk/src/main/AndroidManifest.xml`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Manifiesto mínimo para el módulo Android del SDK.

````xml
<manifest />
````

### Archivo sample/src/main/AndroidManifest.xml

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/AndroidManifest.xml`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Declara permiso de internet y la Activity de inicio de la app sample.

````xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:name=".SampleApplication"
        android:label="SDK MVVM Sample"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".SampleActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
````

### Archivo sample/src/main/res/layout/activity_sample.xml

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/res/layout/activity_sample.xml`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Define la UI de muestra con indicador de carga y área de contenido.

````xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp">
    <ProgressBar
        android:id="@+id/loading"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
    <TextView
        android:id="@+id/content"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingTop="16dp"
        android:textSize="16sp" />
</LinearLayout>
````

**Al completar este paso, ejecuta desde la raíz de `02-shared-android-sdk`:**

```bash
./gradlew projects
```

## Paso 2. Crea la capa domain

La capa interna define el modelo de negocio, el contrato del repositorio y el caso de uso. No conoce Retrofit, Android ni la UI. El alias `Product` conserva el paquete Kotlin que ya usaban Events y Explore mientras el nuevo código se organiza en `domain.model`.

### Archivo domain/src/main/java/com/redbull/lab/domain/model/Product.kt

En el proyecto `02-shared-android-sdk`, en la ruta `domain/src/main/java/com/redbull/lab/domain/model/Product.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Modelo de dominio estable que no depende de JSON ni HTTP.

````kotlin
package com.redbull.lab.domain.model

/** Product model exposed by the SDK, independent from any HTTP payload. */
data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val thumbnail: String,
)
````

### Archivo domain/src/main/java/com/redbull/lab/domain/Product.kt

En el proyecto `02-shared-android-sdk`, en la ruta `domain/src/main/java/com/redbull/lab/domain/Product.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Alias de compatibilidad para conservar el paquete Kotlin usado por las apps existentes.

````kotlin
package com.redbull.lab.domain

/**
 * Kotlin compatibility alias for the original public product package.
 * New SDK code should import `com.redbull.lab.domain.model.Product`.
 */
typealias Product = com.redbull.lab.domain.model.Product
````

### Archivo domain/src/main/java/com/redbull/lab/domain/repository/ProductRepository.kt

En el proyecto `02-shared-android-sdk`, en la ruta `domain/src/main/java/com/redbull/lab/domain/repository/ProductRepository.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Abstracción del dominio para recuperar productos.

````kotlin
package com.redbull.lab.domain.repository

import com.redbull.lab.domain.model.Product

/** Contract for loading products without exposing the network implementation. */
interface ProductRepository {
    suspend fun getProducts(limit: Int = 20): List<Product>
}
````

### Archivo domain/src/main/java/com/redbull/lab/domain/usecase/GetProductsUseCase.kt

En el proyecto `02-shared-android-sdk`, en la ruta `domain/src/main/java/com/redbull/lab/domain/usecase/GetProductsUseCase.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Caso de uso que coordina la operación de dominio.

````kotlin
package com.redbull.lab.domain.usecase

import com.redbull.lab.domain.model.Product
import com.redbull.lab.domain.repository.ProductRepository

/** Retrieves products through the domain repository contract. */
class GetProductsUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(limit: Int = 20): List<Product> = repository.getProducts(limit)
}
````

### Archivo domain/src/test/java/com/redbull/lab/domain/usecase/GetProductsUseCaseTest.kt

En el proyecto `02-shared-android-sdk`, en la ruta `domain/src/test/java/com/redbull/lab/domain/usecase/GetProductsUseCaseTest.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Prueba el caso de uso con un repositorio falso.

````kotlin
package com.redbull.lab.domain.usecase

import com.redbull.lab.domain.model.Product
import com.redbull.lab.domain.repository.ProductRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProductsUseCaseTest {
    @Test
    fun returnsProductsFromRepository() = runBlocking {
        val expected = listOf(Product(1, "Sample", "Demo", 4.5, "image"))
        val repository = object : ProductRepository {
            override suspend fun getProducts(limit: Int) = expected
        }

        assertEquals(expected, GetProductsUseCase(repository)(10))
    }
}
````

**Al completar este paso, ejecuta desde la raíz de `02-shared-android-sdk`:**

```bash
./gradlew :domain:test
```

## Paso 3. Implementa la capa data

La API Retrofit y sus DTO viven en `remote`; `mapper` convierte el formato remoto al modelo de domain; `repository` implementa el contrato del dominio; `di` construye la implementación. El módulo no obliga a los consumidores a adoptar Hilt/Dagger: el SDK publicará una fábrica propia y cada app puede integrarla con su contenedor.

### Archivo data/src/main/java/com/redbull/lab/data/remote/api/ProductApi.kt

En el proyecto `02-shared-android-sdk`, en la ruta `data/src/main/java/com/redbull/lab/data/remote/api/ProductApi.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Declara el endpoint REST con Retrofit.

````kotlin
package com.redbull.lab.data.remote.api

import com.redbull.lab.data.remote.dto.ProductResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/** Retrofit contract for the external product endpoint. */
internal interface ProductApi {
    @GET("products")
    suspend fun getProducts(@Query("limit") limit: Int): ProductResponseDto
}
````

### Archivo data/src/main/java/com/redbull/lab/data/remote/dto/ProductDto.kt

En el proyecto `02-shared-android-sdk`, en la ruta `data/src/main/java/com/redbull/lab/data/remote/dto/ProductDto.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Representa cada objeto recibido desde la API remota.

````kotlin
package com.redbull.lab.data.remote.dto

/** Wire-format product received from DummyJSON. */
data class ProductDto(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val thumbnail: String,
)
````

### Archivo data/src/main/java/com/redbull/lab/data/remote/dto/ProductResponseDto.kt

En el proyecto `02-shared-android-sdk`, en la ruta `data/src/main/java/com/redbull/lab/data/remote/dto/ProductResponseDto.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Representa el envoltorio JSON de la lista de productos.

````kotlin
package com.redbull.lab.data.remote.dto

/** Envelope returned by the DummyJSON products endpoint. */
data class ProductResponseDto(val products: List<ProductDto> = emptyList())
````

### Archivo data/src/main/java/com/redbull/lab/data/mapper/ProductMapper.kt

En el proyecto `02-shared-android-sdk`, en la ruta `data/src/main/java/com/redbull/lab/data/mapper/ProductMapper.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Convierte el DTO remoto al modelo de dominio.

````kotlin
package com.redbull.lab.data.mapper

import com.redbull.lab.data.remote.dto.ProductDto
import com.redbull.lab.domain.model.Product

/** Converts the external wire model into the stable domain model. */
internal fun ProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    description = description,
    price = price,
    thumbnail = thumbnail,
)
````

### Archivo data/src/main/java/com/redbull/lab/data/repository/RetrofitProductRepository.kt

En el proyecto `02-shared-android-sdk`, en la ruta `data/src/main/java/com/redbull/lab/data/repository/RetrofitProductRepository.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Implementa el contrato domain usando Retrofit, aplica límite seguro y mapea los resultados.

````kotlin
package com.redbull.lab.data.repository

import com.redbull.lab.data.mapper.toDomain
import com.redbull.lab.data.remote.api.ProductApi
import com.redbull.lab.domain.model.Product
import com.redbull.lab.domain.repository.ProductRepository

/** Implements the domain repository using the Retrofit API. */
internal class RetrofitProductRepository(private val api: ProductApi) : ProductRepository {
    override suspend fun getProducts(limit: Int): List<Product> =
        api.getProducts(limit.coerceIn(1, 100)).products.map { it.toDomain() }
}
````

### Archivo data/src/main/java/com/redbull/lab/data/di/RemoteDataFactory.kt

En el proyecto `02-shared-android-sdk`, en la ruta `data/src/main/java/com/redbull/lab/data/di/RemoteDataFactory.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Construye Retrofit/Moshi y el repositorio concreto sin acoplar el SDK a un DI externo.

````kotlin
package com.redbull.lab.data.di

import com.redbull.lab.data.remote.api.ProductApi
import com.redbull.lab.data.repository.RetrofitProductRepository
import com.redbull.lab.domain.repository.ProductRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Constructs the remote data graph across Gradle modules without forcing consumers to depend on DI frameworks. This wiring API is not part of the supported SDK facade. */
object RemoteDataFactory {
    fun createProductRepository(baseUrl: String): ProductRepository {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ProductApi::class.java)
        return RetrofitProductRepository(api)
    }
}
````

### Archivo data/src/test/java/com/redbull/lab/data/mapper/ProductMapperTest.kt

En el proyecto `02-shared-android-sdk`, en la ruta `data/src/test/java/com/redbull/lab/data/mapper/ProductMapperTest.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Prueba la transformación DTO a modelo de domain.

````kotlin
package com.redbull.lab.data.mapper

import com.redbull.lab.data.remote.dto.ProductDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductMapperTest {
    @Test
    fun mapsRemoteDtoToDomainModel() {
        val product = ProductDto(7, "Demo", "Description", 12.0, "thumb").toDomain()

        assertEquals(7, product.id)
        assertEquals("Demo", product.title)
        assertEquals(12.0, product.price, 0.0)
    }
}
````

**Al completar este paso, ejecuta desde la raíz de `02-shared-android-sdk`:**

```bash
./gradlew :data:test
```

## Paso 4. Expón el SDK sin filtrar detalles internos

`RedBullSdk` conserva la API existente: `create(baseUrl)` y `getProducts(limit)`. Mantiene DummyJSON como host por defecto, exige HTTPS y limita `limit` al rango 1–100 en el repositorio. Las apps no reciben tipos Retrofit ni DTOs.

### Archivo sdk/src/main/java/com/redbull/lab/sdk/RedBullSdk.kt

En el proyecto `02-shared-android-sdk`, en la ruta `sdk/src/main/java/com/redbull/lab/sdk/RedBullSdk.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Punto de entrada público que conserva la API y el comportamiento actuales.

````kotlin
package com.redbull.lab.sdk

import com.redbull.lab.data.di.RemoteDataFactory
import com.redbull.lab.domain.model.Product
import com.redbull.lab.domain.usecase.GetProductsUseCase

/** Public entry point for applications consuming the shared Android platform SDK. */
class RedBullSdk private constructor(private val getProductsUseCase: GetProductsUseCase) {
    /** Returns normalized products from the configured API. */
    suspend fun getProducts(limit: Int = 20): List<Product> = getProductsUseCase(limit)

    companion object {
        /** Creates an SDK instance. The base URL must end with a slash. */
        @JvmStatic
        fun create(baseUrl: String = "https://dummyjson.com/"): RedBullSdk {
            require(baseUrl.startsWith("https://")) { "The SDK requires HTTPS" }
            return RedBullSdk(GetProductsUseCase(RemoteDataFactory.createProductRepository(baseUrl)))
        }
    }
}
````

**Al completar este paso, ejecuta desde la raíz de `02-shared-android-sdk`:**

```bash
./gradlew :sdk:assembleRelease
```

## Paso 5. Sigue MVVM y Clean Architecture en la app sample

El SDK de producción no dibuja pantallas, por lo que poner un ViewModel dentro de su runtime mezclaría responsabilidades. El módulo `sample` contiene la capa de presentation y demuestra MVVM: Activity observa estado, ViewModel coordina el caso de uso, domain define su contrato, data adapta el SDK y Application arma el grafo de dependencias.

### Archivo sample/src/main/java/com/redbull/lab/sample/domain/repository/SampleProductRepository.kt

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/java/com/redbull/lab/sample/domain/repository/SampleProductRepository.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Contrato app-facing para que la capa domain del sample no dependa del SDK.

````kotlin
package com.redbull.lab.sample.domain.repository

import com.redbull.lab.domain.model.Product

/** Sample-app boundary for product loading, independent from the SDK implementation. */
interface SampleProductRepository {
    suspend fun loadProducts(): List<Product>
}
````

### Archivo sample/src/main/java/com/redbull/lab/sample/domain/usecase/LoadProductsUseCase.kt

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/java/com/redbull/lab/sample/domain/usecase/LoadProductsUseCase.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Caso de uso de la app sample.

````kotlin
package com.redbull.lab.sample.domain.usecase

import com.redbull.lab.domain.model.Product
import com.redbull.lab.sample.domain.repository.SampleProductRepository

/** Requests the sample catalog through its own domain contract. */
class LoadProductsUseCase(private val repository: SampleProductRepository) {
    suspend operator fun invoke(): List<Product> = repository.loadProducts()
}
````

### Archivo sample/src/main/java/com/redbull/lab/sample/data/SdkSampleProductRepository.kt

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/java/com/redbull/lab/sample/data/SdkSampleProductRepository.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Adapta RedBullSdk al contrato propio de la app sample.

````kotlin
package com.redbull.lab.sample.data

import com.redbull.lab.domain.model.Product
import com.redbull.lab.sample.domain.repository.SampleProductRepository
import com.redbull.lab.sdk.RedBullSdk

/** Adapts the published SDK facade to the sample app's domain interface. */
class SdkSampleProductRepository(private val sdk: RedBullSdk) : SampleProductRepository {
    override suspend fun loadProducts(): List<Product> = sdk.getProducts()
}
````

### Archivo sample/src/main/java/com/redbull/lab/sample/presentation/SampleUiState.kt

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/java/com/redbull/lab/sample/presentation/SampleUiState.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Estado inmutable de carga, productos y error.

````kotlin
package com.redbull.lab.sample.presentation

import com.redbull.lab.domain.model.Product

/** Immutable screen state rendered by the sample Activity. */
data class SampleUiState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
)
````

### Archivo sample/src/main/java/com/redbull/lab/sample/presentation/SampleViewModel.kt

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/java/com/redbull/lab/sample/presentation/SampleViewModel.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** ViewModel MVVM que ejecuta el caso de uso y expone StateFlow.

````kotlin
package com.redbull.lab.sample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redbull.lab.sample.domain.usecase.LoadProductsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loads catalog data and exposes a lifecycle-safe UI state to the Activity. */
class SampleViewModel(private val loadProducts: LoadProductsUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = _uiState.asStateFlow()

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.value = SampleUiState(isLoading = true)
            try {
                _uiState.value = SampleUiState(isLoading = false, products = loadProducts())
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.value = SampleUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load products.",
                )
            }
        }
    }
}
````

### Archivo sample/src/main/java/com/redbull/lab/sample/presentation/SampleViewModelFactory.kt

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/java/com/redbull/lab/sample/presentation/SampleViewModelFactory.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Construye el ViewModel a partir de las dependencias del sample.

````kotlin
package com.redbull.lab.sample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.redbull.lab.sample.domain.usecase.LoadProductsUseCase

/** Creates the sample ViewModel from the app's explicit dependency graph. */
class SampleViewModelFactory(private val loadProducts: LoadProductsUseCase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SampleViewModel::class.java))
        return SampleViewModel(loadProducts) as T
    }
}
````

### Archivo sample/src/main/java/com/redbull/lab/sample/SampleApplication.kt

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/java/com/redbull/lab/sample/SampleApplication.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Composition root: crea SDK, adaptador, caso de uso y factory.

````kotlin
package com.redbull.lab.sample

import android.app.Application
import com.redbull.lab.sample.data.SdkSampleProductRepository
import com.redbull.lab.sample.domain.usecase.LoadProductsUseCase
import com.redbull.lab.sample.presentation.SampleViewModelFactory
import com.redbull.lab.sdk.RedBullSdk

/** Composition root for the sample app's manual dependency graph. */
class SampleApplication : Application() {
    val viewModelFactory: SampleViewModelFactory by lazy {
        val sdk = RedBullSdk.create()
        val repository = SdkSampleProductRepository(sdk)
        SampleViewModelFactory(LoadProductsUseCase(repository))
    }
}
````

### Archivo sample/src/main/java/com/redbull/lab/sample/SampleActivity.kt

En el proyecto `02-shared-android-sdk`, en la ruta `sample/src/main/java/com/redbull/lab/sample/SampleActivity.kt`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Observa el StateFlow respetando el ciclo de vida y renderiza los estados.

````kotlin
package com.redbull.lab.sample

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import com.redbull.lab.sample.presentation.SampleViewModel
import kotlinx.coroutines.launch

/** Displays the SDK-backed catalog while leaving business work in the ViewModel. */
class SampleActivity : ComponentActivity() {
    private val viewModel: SampleViewModel by lazy {
        ViewModelProvider(this, (application as SampleApplication).viewModelFactory)
            .get(SampleViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        val progress = findViewById<ProgressBar>(R.id.loading)
        val content = findViewById<TextView>(R.id.content)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progress.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
                    content.text = when {
                        state.errorMessage != null -> state.errorMessage
                        state.products.isEmpty() && state.isLoading -> "Loading products…"
                        state.products.isEmpty() -> "No products returned."
                        else -> state.products.joinToString("\n\n") { "${it.title} — $${it.price}" }
                    }
                }
            }
        }
    }
}
````

**Al completar este paso, ejecuta desde la raíz de `02-shared-android-sdk`:**

```bash
./gradlew :sample:assembleDebug
```

## Paso 6. Documenta el contrato multiplataforma

`contracts/openapi.yaml` es el esquema neutral que backend, Android, iOS y web pueden revisar o usar para generar clientes. Es una propuesta de contrato; el laboratorio adapta DummyJSON internamente y no afirma que DummyJSON implemente esa ruta.

### Archivo contracts/openapi.yaml

En el proyecto `02-shared-android-sdk`, en la ruta `contracts/openapi.yaml`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Contrato de API neutral para equipos y clientes multiplataforma.

````yaml
openapi: 3.0.3
info:
  title: Shared Product Catalog API
  version: 1.0.0
  description: >-
    Stable cross-client contract for the sample catalog. Android, iOS, web, and
    backend teams can generate or implement clients from this schema. The Android
    lab currently adapts DummyJSON into this contract's domain model.
servers:
  - url: https://api.example.com
    description: Replace with the team's API gateway URL.
paths:
  /v1/products:
    get:
      operationId: listProducts
      summary: List products using offset pagination
      parameters:
        - name: limit
          in: query
          required: false
          description: Maximum number of items to return.
          schema:
            type: integer
            minimum: 1
            maximum: 100
            default: 20
        - name: skip
          in: query
          required: false
          description: Number of items to skip before returning results.
          schema:
            type: integer
            minimum: 0
            default: 0
      responses:
        '200':
          description: Page of products.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductPage'
        '400':
          description: Invalid pagination parameters.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiError'
        '500':
          description: Unexpected server error.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiError'
components:
  schemas:
    ProductPage:
      type: object
      required: [items, total, limit, skip]
      properties:
        items:
          type: array
          items:
            $ref: '#/components/schemas/Product'
        total:
          type: integer
          minimum: 0
        limit:
          type: integer
          minimum: 1
        skip:
          type: integer
          minimum: 0
    Product:
      type: object
      required: [id, title, description, price]
      properties:
        id:
          type: integer
          format: int64
        title:
          type: string
          minLength: 1
        description:
          type: string
        price:
          type: number
          format: double
          minimum: 0
        thumbnail:
          type: string
          format: uri
          nullable: true
        category:
          type: string
          nullable: true
    ApiError:
      type: object
      required: [code, message]
      properties:
        code:
          type: string
        message:
          type: string
        traceId:
          type: string
          nullable: true
````

### Archivo contracts/README.md

En el proyecto `02-shared-android-sdk`, en la ruta `contracts/README.md`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Instrucciones de mantenimiento del esquema OpenAPI.

````markdown
# Contrato multiplataforma

`openapi.yaml` describe un contrato HTTP neutral para catálogo. Sirve de referencia compartida entre Android, iOS, web y backend. En el laboratorio, el SDK Retrofit habla con DummyJSON y adapta su respuesta al modelo de dominio Android; el YAML propone el contrato estable que un backend propio podría ofrecer.

## Cómo usar el contrato en un equipo

1. El equipo acuerda rutas, nombres, paginación, campos requeridos/nullable, formatos, errores y reglas de compatibilidad en esta especificación.
2. Backend revisa que los endpoints implementen el contrato. Android e iOS pueden generar interfaces/modelos con una herramienta compatible con OpenAPI o implementar sus propios DTOs.
3. Cada plataforma mantiene un mapper desde su DTO de transporte a su modelo de dominio. No se comparte el DTO de Retrofit como modelo de UI.
4. CI valida que el YAML sea válido y puede comparar cambios para detectar breaking changes antes de publicar.
5. Cambios aditivos compatibles se documentan y prueban; eliminar/renombrar campos o cambiar su tipo requiere estrategia de compatibilidad, una versión nueva o migración coordinada.

Los clientes generados no sustituyen la revisión del diseño: revisa nullability, tipos numéricos, fechas, enums y errores para que el resultado sea idiomático en Kotlin y Swift. Para producción, cambia `api.example.com`, alinea el schema con el backend real y decide autenticación, límites, retries y política de deprecación. El contrato del ejemplo no representa una API ya desplegada.

## Mapeo conceptual

| OpenAPI | Android del laboratorio | Otro cliente |
|---|---|---|
| `ProductPage` | DTO de respuesta de data | modelo de respuesta Swift/web |
| `Product` | DTO y luego `domain.Product` | DTO/modelo de dominio de esa plataforma |
| `ApiError` | error normalizado del data layer | error de aplicación específico |
| `limit`, `skip` | parámetros Retrofit | parámetros equivalentes |
````

**Al completar este paso:**

Abre `contracts/openapi.yaml` y revisa que cada ruta, parámetro, respuesta y esquema esté documentado.

## Paso 7. Habilita CI y releases versionados

El workflow de CI corre pruebas y compila el sample. Al integrar en `main`, el de publicación lee la versión central, valida y publica `android-domain`, `android-data` y `android-sdk` bajo esa versión, y crea un GitHub Release con notas automáticas. Si el tag de esa versión ya existe, omite la publicación duplicada. README explica capas, ejecución, API pública, seguridad y la razón de las versiones elegidas. `LibraryCoordinates.VERSION_NAME` centraliza la versión por defecto; al publicar con un tag, el workflow pasa esa versión a los tres artefactos mediante `releaseVersion`.

### Archivo .github/workflows/android-ci.yml

En el proyecto `02-shared-android-sdk`, en la ruta `.github/workflows/android-ci.yml`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Valida el SDK y compila el sample en push y pull request. Usa `android-actions/setup-android@v4`: la versión v3 pedía por defecto el paquete SDK `tools`, que Google retiró; v4 ya no lo solicita. Luego se instalan explícitamente Android API 36 y Build Tools 36.

````yaml
name: Android CI
on: [push, pull_request]
permissions:
  contents: read
jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v4
      - name: Install Android API 36
        run: sdkmanager "platforms;android-36" "build-tools;36.0.0"
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew test assembleDebug
````

### Archivo .github/workflows/publish.yml

En el proyecto `02-shared-android-sdk`, en la ruta `.github/workflows/publish.yml`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Al integrar cambios en `main`, lee `VERSION_NAME` de `LibraryCoordinates.kt`, valida que sea `MAJOR.MINOR.PATCH`, comprueba que el tag `vVERSION` no exista, ejecuta validaciones, publica los tres artefactos con esa versión en GitHub Packages y crea el GitHub Release con notas generadas. Si ese tag ya existe, evita republicar una versión duplicada. Usa `android-actions/setup-android@v4`; v3 pedía el paquete retirado `tools`.

````yaml
name: Publish Android library
on:
  push:
    branches: [main]
permissions:
  contents: read
jobs:
  prepare:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.version.outputs.version }}
      release_needed: ${{ steps.version.outputs.release_needed }}
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: Read version and check existing release
        id: version
        run: |
          version=$(sed -nE 's/.*VERSION_NAME = "([^"]+)".*/\1/p' buildSrc/src/main/kotlin/LibraryCoordinates.kt)
          if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            echo "Invalid release version in LibraryCoordinates.kt: $version" >&2
            exit 1
          fi
          echo "version=$version" >> "$GITHUB_OUTPUT"
          if git ls-remote --exit-code --tags origin "refs/tags/v$version" >/dev/null 2>&1; then
            echo "Release v$version already exists; skipping duplicate publication."
            echo "release_needed=false" >> "$GITHUB_OUTPUT"
          else
            echo "release_needed=true" >> "$GITHUB_OUTPUT"
          fi

  verify:
    needs: prepare
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v4
      - name: Install Android API 36
        run: sdkmanager "platforms;android-36" "build-tools;36.0.0"
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew test assembleDebug

  publish:
    needs: [prepare, verify]
    if: needs.prepare.outputs.release_needed == 'true'
    runs-on: ubuntu-latest
    permissions:
      contents: write
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v4
      - name: Install Android API 36
        run: sdkmanager "platforms;android-36" "build-tools;36.0.0"
      - uses: gradle/actions/setup-gradle@v4
      - name: Publish SDK artifacts to GitHub Packages
        run: ./gradlew publishAllPublicationsToGitHubPackagesRepository -PreleaseVersion="${{ needs.prepare.outputs.version }}"
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v${{ needs.prepare.outputs.version }}
          target_commitish: ${{ github.sha }}
          generate_release_notes: true
````

### Archivo .gitignore

En el proyecto `02-shared-android-sdk`, en la ruta `.gitignore`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Excluye caches, builds y configuración local.

````text
**/.gradle/
**/build/
local.properties
*.iml
.idea/
.DS_Store
````

### Archivo README.md

En el proyecto `02-shared-android-sdk`, en la ruta `README.md`, crea el archivo si no existe y pega el siguiente código.

**Explicación:** Documenta arquitectura, forma de uso, versiones y razones de actualización.

````markdown
# Shared Android SDK

This is the second independent GitHub repository in the lab. It publishes a stable SDK facade backed by DummyJSON and contains a runnable sample app that demonstrates Clean Architecture and MVVM while consuming that facade.

## Architecture and modules

```text
:domain   pure Kotlin model, repository contract, use case
:data     Retrofit API/DTOs, mapper, repository implementation, network factory
:sdk      public RedBullSdk facade, keeps implementation details internal
:sample   runnable Android consumer demonstrating data/domain/presentation + MVVM
```

The production SDK has no screen to render, so an Android `ViewModel` does not belong in its runtime library. `:sample` supplies that presentation layer: `SampleViewModel` owns screen state and calls a use case; its Activity renders state and does not perform network or business logic. The sample's composition root wires dependencies without forcing Hilt/Dagger on SDK consumers. Production apps can still provide the SDK through their own DI setup (the Events and Explore apps use Hilt).

Dependency direction is inward: `domain` has no Android, Retrofit, or UI dependencies; `data` implements the domain contract; `sdk` exposes a small factory and delegates to the use case. The sample app consumes only the public SDK facade and has its own app-facing repository/use case so it demonstrates the same boundary pattern as the separate consumer apps.

Public API retained: `RedBullSdk.create(baseUrl)` and `getProducts(limit)` keep their behavior and defaults. The original Kotlin import `com.redbull.lab.domain.Product` remains available as a typealias; new code should use `com.redbull.lab.domain.model.Product`. The network default remains `https://dummyjson.com/`, requires HTTPS, and clamps the requested page size to 1–100.

## Build and run

Requires JDK 17 and Android SDK Platform 36. From this repo root:

```bash
./gradlew test assembleDebug
./gradlew :sample:installDebug
./gradlew publishToMavenLocal
```

The sample app launches and fetches products from DummyJSON. It needs network access, but no API key.

The published consumer coordinate remains `com.redbull.lab:android-sdk:<version>`. Publishing this repository also publishes `android-data` and the pure Kotlin `android-domain` at the same version. The API contract proposal is in `contracts/openapi.yaml`.

## Version choices and update rationale

The initial catalog was pinned to AGP 8.7.3, Kotlin 2.0.21, Retrofit 2.11.0, Moshi 1.15.1 and coroutines 1.9.0 when the scaffold was prepared. Those versions were selected as a conservative reproducible baseline, but the dependency catalog was not rechecked before delivery; that is why it had fallen behind current stable releases. Pinning versions is useful for reproducibility, but it does not replace periodic maintenance.

The current catalog uses AGP 8.13.2 + Gradle 8.13 + Kotlin 2.3.21 as a compatible conservative toolchain; Android's release notes explicitly call out Kotlin 2.3 support in AGP 8.13.2. Kotlin 2.4.21 is newer, but we kept the explicitly compatible Kotlin 2.3 / AGP 8.13 line for this lab rather than mixing an unverified compiler/Android Gradle Plugin upgrade. Retrofit is updated to stable 3.0.0, Moshi to stable 1.15.2 (not the 2.0 alpha), and coroutines to 1.11.0. The unused direct OkHttp declaration was removed; Retrofit 3.0.0 itself brings its supported OkHttp 4.12 dependency, and this SDK does not use OkHttp APIs directly.

The app repositories' Kotlin and AGP plugin pins and Gradle wrappers were also aligned to Kotlin 2.3.21 / AGP 8.13.2 / Gradle 8.13 so their compilers can consume the SDK's Kotlin metadata. UI dependency versions in those projects were not broadly upgraded as part of this SDK change.

Version references checked on 2026-11-01:

- [Android Gradle Plugin 8.13 release notes](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Android Gradle Plugin and Gradle compatibility table](https://developer.android.com/build/releases/about-agp)
- [Kotlin release history](https://kotlinlang.org/docs/releases.html)
- [Retrofit releases](https://github.com/square/retrofit/releases)
- [Moshi repository and stable coordinates](https://github.com/square/moshi)
- [Kotlin Coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases)
- [OkHttp repository](https://github.com/square/okhttp)

## Publication

Al integrar cambios en `main`, el workflow comprueba tests y build, publica `android-sdk`, `android-data` y `android-domain` en GitHub Packages con la versión de `LibraryCoordinates.kt` y crea el GitHub Release correspondiente. Concede acceso de lectura al paquete a Events y Explore; nunca guardes tokens en el repositorio.
````

**Al completar este paso, ejecuta desde la raíz de `02-shared-android-sdk`:**

```bash
./gradlew test assembleDebug publishToMavenLocal
```

## Ejecutar la app de ejemplo

Con JDK 17, SDK 36 y un emulador/dispositivo con conexión, desde el root del SDK ejecuta:

```bash
./gradlew :sample:installDebug
```

La pantalla muestra carga, error o lista de productos. El flujo es Activity → `SampleViewModel` → `LoadProductsUseCase` → `SampleProductRepository` → `RedBullSdk`.

## Probar el flujo de publicación local y actualización de apps

1. Desde este proyecto ejecuta `./gradlew publishToMavenLocal`.
2. En Events y Explore cambia la versión de `sharedSdk` a `0.2.0` (o al valor que publiques).
3. Desde cada app ejecuta `./gradlew test assembleDebug`.
4. Para GitHub Packages y el GitHub Release, cambia `VERSION_NAME` en `buildSrc/src/main/kotlin/LibraryCoordinates.kt`, integra ese cambio en `main` y deja que el workflow publique los tres artefactos y cree el release `vVERSION_NAME`. Incrementa `VERSION_NAME` antes de la siguiente publicación; si el tag ya existe, el workflow omite la duplicada.
5. Concede acceso `Read` al paquete para los repos Events y Explore. Mantén tokens locales en `~/.gradle/gradle.properties`; nunca en Git.

## Dependencias: por qué estaban antiguas y qué cambió

El primer scaffold fijó versiones de la época en que se preparó (2024) para que las cuatro builds usaran un baseline reproducible. El error fue dejar esos pins sin una revisión de mantenimiento antes de entregar el laboratorio; fijar versiones evita builds flotantes, pero no significa congelarlas indefinidamente.

El SDK ahora usa AGP 8.13.2 + Gradle 8.13 + Kotlin 2.3.21 como conjunto conservador compatible; se eligió el último patch 8.x documentado con soporte Kotlin 2.3, en vez de saltar el SDK a AGP 9 y obligar a una migración mayor de toolchains. Kotlin 2.4.21 ya existe, pero requiere revisar esa compatibilidad por separado. Retrofit 3.0.0 es la release estable actual, Moshi 1.15.2 es estable (la 2.0 encontrada sigue siendo alpha), y Coroutines queda en 1.11.0. Se eliminó OkHttp como dependencia directa porque el código no usa su API: Retrofit 3.0.0 trae la versión 4.12 que declara como soportada.

Los catálogos y wrappers de los dos consumidores también se alinearon a Kotlin 2.3.21 / AGP 8.13.2 / Gradle 8.13 para que puedan leer el metadata Kotlin del SDK.

Fuentes oficiales consultadas el 2026-11-01: [AGP 8.13 soporta Kotlin 2.3 y requiere Gradle 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [compatibilidad AGP/Gradle](https://developer.android.com/build/releases/about-agp), [Kotlin release history](https://kotlinlang.org/docs/releases.html), [Retrofit releases](https://github.com/square/retrofit/releases), [Moshi](https://github.com/square/moshi), [Coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases), [OkHttp](https://github.com/square/okhttp).

## Límites

La app sample usa `ViewModel` y `StateFlow` para enseñar MVVM; se mantiene pequeña a propósito. El SDK de producción no incluye una UI ni viewmodels, porque eso acoplaría una biblioteca de datos a la pantalla/host app. Los módulos propios se describen con KDoc y la fachada sigue siendo la API pública. Ejecuta los comandos anteriores en un entorno con Android SDK; la validación de esta actualización se ejecutó con JDK 17 y Android SDK 36.
