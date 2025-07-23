import Foundation
import GoogleMobileAds

protocol AMBNativeAdViewProvider: NSObjectProtocol {
    func createView(_ nativeAd: NativeAd) -> UIView
    // delegate
    func didShow(_ ad: AMBNativeAd)
    func didHide(_ ad: AMBNativeAd)
}

extension AMBNativeAdViewProvider {
    func didShow(_ ad: AMBNativeAd) {}
    func didHide(_ ad: AMBNativeAd) {}
}

class AMBNativeAd: AMBAdBase, NativeAdLoaderDelegate, NativeAdDelegate {
    static var providers = [String: AMBNativeAdViewProvider]()

    var mLoader: AdLoader!
    let viewProvider: AMBNativeAdViewProvider
    var mAd: NativeAd?
    var ctxLoad: AMBContext?

    lazy var view: UIView = {
        return viewProvider.createView(mAd!)
    }()

    init(id: String, adUnitId: String, adRequest: Request, viewProvider: AMBNativeAdViewProvider) {
        self.viewProvider = viewProvider

        super.init(id: id, adUnitId: adUnitId, adRequest: adRequest)

        mLoader = AdLoader(adUnitID: adUnitId, rootViewController: plugin.viewController,
                              adTypes: [.native], options: nil)
        mLoader.delegate = self
    }

    convenience init?(_ ctx: AMBContext) {
        let view = ctx.optString("view") ?? "default"
        guard let id = ctx.optId(),
              let adUnitId = ctx.optAdUnitID(),
              let viewProvider = Self.providers[view]
        else {
            return nil
        }
        self.init(id: id,
                  adUnitId: adUnitId,
                  adRequest: ctx.optRequest(),
                  viewProvider: viewProvider)
    }

    override func load(_ ctx: AMBContext) {
        ctxLoad = ctx
        mLoader.load(adRequest)
    }

    override func isLoaded() -> Bool {
        if mLoader == nil {
            return false
        }
        return !mLoader.isLoading
    }

    override func show(_ ctx: AMBContext) {
        if let x = ctx.opt("x") as? Double,
           let y = ctx.opt("y") as? Double,
           let w = ctx.opt("width") as? Double,
           let h = ctx.opt("height") as? Double {
            view.frame = CGRect(x: x, y: y, width: w, height: h)
        }

        if let rootView = plugin.viewController.view, view.superview != rootView {
            rootView.addSubview(view)
        }
        view.isHidden = false

        viewProvider.didShow(self)
    }

    override func hide(_ ctx: AMBContext) {
        view.isHidden = true
        viewProvider.didHide(self)
        ctx.resolve()
    }

    func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) {
        mAd = nativeAd
        nativeAd.delegate = self
        self.emit(AMBEvents.adLoad)
        if !adLoader.isLoading {
            ctxLoad?.resolve()
            ctxLoad = nil
        }
    }

    func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) {
        self.emit(AMBEvents.adLoadFail, error)
        if !adLoader.isLoading {
            ctxLoad?.reject(error)
            ctxLoad = nil
        }
    }

    func nativeAdDidRecordImpression(_ nativeAd: NativeAd) {
        self.emit(AMBEvents.adImpression, nativeAd)
    }

    func nativeAdDidRecordClick(_ nativeAd: NativeAd) {
        self.emit(AMBEvents.adClick, nativeAd)
    }

    func nativeAdWillPresentScreen(_ nativeAd: NativeAd) {
        self.emit(AMBEvents.adShow, nativeAd)
    }

    func nativeAdWillDismissScreen(_ nativeAd: NativeAd) {
        // The native ad will dismiss a full screen view.
    }

    func nativeAdDidDismissScreen(_ nativeAd: NativeAd) {
        self.emit(AMBEvents.adDismiss, nativeAd)
    }

    func nativeAdWillLeaveApplication(_ nativeAd: NativeAd) {
        // The native ad will cause the application to become inactive and
        // open a new application.
    }
}