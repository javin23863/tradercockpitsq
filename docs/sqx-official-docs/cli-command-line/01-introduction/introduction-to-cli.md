# Introduction to CLI

- Source: <https://strategyquant.com/doc/cli-command-line/introduction-to-cli/>
- Section: CLI (command line) › Introduction
- Fetched: 2026-09-04

---

# What is CLI?

CLI (Command Line Interface) allows you to run and interact with StrategyQuant / QuantDataManager from command line.

It can be used also to run SQ / QDM from batch scripts and external programs.

Note – improved CLI is avaiable in StrategyQuant from Build 127 and in Quant Data Manager from Build 119.   
There is a more limited set of commands in earlier versions of QDM for the moment.

Our focus with CLI is to make it fully featured way to interact with the program, so that you can control SQ / QDM using the commands from external scripts or external programs.

**SQ CLI** is available as a new **sqcli.exe** in StrategyQuant installation folder,  
**QDM CLI** is available as **qdmcli.exe**.

Note – We mostly use sqcli.exe in the documentation example, if you use QuantDataManager simply replace it with qdmcli.exe.

# Running from command line

You can execute a CLI command from command line or batch file. For example the following command will run CLI command to update all the data

```
sqcli.exe -data action=update
```

or

```
qdmcli.exe -data action=update
```

and then it finishes.

# Running in interactive mode

Start sqcli.exe without any parameters. It wil open CLI window:

[![](https://strategyquant.com/wp-content/uploads/2020/04/sqcli-1-750x440.png)](https://strategyquant.com/wp-content/uploads/2020/04/sqcli-1-750x440.png)

When CLI is started this way it will start StartegyQuant engine in the backgroound, without an user interface. You can type commands to perform, for example typing

```
-data action=update
```

updates all the data.

Type **-h** to list all the available commands.

When closing interactive CLI please use command **-exit** so that it exits correctly.

# Redirecting output to a file

when you execute some command you might want to log the output. There is a special command ***> filename*** for that.

Example in command line CLI:

```
sqcli.exe -symbol action=list > C:/reports/output.log
```

The same in interactive CLI:

```
-symbol action=list > C:/reports/output.log
```

# Running multiple commands

When you want to run multiple consecutive commands, you can use a special command ***-run filename***

which will load and execute all the CLi commands in the filename.

You can start it for example like:

```
sqcli.exe -run file=C:/commands.txt
```

and the file **C:/commands.txt** can contain multiple CLI commands separated by line that will be executed one by one:

```
-data action=update
-symbol action=list > C:/symbols.log
-databank action=list project=Builder   > C:/databanks.log
```
