import { MobileAd, MobileAdOptions } from './shared'

export default class AppOpenAd extends MobileAd<
  MobileAdOptions
> {
  static cls = 'AppOpenAd'

  public isLoaded() {
    return super.isLoaded()
  }

  public load() {
    return super.load()
  }

  async show() {
    return super.show() as Promise<boolean>
  }
}
