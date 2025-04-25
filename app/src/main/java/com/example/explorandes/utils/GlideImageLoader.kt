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
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Caché en disco para todas las versiones
        
        var glideRequest: RequestBuilder<Drawable> = Glide.with(context)
            .load(url)
            .apply(requestOptions)
            .transition(DrawableTransitionOptions.withCrossFade())
        
        // Determinar política de caché según la conectividad
        if (!isNetworkAvailable()) {
            // Si no hay red, sólo usar caché
            glideRequest = glideRequest.onlyRetrieveFromCache(true)
        } 
        
        glideRequest.into(imageView)
        
        // Para imágenes pequeñas, pre-cargar en segundo plano para uso offline
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
        
        // Para miniaturas usamos caché en memoria para acceso rápido y eficiente
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
     * Precargar una imagen en caché para uso futuro sin conexión
     */
    private fun preloadImage(url: String) {
        // Usar el ImageCacheManager para guardar la imagen en segundo plano
        Thread {
            try {
                imageCacheManager.preloadImage(url)
            } catch (e: Exception) {
                // No hacer nada, la precarga es opcional
            }
        }.start()
    }
    
    /**
     * Determina si una imagen es considerada pequeña basado en su URL
     * 
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
     * Limpia la caché de imágenes 
     */
    fun clearCache() {
        Glide.get(context).clearMemory()
        
        // Limpiar caché de disco en un hilo secundario
        Thread {
            Glide.get(context).clearDiskCache()
            imageCacheManager.clearCache()
        }.start()
    }
}