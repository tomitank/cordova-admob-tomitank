import { exec } from 'cordova';
import AppOpenAd from './app-open';
import channel from 'cordova/channel';
import InterstitialAd from './interstitial';
import BannerAd, { BannerAdOptions } from './banner';
import NativeAd, { NativeAdOptions } from './native';
import RewardedAd, { RewardedAdOptions, ServerSideVerificationOptions } from './rewarded';
import RewardedInterstitialAd, { RewardedInterstitialAdOptions } from './rewarded-interstitial';
import { AdMobConfig, Events, execAsync, NativeActions, Platforms, RequestConfig, AdSizeType, TrackingAuthorizationStatus, MobileAd } from './shared';

export * from './api';
export {
  AppOpenAd,
  BannerAd,
  BannerAdOptions,
  InterstitialAd,
  NativeAd,
  NativeAdOptions,
  RewardedAd,
  RewardedAdOptions,
  RewardedInterstitialAd,
  RewardedInterstitialAdOptions,
  ServerSideVerificationOptions,
};

function onMessageFromNative(event: any) {
  const { data } = event;
  if (data && data.adId) {
    data.ad = MobileAd.getAdById(data.adId);
  }
  cordova.fireDocumentEvent(event.type, data);
}

function cordovaEventListener() {
  const feature = 'onAdMobPlusReady';
  channel.createSticky(feature);
  channel.waitForInitialization(feature);
  exec(onMessageFromNative, console.error, 'AdMob', NativeActions.ready, []);
  channel.initializationComplete(feature);
}

export class AdMob {
  /**
   * this uset true only in reinit when user don't has callback
   * In normal use this need to set to true when ready event fired.
   * But in my application i set true only when i have internet connection.
   */
  public ready: boolean = false;
  public readonly AppOpenAd = AppOpenAd;
  public readonly BannerAd = BannerAd;
  public readonly InterstitialAd = InterstitialAd;
  public readonly NativeAd = NativeAd;
  public readonly RewardedAd = RewardedAd;
  public readonly RewardedInterstitialAd = RewardedInterstitialAd;
  public readonly AdSizeType = AdSizeType;
  public readonly Events = Events;
  public readonly TrackingAuthorizationStatus = TrackingAuthorizationStatus;
  constructor() {
    channel.onCordovaReady.subscribe(cordovaEventListener);
  }

  /**
   * @return true: successfully reinited the cordova bridge
   * @return false: reinit not needed bridge still connected
   * @return 'fail': ready event not fired after reinit
   * @param callback? Function
   */
  public async reinitWhenNeeded(callback?: Function) {
    try {
      await new Promise<void>((resolve, reject) => {
        const onReady = () => {
          clearTimeout(timeoutId);
          document.removeEventListener(Events.ready, onReady);
          resolve();
        };

        // wait fo ready event..
        document.addEventListener(Events.ready, onReady);

        // trigger ready event..
        exec(()=>{}, ()=>{}, 'AdMob', NativeActions.ready, []);

        // timeout when ready not fired..
        const timeoutId = setTimeout(() => { // Max 2 sec..
          document.removeEventListener(Events.ready, onReady);
          reject();
        }, 2000);
      });
      return false;
    } catch (error) { // reinit only when has error..
      return this.reinit(callback);
    }
  }

  private reinit(callback?: Function): Promise<true|'fail'> {
    this.cleanup();
    return new Promise((resolve) => {
      const onReady = () => {
        clearTimeout(timeoutId);
        document.removeEventListener(Events.ready, onReady);
        resolve(true);
        if (typeof callback === 'function') {
          callback();
        } else {
          this.ready = true;
        }
      };

      // wait fo ready event..
      document.addEventListener(Events.ready, onReady);

      // trigger ready event with subscribe..
      channel.onCordovaReady.subscribe(cordovaEventListener);

      // timeout when ready not fired..
      const timeoutId = setTimeout(() => { // Max 2 sec..
        document.removeEventListener(Events.ready, onReady);
        resolve('fail');
      }, 2000);
    });
  }

  private cleanup() {
    this.ready = false;
    MobileAd.cleanup();
    channel.onCordovaReady.unsubscribe(cordovaEventListener);
  }

  public configure(config: AdMobConfig) {
    return execAsync(NativeActions.configure, [config]);
  }

  public configRequest(requestConfig: RequestConfig) {
    return execAsync(NativeActions.configRequest, [requestConfig]);
  }

  public setAppMuted(value: boolean) {
    return execAsync(NativeActions.setAppMuted, [value]);
  }

  public setAppVolume(value: number) {
    return execAsync(NativeActions.setAppVolume, [value]);
  }

  public start() {
    return MobileAd.start();
  }

  public async requestTrackingAuthorization(): Promise<
    TrackingAuthorizationStatus | false
  > {
    if (cordova.platformId === Platforms.ios) {
      const n = await execAsync(NativeActions.requestTrackingAuthorization);
      if (n !== false) {
        return TrackingAuthorizationStatus[
          TrackingAuthorizationStatus[n as number]
        ];
      }
    }
    return false;
  }
}

declare global {
  const admob: AdMob;
}

export default AdMob;
