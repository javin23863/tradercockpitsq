# -project Manage projects

- Source: <https://strategyquant.com/doc/cli-command-line/project-manage-projects/>
- Section: CLI (command line) › Commands (SQ only)
- Fetched: 2026-09-04

---

Arguments:

**action**: Performs the specific action [list,start,stop,pause,resume,remove,status,loadconfig,saveconfig]

**name**: (optional) Project name

**file**: (optional) Path of the config file

Examples:

```
sqcli.exe -project action=list
sqcli.exe -project action=start name=Builder
sqcli.exe -project action=stop name=Builder
sqcli.exe -project action=pause name=Builder
sqcli.exe -project action=resume name=Builder
sqcli.exe -project action=remove name=Custom
sqcli.exe -project action=status name=Builder
sqcli.exe -project action=loadconfig name=Builder file=Builder.cfx
sqcli.exe -project action=saveconfig name=Builder file=Builder.cfx
```
