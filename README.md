# [Cordova AdMob tomitank]

Based on AdMob Plus Cordova with updated SDK-s
- Android: 24.5.0
- iOS: 12.9.0

[![](https://img.shields.io/static/v1?label=Sponsor%20Me&style=for-the-badge&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86)](https://github.com/sponsors/tomitank)

## Breaking changes

- We don't use admob.banner.[eventName] instead of this use "admob.ad.size" and "admob.ad.sizechange"
- This only enabled for banner type so you can use like this:
```
const banner = new BannerAd(...);
banner.on('size', (res) => ...);
```

## Documentation

You can find the documentation [on the website](https://admob-plus.github.io/docs/cordova).

## Contributing

- Star this repository
- Open issue for feature requests
- [Sponsor this project](https://github.com/sponsors/tomitank)

## License

Cordova AdMob tomitank is [MIT licensed](../../LICENSE).
