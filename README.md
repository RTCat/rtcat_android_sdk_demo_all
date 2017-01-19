## 实时猫 Android SDK Demo

基于 [实时猫 Android SDK](https://shishimao.com) 开发的样例 Demo

## 说明

此样例用于演示使用`实时猫 SDK`主要功能，访问实时猫开发者文档，查看[样例代码说明](http://docs.shishimao.com/04.%20android/02.%20demo.html)

## 功能

1. 获取本地音视频
2. 互动白板
3. 多人音视频通信
4. 文字聊天
5. 日志和错误
6. 高级主题：音视频编解码
7. 高级主题：仿微信布局
8. 高级主题：发送文件
9. 高级主题：屏幕分享

## 使用

1. `git clone https://github.com/RTCat/rtcat_android_demo_all.git`
2. 通过Android Studio导入, File > Import Project ,选择项目中的build.gradle文件导入
3. (可选步骤) 在 [Config.java](/RTCat-Android-SDK/src/main/java/com/shishimao/android/sdk/Config.java) 中修改`APIKEY`、`SECRET`、`SESSION`，使用个人项目信息创建会话。获取方式请参考[实时猫开发者文档](http://docs.shishimao.com/02.%20getting-started/02.%20dashboard-and-projects.html#)
4. 从多个样例中选择一个，将其安装在手机中，进行测试
5. (可选步骤：在多人通讯时需要) 使用另一台手机，同样安装相同的样例程序，作为第二个客户端

## 注意事项

1. 屏幕分享功能，仅支持Android5.0以上系统。
2. 不同的样例程序，需要不同的系统权限。允许此应用申请的权限后方可使用。