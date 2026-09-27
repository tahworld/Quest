# Quest Windows 测试端

Windows 10/11 双击 `Quest.exe`。首次运行填写想坚持的方向，选择可用时间、精力和资源，再生成今天的一步。行动完成后写下实际结果，下次生成会参考最近五条记录。换一个会保存拒绝原因。

填写自己的 DeepSeek API Key 后使用在线模型；模型名和 HTTPS Base URL 可修改。留空时使用离线示例，示例不代表在线模型的实际质量。Key 仅保留在本次运行的内存中，关闭软件后需要重新填写。

本机记录存储于 `%LOCALAPPDATA%\Quest\quest-desktop.db`，与 Android 数据不互通。此测试端包含方向、行动生成、换一个、开始、完成及结果接续；它没有 Android 版的项目、人设、行动答疑和完整结果分析功能。

仓库的 Windows desktop trial 工作流在 Windows runner 上运行测试并打包 `Quest.exe`。需要自行运行源码时，安装 Python 3.12，然后在仓库根目录执行 `python desktop/quest.py`。运行测试：`python -m unittest discover -s desktop/tests -v`。
