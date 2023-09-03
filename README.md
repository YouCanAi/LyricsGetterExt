# LyricsGetterExt

基于 [KaguraRinko/StatusBarLyricExt](https://github.com/KaguraRinko/StatusBarLyricExt)

仅是对 [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter) 进行支持，去除了系统检测。

大部分时候只会在 Spotify 上测试可用性，本人没怎么学过 Android ，写着玩的。

使用 [xiaowine/Lyric-Getter-Api](https://github.com/xiaowine/Lyric-Getter-Api) 推送歌词。

请在 Xposed 管理器内对 [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter) 勾选 LyricsGetterExt ， 否则无法正常推送歌词。~~（目前本项目不在 Lyrics Getter 推荐作用域列表，请手动搜索勾选。）~~(目前已通过pr将本项目添加至 “推荐应用” 列表，在使用前注意查看是否正常勾选)

不对稳定性做任何保证，因为上面也提到，我没怎么学过 Android 开发，主要功能代码均由上游分支 [KaguraRinko/StatusBarLyricExt](https://github.com/KaguraRinko/StatusBarLyricExt) 提供，十分感谢。

若经常在某个时间段无法正常推送歌词，请检查电量，我这边快没电也掉，插上电就好了。

# 原仓库 README

这个工具可以为使用 [MediaSession](https://developer.android.google.cn/reference/android/media/session/MediaSession) 的音乐播放器添加状态栏歌词功能

~~目前仅支持 [Flyme](https://www.flyme.com/) 和 [exTHmUI](https://www.exthmui.cn/) 的状态栏歌词功能~~

## 原理
- 通过 [MediaController](https://developer.android.google.cn/reference/android/media/session/MediaController) 取得当前播放的媒体信息
- 联网获取歌词后显示在状态栏上

## 使用的开源项目
- [LyricView](https://github.com/markzhai/LyricView)
