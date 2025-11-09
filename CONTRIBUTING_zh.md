# 为 Dominion 作出贡献

感谢您考虑为 Dominion 作出贡献！我们感谢每个人的贡献。只要您参与开发本项目，您就同意了我们的代码准则。

## 如何构建

要在本地构建此项目，您需要安装 Java Development Kit (JDK) 21 或者更高版本以及 Gradle。

1. 将存储库克隆到本地：

    ```bash
    git clone https://github.com/LunaDeerMC/Dominion.git
    cd Dominion
    git submodule update --init --recursive
    ```

2. 使用 Gradle 编译此项目：

    ```bash
    ./gradlew shadowJar
    ```
    编译好的插件 JAR 文件可以在 `build/libs` 路径下找到。

## 如何贡献

### 🪲报告bug

如果您发现了一个bug，请在我们的 GitHub 存储库上打开一个 Issue 来报告。请尽可能地包含所有细节，
包括复现问题的步骤，所预期的结果以及实际结果。

### 💡功能建议

我们欢迎对新功能或者改进所提出的建议。请在我们的 GitHub 存储库上打开一个 Issue 并描述您所
希望看到的新功能，以及您认为它有用的原因，还有别的相关信息。

### 🌐翻译

#### **翻译插件消息**

自从插件消息开始由 AI 翻译，翻译就很有可能出现失误。如果您发现了任何翻译错误
（或者不当翻译），这里有两种方式来帮我们改进翻译：

1. Fork 这个存储库并克隆到本地。
2. 为您的翻译创建一个新分支。
3. 翻译插件消息。
4. 提交并推送您的更改到新分支。
5. 发起一个 Pull Request，等待您的 Pull Request 被审阅并合并。

插件消息可以在 `languages` 路径中找到，可将文件翻译到您想要的语言。

- 如果您想要翻译的语言尚不存在于项目中，用对应的语言代码创建一个文件（比如说 `zh-cn.yml` 是简体中文的文件名）
- 如果一个翻译已存在但它不是最新的, 请通过翻译 `zh-cn.yml` 文件来更新它。
- 不要忘记在文件顶部注释留下您的大名。

> 插件消息也被上传到了 Crowdin，您也可以直接在
> 这个 [Crowdin 项目](https://crowdin.com/project/dominion) 中作出翻译更改。
>
> 不过我们**不推荐**这个方法，因为 Crowdin 上的翻译可能不是最新的。
>
> ![Crowdin](https://badges.crowdin.net/dominion/localized.svg)


#### **翻译文档**

1. Fork 这个存储库并克隆到本地。
2. 为您的翻译创建一个新分支。
3. 翻译文档或者插件消息。
4. 提交并推送您的更改到新分支。
5. 发起一个 Pull Request，等待您的 Pull Request 被审阅并合并。

文档可以在 `docs` 路径中找到，可将文件翻译为您想要贡献的语言。 
- 如果您想要翻译的语言尚不存在，用对应的语言代码创建一个路径（比如说 `zh-cn` 是简体中文的文件名）
- 如果一个翻译已存在但它不是最新的, 请通过翻译 `zh-cn` 文件夹来更新它。
- 不要忘记在 `docs/YOUR_LANGUAGE_CODE/README.md` 处留下您的大名和联系方式。

## 🫴获取帮助

如果您需要帮助，请随时在我们的 GitHub 存储库上创建一个 Issue 来提出问题。

感谢您的贡献❤！
