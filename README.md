# Iceraven Browser! [![Build Status](https://travis-ci.org/fork-maintainers/iceraven-browser.svg?branch=fork)](https://travis-ci.org/fork-maintainers/iceraven-browser) ![Android build](https://github.com/fork-maintainers/iceraven-browser/workflows/Android%20build/badge.svg)

Definitely not brought to you by Mozilla!

Iceraven Browser is a web browser for Android, based on [Mozilla's Fenix version of Firefox](https://github.com/mozilla-mobile/fenix/), [GeckoView](https://mozilla.github.io/geckoview/) and [Mozilla Android Components](https://mozac.org/).

Our goal is to be a close fork of the new Firefox for Android that seeks to provide users with more options, more opportunities to customize (including a broad extension library), and more information about the pages they visit and how their browsers are interacting with those pages.

Notable features include:
  * `about:config` support
  * The ability to *attempt* to install a much longer list of add-ons than Mozilla's Fenix version of Firefox accepts. Currently the browser queries [this AMO collection](https://addons.mozilla.org/en-US/firefox/collections/16201230/What-I-want-on-Fenix/) **Most of them will not work**, because they depend on code that Mozilla is still working on writing in `android-components`, but you may attempt to install them. If you don't see an add-on you want, you can [request it](https://github.com/fork-maintainers/iceraven-browser/issues/new).
  * **No warranties or guarantees of security or updates or even stability**! Note that Iceraven Browser is a mix of "Firefox Nightly" with our own added modifications and shipped with the stable version of GeckoView engine. Hence, the browser may contain bugs introduced upstream. Binaries are currently built automatically by our Github release automation. These binaries are signed with a debug key. When we finally publish this somewhere official like F-droid, we will sign the apks with a proper key suitable for public release. Due to the current way we create the releases and sign them, you may not want to rely on such "alpha" quality software as your primary web browser, as it will have bugs. So, use this browser only if you are comfortable with these limitations/potential risks.

**Note/Disclaimer:** Iceraven Browser could not exist without the hardworking folks at the Mozilla Corporation who work on the Mozilla Android Components and Firefox projects, but it is not an official Mozilla product, and is not provided, endorsed, vetted, approved, or secured by Mozilla.

In addition, we intend to try to cut down on telemetry and proprietary code to as great of an extent as possible as long as doing so does not compromise the user experience or make the fork too hard to maintain. Right now, webelieve that no telemetry should be being sent to Mozilla anymore, but we cannot guarantee this; data may still be sent. **If you catch the app sending data to Mozilla, Adjust, Leanplum, Firebase, or any other such service, please open an issue!** Presumably data that reaches Mozilla is governed by Mozilla's privacy policy, but as Iceraven Browser is, again **not a Mozilla product**, we can make no promises.

Iceraven Browser combines the power of Fenix (of which we are a fork) and the spirit of Fennec, with a respectful nod toward the grand tradition of Netscape Navigator, from which all Gecko-based projects came, including the earliest of our predecessors, the old Mozilla Phoenix and Mozilla Firefox desktop browsers.

That said, Iceraven Browser is an independent all-volunteer project, and has no affiliation with Netscape, Netscape Navigator, Mozilla, Mozilla Firefox, Mozila Phoenix, Debian, Debian Iceweasel, Parabola GNU/Linux-libre Iceweasel, America Online, or Verizon, among others. :)  Basically, if you don't like the browser, it's not their fault. :)

## Installation

[**Download APKs from the Releases Page**](https://github.com/fork-maintainers/iceraven-browser/releases)

## 🔨 Building

1. Set up the environment. We need the Android SDK at `$ANDROID_SDK_ROOT` and a Java JDK at `$JAVA_HOME` that isn't the Ubuntu Java 8 one. We want environment variables that look something like:

```sh
# Where does our system install the JDK? This is the right path for the Ubuntu Java 11 JDK, if it is installed.
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
# Where did we install the Android SDK?
export ANDROID_SDK_ROOT=$HOME/android-sdk/android-sdk-linux/
```

If we don't have the Android SDK, we can install it thusly on Linux:

```sh
mkdir -p $HOME/android-sdk/android-sdk-linux
cd $HOME/android-sdk/android-sdk-linux
mkdir -p licenses
echo "8933bad161af4178b1185d1a37fbf41ea5269c55" >> licenses/android-sdk-license
echo "d56f5187479451eabf01fb78af6dfcb131a6481e" >> licenses/android-sdk-license
echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" >> licenses/android-sdk-license
mkdir cmdline-tools
cd cmdline-tools
wget "$(curl -s https://developer.android.com/studio | grep -oP "https://dl.google.com/android/repository/commandlinetools-linux-[0-9]+_latest.zip")"
unzip commandlinetools-linux-*_latest.zip
cd ..
```

2. Clone the project.

```sh
git clone https://github.com/fork-maintainers/iceraven-browser
```

4. Go inside `iceraven-browser`. That's where the build is coordinated from.

```sh
cd iceraven-browser
```

5. Configure the project. For your personal use you need to sign the apk file. The simplest way to do this is to use the debug key that is auto-generated by Android SDK. This is not a great idea for releasing, but acceptable for your personal use. You can configure it as follows:

```sh
echo "autosignReleaseWithDebugKey=" >>local.properties
```

6. Build the project. To build the Iceraven-branded release APKs, you can do:

```sh
./gradlew assembleForkRelease -PversionName="$(git describe --tags HEAD)"
```

The APKs will show up in `app/build/outputs/apk/forkRelease/`.

## Getting Involved

This is an all-volunteer project. No one is getting paid (at least not by the project itself.).

Therefore, everyone should feel free to open issues and pull requests.  Join the club!

Developers are especially welcome, wanted, and needed.

## I want to open a Pull Request!

We encourage you to participate in this open source project. We love Pull Requests, Bug Reports, ideas, (security) code reviews or any other kind of positive contribution.

### 🙅 How to skip CI checks for PRs 🙅

If you want to skip Github CI checks in a PR, please add the following to the PR title exactly: `[skip ci]`.
Also, please include the exact phrase `[skip ci]` in every commit message. This is to avoid Travis CI checks as well as skipping Github CI checks after merging the commits to the `fork` branch.

This is useful to do **if** you are sure that your changes do not effect the app's code (ex: changes to `README.md`).

## 🚀 Release automation 🚀

We have now setup release automation so that Github actions automatically trigger a release build and publish a release when we push a tag to the repository.

**NOTE**: The tag should be of the format `iceraven-x.y.z`, where `x.y.z` is the release version, for the automation to kick in and also so that the built app will have the correct version name.

## ✏️  I want to file an issue!

Great! We encourage you to participate in this open source project. We love Pull Requests, Bug Reports, ideas, (security) code reviews or any other kind of positive contribution.

To make it easier to triage, we have these issue requirements:

* Please do your best to search for duplicate issues before filing a new issue so we can keep our issue board clean.
* Every issue should have **exactly** one bug/feature request described in it. Please do not file meta feedback list tickets as it is difficult to parse them and address their individual points.
* Feature Requests are better when they’re open-ended instead of demanding a specific solution -ie  “I want an easier way to do X” instead of “add Y”
* Issues are not the place to go off topic or debate.
* While we do not yet have Community Participation Guidelines of our own, we ask that you show respect to everyone and treat others as you would like to be treated. Behavior that would violate [Mozilla's Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/) is almost certainly unwelcome. However, as a small project without community managers, we cannot promise prompt and consistent enforcement.

Please keep in mind that even though a feature you have in mind may seem like a small ask, as a small team, we have to prioritize our planned work and every new feature adds complexity and maintenance and may take up design, research, product, and engineering time. We appreciate everyone’s passion but we will not be able to incorporate every feature request or even fix every bug. That being said, just because we haven't replied, doesn't mean we don't care about the issue, please be patient with our response times as we're very busy.

## License


    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
