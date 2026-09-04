# Import / Export custom indicators and other snippets

- Source: <https://strategyquant.com/doc/programming-for-sq/import-export-custom-indicators-and-other-snippets/>
- Section: Programming for StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

StrategyQuant CodeEditor allows you to simply export custom indicators and other snippets you created, and then import them on another computer or send it to another user for import.

Note – only custom snippets can be exported, because default set of snippets is the same on all SQ installations.

## Exporting indicators and other snippets

Let’s try to export two indicators created in [ForceIndex article](https://strategyquant.com/codebase/forceindex/).

First, go to Code Editor and there choose Import/Export -> Export extensions in the top menu:

[![](https://strategyquant.com/wp-content/uploads/2019/03/indy_export.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/indy_export.jpg)

this will open a dialogue where you can select which snippets you want to export. You can export multiple of them. In your example we’ll choose MA and ForceIndex snippets.

[![](https://strategyquant.com/wp-content/uploads/2019/03/dlailog_export.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/dlailog_export.jpg)

Now just choose folder and file name of the export in the file dialog:

[![](https://strategyquant.com/wp-content/uploads/2019/03/filedialog_export.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/filedialog_export.jpg)

We’ll name the file ForceIndex\_export. Click **Select** and your selected snippets will be exported to this file.

The file will have an .sxp extension. You can now use this file to import the indicators on another SQ installation, or send the file to another user to import them.

## Importing snippets

Importing indicators and other nsippets is equally simple. Go to Code Editor, and select Import/Export -> Import extensions:

[![](https://strategyquant.com/wp-content/uploads/2019/03/indy_import.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/indy_import.jpg)

Now choose the file to be imported in the file dialog:

[![](https://strategyquant.com/wp-content/uploads/2019/03/filedialog_import.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/filedialog_import.jpg)

and click Select. All the snippets in this file will be imported.

When it is done, you can see them in Navigator (you might need to Refresh it first):

[![](https://strategyquant.com/wp-content/uploads/2019/03/indys_imported.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/indys_imported.jpg)

Now you have the snippets in your SQ code base. Hit **Compile all** to recompile all the snippets and you’ll be able to use them in SQ or in AlgoWizard.
