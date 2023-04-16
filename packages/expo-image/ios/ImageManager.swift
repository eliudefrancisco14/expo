import SDWebImage

struct ImageLoadResult {
  let image: UIImage?
  let cacheType: ImageCacheType
}

struct ImageLoadOptions {
  let cachePolicy: ImageCachePolicy
  let screenScale: Double
}

private let diskCache = DiskCache()
private let memoryCache = SDMemoryCache<NSString, UIImage>()

internal final class ImageManager {
  internal typealias LoadingTask = Task<UIImage?, Error>

  private let sdImageManager = SDWebImageManager(
    cache: SDImageCache.shared,
    loader: SDImageLoadersManager.shared
  )

  private var pendingTask: LoadingTask?
  private var pendingOperation: SDWebImageCombinedOperation?

  internal func loadImage(source: ImageSource, options: ImageLoadOptions) async -> ImageLoadResult {
    pendingTask?.cancel()

    let cacheKey = source.getCacheKey()

    // Try to load the source from caches.
    if source.isCachingAllowed, let cacheKey, !cacheKey.isEmpty {
      if options.cachePolicy.canUseMemoryCache {
        if let image = memoryCache.object(forKey: cacheKey as NSString) as? UIImage {
          // Found the image in the memory.
          return ImageLoadResult(image: image, cacheType: .memory)
        }
      }
      if options.cachePolicy.canUseDiskCache {
        if let imageData = await diskCache.query(key: cacheKey) {
          // Found the image in the disk cache.
          let image = UIImage(data: imageData)
          return ImageLoadResult(image: image, cacheType: .disk)
        }
      }
    }

    // Image not found in caches. Here we initiate an actual loading
    // from network, file system or generating the hashed placeholder.
    let result = await sd_loadImage(source: source, options: options)

    // Save the image to caches if the load succeeded.
    if let image = result.image, source.isCachingAllowed, let cacheKey {
      if options.cachePolicy.canUseMemoryCache {
        memoryCache.setObject(image, forKey: cacheKey as NSString)
      }
      if options.cachePolicy.canUseDiskCache, result.cacheType != .disk {
        // Defer storing the image on the disk cache, so that the image can be processed and rendered earlier.
        Task {
          if let data = image.sd_imageData() {
            await diskCache.store(key: cacheKey, data: data)
          }
        }
      }
    }

    return result
  }

  internal func sd_loadImage(source: ImageSource, options: ImageLoadOptions) async -> ImageLoadResult {
    var context = SDWebImageContext()

    // Cancel currently running load requests.
//    cancelPendingOperation()

    // Modify URL request to add headers.
    if let headers = source.headers {
      context[SDWebImageContextOption.downloadRequestModifier] = SDWebImageDownloaderRequestModifier(headers: headers)
    }

    context[.cacheKeyFilter] = createCacheKeyFilter(source.cacheKey)
//    context[.imageTransformer] = createTransformPipeline()

    // Assets from the bundler have `scale` prop which needs to be passed to the context,
    // otherwise they would be saved in cache with scale = 1.0 which may result in
    // incorrectly rendered images for resize modes that don't scale (`center` and `repeat`).
    context[.imageScaleFactor] = source.scale

    context[.originalQueryCacheType] = SDImageCacheType.none.rawValue
    context[.originalStoreCacheType] = SDImageCacheType.none.rawValue
    context[.queryCacheType] = SDImageCacheType.none.rawValue
    context[.storeCacheType] = SDImageCacheType.none.rawValue

    // Some loaders (e.g. blurhash) need access to the source and the screen scale.
    context[ImageView.contextSourceKey] = source
    context[ImageView.screenScaleKey] = options.screenScale

//    onLoadStart([:])

    return await withCheckedContinuation { continuation in
      let completion: SDInternalCompletionBlock = { image, data, error, cacheType, finished, imageUrl in
        let result = ImageLoadResult(image: image, cacheType: ImageCacheType.fromSdCacheType(cacheType))
        continuation.resume(returning: result)
      }
      pendingOperation = sdImageManager.loadImage(
        with: source.uri,
        options: [],
        context: context,
        progress: nil,
        completed: completion
      )
    }
  }

  // MARK: - helpers

  private func cancelPendingOperation() {
    pendingOperation?.cancel()
    pendingOperation = nil
  }
}
