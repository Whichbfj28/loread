# 简介

Inoreader 是当下体验最好的国外 RSS 服务。
利用其开放的 api ，完成了该第三方客户端。
下载地址：http://fir.im/65qv


# 布局&功能

![截图](doc/overview.png)
如上图，从左至右依次对应 tab 栏的“设置，加星文章，文件夹/标签，已读未读文章”

目前实现以下几个功能：

* 处理 文章状态（已读/未读/加星）
* 文件夹与文件状态的交叉查询
* 设置 文章的保存期，及其过期后的清理
* 保存 离线状态下的一些网络请求（文章状态处理，图片下载），待有网再同步

对文章列表项的手势操作：

* 左滑是切换文章的“已读/未读”状态
* 右滑是切换文章的“加星/取消加星”状态
* 长按是“上面的文章标记为已读，下面的文章标记为已读”
* 被置为已读的文章 textColor 会暗淡。
* 手动对文章标记了状态的会在文章左下角显示一个状态 icon

另外：

* 右上角的数字为“当前目录下的未读数目”
* 设置中的“滚动标记”“保存位置”还不可用
* 文章列表上的缩略图，由于没有相关 api ，目前只是在打开正文后返回，才能获取显示

PS:

* 由于开发中本人也还在不断学习，难免有些历史遗留的错误代码，测试提示，暂时也未被清理，但不影响基本使用


# 后期规划

在解决一些离奇 bug （如果有）的前提下，应该会添加以下一些功能：

- 优雅的黑夜主题
- 文章内容页左右切换文章
- 保存最近文章的阅读进度
- 记住列表位置（在加载新数据，切换列表时）
- 被中断程序时的数据保存工作
- webview占位图（点击下图片）
- 未读数根据当前分组实时变化


# 库的使用

* OkHttp ， Gson ， Greendao ， Glide 等等
