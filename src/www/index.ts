import { exec } from 'cordova';
import AppOpenAd from './app-open';
import channel from 'cordova/channel';
import InterstitialAd from './interstitial';
import BannerAd, { BannerAdOptions } from './banner';
import NativeAd, { NativeAdOptions } from './native';
import RewardedAd, { RewardedAdOptions, ServerSideVerificationOptions } from './rewarded';
import RewardedInterstitialAd, { RewardedInterstitialAdOptions } from './rewarded-interstitial';
import { AdMobConfig, Events, execAsync, NativeActions, Platforms, RequestConfig, start, AdSizeType, TrackingAuthorizationStatus, MobileAd, cleanup } from './shared';

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
  exec(onMessageFromNative, console.error, 'AdMob', NativeActions.ready, []);
  channel.initializationComplete(feature);
}

export class AdMob {
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


    const feature = 'onAdMobPlusReady';
    channel.createSticky(feature);
    channel.waitForInitialization(feature);

    channel.onCordovaReady.subscribe();
  }

  public cleanup() {
    cleanup();
    channel.onCordovaReady.unsubscribe()
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
    return start();
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
