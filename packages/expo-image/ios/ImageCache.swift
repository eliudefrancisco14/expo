import SDWebImage

internal final class ImageCache: SDImageCache {
  private let diskCacheActor = DiskCache()

  override func store(
    _ image: UIImage?,
    imageData: Data?,
    forKey key: String?,
    options: SDWebImageOptions = [],
    context: [SDWebImageContextOption : Any]?,
    cacheType: SDImageCacheType,
    completion completionBlock: SDWebImageNoParamsBlock? = nil
  ) {
    guard let key, let imageData else {
      completionBlock?()
      return
    }

    if cacheType == .disk || cacheType == .all {
      Task {
        await diskCacheActor.store(key: key, data: imageData)
        completionBlock?()
      }
      return
    }

    return super.store(
      image,
      imageData: imageData,
      forKey: key,
      options: options,
      context: context,
      cacheType: cacheType,
      completion: completionBlock
    )
  }
}
