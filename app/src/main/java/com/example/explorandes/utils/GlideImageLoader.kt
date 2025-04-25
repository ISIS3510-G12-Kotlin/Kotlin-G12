package com.example.explorandes.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.explorandes.R
import com.example.explorandes.cache.ImageCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Implementación para cargar imágenes usando Glide con soporte para caché.
 * Esta clase implementa la estrategia de memoria local para imágenes.
 */
class GlideImageLoader(private val context: Context) {

    private val imageCacheManager = ImageCacheManager.getInstance(context)

    // Determina si hay conexión a Internet activa
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Carga una imagen desde la URL en un ImageView, con configuración para usar caché
     */
    fun loadImage(url: String?, imageView: ImageView, placeholderId: Int = R.drawable.profile_placeholder) {
        if (url == null || url.isEmpty()) {
            loadPlaceholder(imageView, placeholderId)
            return
        }

        // Configurar opciones de Glide
        val requestOptions = RequestOptions()
            .placeholder(placeholderId)
            .error(placeholderId)
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        var glideRequest: RequestBuilder<Drawable> = Glide.with(context)
            .load(url)
            .apply(requestOptions)
            .transition(DrawableTransitionOptions.withCrossFade())

        // Si no hay red, solo recuperar de caché
        if (!isNetworkAvailable()) {
            glideRequest = glideRequest.onlyRetrieveFromCache(true)
        }

        glideRequest.into(imageView)

        // Pre-cargar si es imagen pequeña
        if (isSmallImage(url)) {
            preloadImage(url)
        }
    }

    /**
     * Carga una imagen para miniaturas con estrategias para optimizar espacio
     */
    fun loadThumbnail(url: String?, imageView: ImageView, placeholderId: Int = R.drawable.profile_placeholder) {
        if (url == null || url.isEmpty()) {
            loadPlaceholder(imageView, placeholderId)
            return
        }

        val requestOptions = RequestOptions()
            .placeholder(placeholderId)
            .error(placeholderId)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(150, 150)
            .centerCrop()

        Glide.with(context)
            .load(url)
            .apply(requestOptions)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
    }

    /**
     * Precargar una imagen en caché para uso futuro sin conexión (corrutina)
     */
    private fun preloadImage(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                imageCacheManager.preloadImage(url)
            } catch (e: Exception) {
                // Ignorar errores de precarga
            }
        }
    }

    /**
     * Determina si una imagen es considerada "pequeña" basado en su URL
     */
    private fun isSmallImage(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("thumbnail") ||
               lowerUrl.contains("icon") ||
               lowerUrl.contains("small") ||
               lowerUrl.contains("avatar") ||
               lowerUrl.endsWith(".ico")
    }

    /**
     * Carga un placeholder en el ImageView
     */
    private fun loadPlaceholder(imageView: ImageView, placeholderId: Int) {
        Glide.with(context)
            .load(placeholderId)
            .into(imageView)
    }

    /**
     * Limpia la caché de imágenes (útil para liberar espacio)
     */
    fun clearCache() {
        Glide.get(context).clearMemory()

        CoroutineScope(Dispatchers.IO).launch {
            Glide.get(context).clearDiskCache()
            imageCacheManager.clearCache()
        }
    }
}
