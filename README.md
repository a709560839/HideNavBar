# MIUI隐藏小白条 HideNavBar

[![License](https://img.shields.io/badge/License%20-GPLv3.0%20-337ab7.svg)](https://www.gnu.org/licenses/gpl-3.0.html#license-text)
[![Download](https://img.shields.io/badge/下载%20-Release%20-5ce500.svg)](https://github.com/a709560839/HideNavBar/releases)

# 如何使用：
1. 直接安装，没有图标。
2. Lsposed启用作用域：系统桌面。重启系统桌面(chimi，西米露 都可以重启系统桌面，我这没界面就不搞了，都没有就MT管理器用终端root命令: am force-stop com.miui.home)。
3. 如图打开此设置：隐藏手势提示线。**(此时在底部不用上滑，直接左右滑动可以切换app即为成功)**
<div>
<img src="https://github.com/a709560839/HideNavBar/blob/main/screenshot/screenshot1.jpg" width="266">
</div>

# 自测可行版本：
我的MIUI养老版本：官方ROM 12.5.7 (和这个应该没关系?)  
系统桌面_RELEASE-4.21.15.3207-10142038  
系统桌面_RELEASE-4.25.1.4297-04211627(这两个的中间版本应该也没问题，其他版本自测)  
  
系统桌面_RELEASE-4.39.4.5839-03301130  
系统桌面_RELEASE-4.39.12.6678-08091009(这两个的中间版本应该也没问题，其他版本自测)  

# 为啥做这个模块：
刷了各种隐藏MIUI小白条的magisk模块，基本都是基于主题隐藏的，效果很好，但是小白条只是透明了，实际屏幕底部还是有一块小白条区域存在着，导致点了没反应。在我的快手极速版APP底部4
个按钮特别明显，点的位置低了一点就不行了，如果你也有这种情况，那么这个模块可能能帮到你。本身代码实现并不难(开源代码，随便看)，主要还是理清逻辑费了点时间。这里表扬一波MIUI，APP不混淆不加固降低了难度。

# 最后：
搞机有风险，救砖模块装了吗，没装也没事，进twrp文件管理，/data/adb/modules 删对应的模块就行。  
最后的最后，各位觉得有用的，点点Star，土豪随意捐赠

<div>
<img src="https://github.com/a709560839/HideNavBar/blob/main/screenshot/alipay.jpg" width="266">
<img src="https://github.com/a709560839/HideNavBar/blob/main/screenshot/wechat.jpg" width="266" >
</div>