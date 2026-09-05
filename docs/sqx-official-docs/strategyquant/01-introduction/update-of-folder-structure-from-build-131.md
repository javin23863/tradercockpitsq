# Update of folder structure from Build 131

- Source: <https://strategyquant.com/doc/strategyquant/update-of-folder-structure-from-build-131/>
- Section: StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

Starting in Build 131 we are modifying a folder structure for user extensions.

***This is a technical change and if you are a “standard” StrategyQuant user and do not develop or use optional extensions available for example on our Codebase then this does not concern you at all.***

In Build 131 we are changing the folders for user extensions – code, snippets, plugins, and libraries – they will be all located in {StrategyQuant installation}/user folder.

These new user customizations folders are created:

/user/extend/Code  
/user/extend/Snippets  
/user/extend/Plugins  
/user/extend/Libs

The old location of extensions in **{StrategyQuant installation}/extend** was also moved to **{StrategyQuant installation}/internal** folder, t now contains only built-in extensions.

All extension files in **/internal** folder are **read-only and unmodifiable**, StrategyQuant will not load any user classes that are added there.  
User-customized content should be placed into the appropriate subfolder in **/user** folder.

**StrategyQuant CodeEditor** supports this automatically in Build131 – you can see both **Buildin** and **User** contents there, and imported extensions are stored into **/user** folder automatically.

**Note on migration / upgrade from older versions** – because of this change we recommend a clean installation of Build 131.

However, autoupdate should work – it will try to recognize custom extensions and migrate them to user folder.
