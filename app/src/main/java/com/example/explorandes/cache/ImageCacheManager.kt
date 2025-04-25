package com.example.explorandes.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import com.bumptech.glide.load.engine.cache.DiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.security.MessageDigest

/**
 * Gestor de cache para imágenes que implementa una estrategia de dos niveles:
 * 1. Caché en memoria (LruCache) para acceso rápido a imágenes usadas recientemente
 * 2. Caché en disco para persistencia y uso sin conexión
 */
class ImageCacheManager private constructor(context: Context) {
    companion object {
        private const val TAG = "ImageCacheManager"
        private const val MEMORY_CACHE_SIZE_PERCENTAGE = 0.25 
        private const val DISK_CACHE_SIZE = 50 * 1024 * 1024 
        private const val DISK_CACHE_DIRECTORY = "image_cache"
        
        // Singleton instance
        @Volatile
        private var INSTANCE: ImageCacheManager? = null
        
        fun getInstance(context: Context): ImageCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageCacheManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
    
    // Caché en memoria usando LruCache
    private val memoryCache: LruCache<String, Bitmap>
    
    // Directorio para caché en disco
    private val diskCacheDir: File
    
    init {
        // Configurar caché en memoria
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = (maxMemory * MEMORY_CACHE_SIZE_PERCENTAGE).toInt()
        
        Log.d(TAG, "Inicializando caché de memoria con $cacheSize KB")
        
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
        
        // Configurar caché en disco
        diskCacheDir = File(context.cacheDir, DISK_CACHE_DIRECTORY)
        if (!diskCacheDir.exists()) {
            val success = diskCacheDir.mkdirs()
            if (!success) {
                Log.e(TAG, "Error al crear directorio de caché en disco")
            }
        }
        
        // Eliminar archivos viejos si el espacio usado es mayor que DISK_CACHE_SIZE
        cleanupDiskCache()
    }
    
    /**
     * Limpia archivos antiguos del caché en disco si se excede el tamaño máximo
     */
    private fun cleanupDiskCache() {
        val cacheFiles = diskCacheDir.listFiles() ?: return
        var totalSize = 0L
        
        // Ordenar archivos por tiempo de modificación (más antiguos primero)
        val sortedFiles = cacheFiles.sortedBy { it.lastModified() }
        
        // Calcular tamaño total
        sortedFiles.forEach { file ->
            totalSize += file.length()
        }
        
        // Eliminar archivos antiguos si se excede el límite
        if (totalSize > DISK_CACHE_SIZE) {
            Log.d(TAG, "La caché en disco ($totalSize bytes) excede el límite ($DISK_CACHE_SIZE bytes), limpiando...")
            
            for (file in sortedFiles) {
                val fileSize = file.length()
                val deleted = file.delete()
                
                if (deleted) {
                    totalSize -= fileSize
                    Log.d(TAG, "Eliminado archivo de caché: ${file.name}, quedan $totalSize bytes")
                    
                    if (totalSize <= DISK_CACHE_SIZE * 0.8) { // Dejamos 20% libre después de limpiar
                        break
                    }
                }
            }
        }
    }
    
    /**
     * Genera un hash MD5 de la URL para usar como nombre de archivo
     */
    private fun generateKey(url: String): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(url.toByteArray())
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Carga una imagen desde la caché (memoria o disco) o desde la red si no está en caché
     */
    suspend fun loadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        val key = generateKey(url)
        
        // 1. Intentar cargar desde la caché en memoria (más rápido)
        var bitmap = getBitmapFromMemoryCache(key)
        if (bitmap != null) {
            Log.d(TAG, "Imagen cargada desde memoria: $url")
            return@withContext bitmap
        }
        
        // 2. Intentar cargar desde la caché en disco
        bitmap = getBitmapFromDiskCache(key)
        if (bitmap != null) {
            // Guardar en memoria para futuras solicitudes
            addBitmapToMemoryCache(key, bitmap)
            Log.d(TAG, "Imagen cargada desde disco: $url")
            return@withContext bitmap
        }
        
        // 3. Cargar desde la red si no está en caché
        try {
            bitmap = downloadImage(url)
            bitmap?.let {
                // Guardar en ambas cachés
                addBitmapToMemoryCache(key, it)
                addBitmapToDiskCache(key, it)
                Log.d(TAG, "Imagen descargada y guardada en caché: $url")
            }
            return@withContext bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error al descargar imagen: $url", e)
            return@withContext null
        }
    }
    
    /**
     * Descarga una imagen desde la URL especificada
     */
    private suspend fun downloadImage(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            connection.connect()
            
            val inputStream = connection.getInputStream()
            return@withContext BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando imagen: $urlString", e)
            return@withContext null
        }
    }
    
    /**
     * Guarda un bitmap en la caché de memoria
     */
    private fun addBitmapToMemoryCache(key: String, bitmap: Bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }
    
    /**
     * Obtiene un bitmap de la caché de memoria
     */
    private fun getBitmapFromMemoryCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }
    
    /**
     * Guarda un bitmap en la caché de disco
     */
    private fun addBitmapToDiskCache(key: String, bitmap: Bitmap) {
        val file = File(diskCacheDir, key)
        if (file.exists()) {
            return
        }
        
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error guardando imagen en disco: $key", e)
        }
    }
    
    /**
     * Obtiene un bitmap de la caché de disco
     */
    private fun getBitmapFromDiskCache(key: String): Bitmap? {
        val file = File(diskCacheDir, key)
        if (!file.exists()) {
            return null
        }
        
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando imagen desde disco: $key", e)
            null
        }
    }
    
    /**
     * Elimina todas las imágenes en caché
     */
    fun clearCache() {
        clearMemoryCache()
        clearDiskCache()
    }
    
    /**
     * Elimina todas las imágenes en la caché de memoria
     */
    fun clearMemoryCache() {
        memoryCache.evictAll()
        Log.d(TAG, "Caché de memoria limpiada")
    }
    
    /**
     * Elimina todas las imágenes en la caché de disco
     */
    fun clearDiskCache() {
        diskCacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
        Log.d(TAG, "Caché de disco limpiada")
    }
    
    /**
     * Obtiene el tamaño actual de la caché en disco en bytes
     */
    fun getDiskCacheSize(): Long {
        var size = 0L
        diskCacheDir.listFiles()?.forEach { file ->
            size += file.length()
        }
        return size
    }
    
    /**
     * Obtiene el tamaño actual de la caché en memoria en KB
     */
    fun getMemoryCacheSize(): Int {
        return memoryCache.size()
    }
    
    /**
     * Precargar una imagen en caché (útil para imágenes que seguramente se usarán)
     */
    suspend fun preloadImage(url: String): Boolean = withContext(Dispatchers.IO) {
        val bitmap = loadImage(url)
        return@withContext bitmap != null
    }
}