[![Release](https://jitpack.io/v/umjammer/vavi-sound-mucom88.svg)](https://jitpack.io/#umjammer/vavi-sound-mucom88)
[![Java CI](https://github.com/umjammer/vavi-sound-mucom88/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound-mucom88/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound-mucom88/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-sound-mucom88/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-25-b07219)

# vavi-sound-mucom88

<img alt="mucom88" src="https://github.com/user-attachments/assets/a8f0b794-ed79-487a-936d-2073007b91f1" width="240" />
© <img alt="koshiro" src="https://github.com/user-attachments/assets/bc7f823d-103b-4d85-8906-e8159977b9f3" width="50" />

🥁 Java version of mucom88.

this is a form of [mucomDotNET](https://github.com/kuma4649/mucomDotNET)

## Install

* [maven](https://jitpack.io/#umjammer/vavi-sound-mucom88)

## Usage

currently this project has no good player, use [vavi-sound-mdplayer](https://github.com/umjammer/vavi-sound-mdplayer) instead

## References

 * http://iwamoo.seesaa.net/article/496523476.html
 * https://github.com/DM-88mkII

### File Types

| name | type | status | desxription |
|------|------|:------:|-------------|
| MUS  | MML  |  ✅️?   |             |
| MUB  | SEQ  |   ✅️   |             |

## TODO

 * ~~compiler~~
   * works similarly to the original c# version, but both are NG  
 * adpcm?
 * ~~spi~~

---

# [Original](https://github.com/kuma4649/mucomDotNET)

## Overview

It is a port of mucom88 to the Java version.
OPNAx2, OPNBx2, OPMx1 can be used at the same time.
(It is a form that incorporates the W function of Boukichi-san. Thanks! Boukichi-san)
The functions of AMD98 are also included with the kindness of Koshiro-san.

Official page

 - [OPEN MUCOM PROJECT](https://github.com/onitama/mucom88) (Ancient Co., Ltd.)
 - [OPEN MUCOM88 Wiki](https://github.com/onitama/mucom88/wiki) (ONION software / Onitama-sama)

## Functions and features

 - One part can be divided into up to 10 pages.
 - You can create a mub that uses the full 64Kbyte for each page. unconfirmed.
 - Some mucom88 can use the omitted functions.

For this reason, playing data using them on mucom88 would be a strange situation.

(No measures have been taken so far.)

## Necessary environment

 - PC with Windows 7 or later OS installed
 - Text editor
 - Spirit and guts

## Copyright / Disclaimer

vavi-sound-mucom88 is a CC BY-NC-SA 4.0 license specified by Creative Commons.

 https://creativecommons.org/licenses/by-nc-sa/4.0/deed.ja

The copyright is owned by the author.
This software is not guaranteed and is due to the use of this software
The author does not take any responsibility for any damage.

The source code of the following software is modified and used for C #.
These sources are copyrighted by their respective authors.
Please refer to each document for the license.

 EncAdpcmA.cs Reference source: https://wiki.neogeodev.org/index.php?title=ADPCM_codecs

The source code of the following software is modified and used for C #.
Or I am using the code / dll.
These sources / binaries are copyrighted by their respective authors.
Please refer to each document for the license.

 - Mucom88 / mucom88win -> CC BY-NC-SA 4.0 -> Code modification
 - AMD98 -> ? -> Code modification

 - Used in MDSound -> LGPL -> dll dynamic linking
 - MusicDriverInterface -> MIT -> dll Used for dynamic linking
 - RealChipCtlWrap -> MIT -> dll Used for dynamic linking
 - NAudio -> MS-PL -> dll Used for dynamic linking
 - SCCI -> ? -> dll Used for dynamic linking
 - C86ctl -> BSD 3-Clause -> dll Used for dynamic linking

## Special Thanks

This tool is indebted to the following people. We also refer to and use the following software and web pages.

thank you very much.

 - Koshiro-san!!
 - Kuroma-san
 - mucom-san
 - Boukichi-san
 - TAN-Y-san
 - Yuki Nyan-san

 - mucom88/mucom88win
 - Music LALF
 - MXDRV
 - MNDRV
 - Visual Studio Community 2019
 - Sakura Editor

 - [mucom wiki](https://github.com/MUCOM88/mucom88/wiki)
 - [Boukichi-san's wiki](https://github.com/BouKiCHi/mucom88/wiki)

---

<sub>image by <a href="https://github.com/onitama/mucom88/wiki">official wiki</a></sub>