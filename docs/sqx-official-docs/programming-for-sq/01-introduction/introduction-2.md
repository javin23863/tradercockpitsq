# Introduction

- Source: <https://strategyquant.com/doc/programming-for-sq/introduction-2/>
- Section: Programming for StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

StrategyQuant version X was built from the scratch as an open, extendable platform.

Majority of the functionality is implemented using either plugins or snippets. What’s the difference between them:

- **Plugin** – is a bigger module that includes both some user interface and some background code. An example of plugin is the whole Builder screen, and it itself contains other subplugins – every settings tab and every result tab are another plugin.Development of plugins is not covered in this manual, it is very technical and complex and will be possibly offered in some future versions of StrategyQuant X.
- **Snippet** – is a “function” implementing one thing. For example – every Money management model is a snippet. Every indicator and building block is a snippet. Every databank column is a snippet.This allows you to extend StrategyQuant with your own indicators, statistics values, etc. in a relatively simple way.

Each Snippet is a short Java class that implements some function. We’ll give an example of most common snippets in this manual, to enable you to start using them.

Snippets are accessible through the CodeEditor icon on the top right corner.

[![](https://strategyquant.com/wp-content/uploads/2019/01/link_codeeditor.png)](https://strategyquant.com/wp-content/uploads/2019/01/link_codeeditor.png)

This will open CodeEditor panel where you can view, edit and create snippets.

[![](https://strategyquant.com/wp-content/uploads/2019/01/code_editor-1024x774.png)](https://strategyquant.com/wp-content/uploads/2019/01/code_editor-1024x774.png)

On the right side of the editor you see the tree structure of all SQ Snippets.

There are two main categories:

- **Code** – these are templates in [Freemarker](https://freemarker.apache.org/) marking language that are used to translate strategies from internal XML format into the target trading platform language – be it MQL, EasyLanguage or any other. When you’ll add new indicator or signal to SQ as a snippet, you also have to define its translation code here, so that SQ knows how to generate the proper code for it. Extending Code will be described along with examples of custom indicators.
- **Snippets** – are files written in Java, each implementing some function.

Snippets are organized hierarchically in categories.
