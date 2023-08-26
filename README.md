# LyricsGetterExt

基于 [KaguraRinko/StatusBarLyricExt](https://github.com/KaguraRinko/StatusBarLyricExt)

仅是对 LyricsGetter 进行支持，去除了系统检测。

# 原仓库 README

这个工具可以为使用 [MediaSession](https://developer.android.google.cn/reference/android/media/session/MediaSession) 的音乐播放器添加状态栏歌词功能

目前仅支持 [Flyme](https://www.flyme.com/) 和 [exTHmUI](https://www.exthmui.cn/) 的状态栏歌词功能

## 原理
- 通过 [MediaController](https://developer.android.google.cn/reference/android/media/session/MediaController) 取得当前播放的媒体信息
- 联网获取歌词后显示在状态栏上

## 使用的开源项目
- [LyricView](https://github.com/markzhai/LyricView)
