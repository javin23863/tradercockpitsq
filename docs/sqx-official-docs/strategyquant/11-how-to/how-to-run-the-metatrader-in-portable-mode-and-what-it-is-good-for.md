# How to run the Metatrader in portable mode and what it is good for?

- Source: <https://strategyquant.com/doc/strategyquant/how-to-run-the-metatrader-in-portable-mode-and-what-it-is-good-for/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

#### What is the portable version of Metatrader 4/5?

Portable is a portable program that can be booted from a portable device such as a flash drive, memory card or other disk location different from program files. The portable program is typically designed to store its configuration and user data in its own location and not on the system disk in the user’s data folder. In newer Windows, it can be usually found in:

“C Users ‘username’ appdata”, which is quite uncomfortable to work with. If we choose a portable version, we will have indicators, strategies, etc. in a folder such as C: mt4 MQL, which is more convenient.

#### What brings the portable version of Metatrader 4/5?

For me, it personally brings better overview and better handling of the MT4 platform. For example, I do not have to install MT4 every time, I can just copy the MT4/MT5 folder and use it for another account. Another advantage is greater simplicity when backing up the MT4 configuration or a possible shifting to another VPS – How to run Metatrader 4 on VPS.

Another advantage is that when I go for a holiday, I can have a MT4 backup on flash disk and if I there is a problem with my mobile or laptop, I can connect from any computer without installing MT4.

#### How to set up a portable Metatrader 4/5?

It’s fairly easy. In the shortcut icon/desktop icon just add / portable, as you can see bellow:

- Right click on the MT4/MT5 icon
- Select properties
- Add to the target / portable

After this change, save the changes and then run MT4 as a portable from the directory where we you run it. I recommend to do this change right after installing MT4, if you modify an already running MT4, you may need to copy the original user data from the MT4 app data directory, which I do not recommend because you could forget something and then wonder that you have lost your indicator etc.

[![](https://strategyquant.com/wp-content/uploads/2018/12/MT4_portable-1.jpg)](https://strategyquant.com/wp-content/uploads/2018/12/MT4_portable-1.jpg)

#### Potential issue – portable version doesn’t work

Please note that Windows blocking portable version if you are using  Program Files installation folder.  You have to use other folder, for example C:MT4portableMT4-1
