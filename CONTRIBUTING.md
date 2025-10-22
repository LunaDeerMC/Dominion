# Contributing to Dominion

Thank you for considering contributing to Dominion! We welcome contributions from everyone. By participating in this
project, you agree to abide by our code of conduct.

## How to Build

To build the project locally, you need to have Java Development Kit (JDK) 21 or higher and Gradle installed on your
machine.

1. Clone the repository:

    ```bash
    git clone https://github.com/LunaDeerMC/Dominion.git
    cd Dominion
    git submodule update --init --recursive
    ```

2. Build the project using Gradle:

    ```bash
    ./gradlew shadowJar
    ```
   The compiled plugin JAR file will be located in the `build/libs` directory.

## How to Contribute

### 🪲Reporting Bugs

If you find a bug, please report it by opening an issue on our GitHub repository. Include as much detail as possible,
including steps to reproduce the issue, the expected result, and the actual result.

### 💡Suggesting Enhancements

We welcome suggestions for new features or improvements. Please open an issue on our GitHub repository and describe the
enhancement you would like to see, why you think it would be useful, and any other relevant information.

### 🔧Creating Pull Requests

If you would like to contribute code to the project, please follow these steps:

1. Fork the repository & clone the repository to your local.
2. Create a new branch for your changes.
3. Make your changes.
4. Commit & Push your changes to your fork.
5. Open a pull request, wait for your pull request to be reviewed and merged.

Please ensure that your code adheres to the project's coding standards.

- Follow the existing code style and conventions.
- Write clear and concise commit messages.
- Update documentation as needed.

**NOTE:** Generally, we do not accept pull requests that add new features or change existing functionality or modify API
repo without prior discussion. Please open an issue first to discuss your proposed changes before submitting a pull
request.

### 🌐Translating

#### **Translate Plugin Messages**

Since the plugin messages are translated by AI, there must be some mistakes in the translation. If you find any mistakes
(or inappropriate translations), there are two ways to help us improve the translation:

1. Fork the repository & clone the repository to your local.
2. Create a new branch for your translation.
3. Translate the plugin messages.
4. Commit & Push your changes to your fork.
5. Open a pull request, wait for your pull request to be reviewed and merged.

Plugin messages are located in the `languages` directory. Translate the files in the language you want to

- If the language you want to translate to does not exist, create a new file with the language code (e.g., `zh-cn.yml`
  for Simplified Chinese).
- If the translation is not up-to-date, please update it from the `zh-cn.yml` file.
- Don't forget to leave your name in the header-comment of the file.

> Plugin messages was uploaded to Crowdin too, you can directly modify the translation on
> the [Crowdin project](https://crowdin.com/project/dominion).
>
> But we **NOT RECOMMEND** this way, because the translation on Crowdin may not be the latest.
> If you insist on using Crowdin and founding contents are outdated, please inform us via issue on GitHub
> to update the contents on Crowdin.
>
> ![Crowdin](https://badges.crowdin.net/dominion/localized.svg)

#### **Translate Documentation**

1. Fork the repository & clone the repository to your local.
2. Create a new branch for your translation.
3. Translate the documentation or plugin messages.
4. Commit & Push your changes to your fork.
5. Open a pull request, wait for your pull request to be reviewed and merged.

Documentations are located in the `docs` directory. Translate the files in the language you want to contribute to.

- If the language you want to translate to does not exist, create a new directory with the language code (e.g., `zh-cn`
  for Simplified Chinese).
- If the documentation is not up-to-date, please update it from the `zh-cn` directory.
- Don't forget to leave your name and contact information in `docs/YOUR_LANGUAGE_CODE/README.md` below.

## 🫴Getting Help

If you need help, feel free to ask questions by opening an issue on our GitHub repository.

Thank you for contributing ❤!
