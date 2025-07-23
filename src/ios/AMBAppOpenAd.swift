import Foundation
import GoogleMobileAds

class AMBAppOpenAd: AMBAdBase, FullScreenContentDelegate {

    var mAd: AppOpenAd?

    convenience init?(_ ctx: AMBContext) {
        guard let id = ctx.optId(),
              let adUnitId = ctx.optAdUnitID()
        else {
            return nil
        }
        self.init(id: id,
                  adUnitId: adUnitId,
                  adRequest: ctx.optRequest())
    }

    deinit {
        clear()
    }

    override func isLoaded() -> Bool {
        return mAd != nil
    }

    override func load(_ ctx: AMBContext) {
        clear()

        AppOpenAd.load(
            with: self.adUnitId,
            request: adRequest,
            completionHandler: { (ad, error) in
                if error != nil {
                    self.emit(AMBEvents.adLoadFail, error!)
                    ctx.reject(error!)
                    return
                }
                ad?.fullScreenContentDelegate = self
                self.mAd = ad

                self.emit(AMBEvents.adLoad)
                ctx.resolve()
            })
    }

    override func show(_ ctx: AMBContext) {
        mAd?.present(from: AMBContext.plugin.viewController)
    }

    func adDidRecordImpression(_ ad: FullScreenPresentingAd) {
        self.emit(AMBEvents.adImpression)
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        clear()
        self.emit(AMBEvents.adShowFail, error)
    }

    func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        self.emit(AMBEvents.adShow)
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        clear()
        self.emit(AMBEvents.adDismiss)
    }

    private func clear() {
        mAd?.fullScreenContentDelegate = nil
        mAd = nil
    }
}