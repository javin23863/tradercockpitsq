# -databank Manage databanks

- Source: <https://strategyquant.com/doc/cli-command-line/databank-manage-databanks/>
- Section: CLI (command line) › Commands (SQ only)
- Fetched: 2026-09-04

---

Arguments:

**action**: Performs the specific action [list,count,save,load,delete,clear,create,remove,synctofiles,syncfromfiles,copy,move,export]

**project**: Project name

**name**: (optional) Databank name

**folder**: (optional) Path of the folder

**destproject**: (optional) Destination project

**destdatabank**: (optional) Destination databank

**file**: (optional) Path of the file to export databank contents

**view**: (optional) View name

**strategies**: (optional) Strategies splitted with a semicolumn

Examples:

```
sqcli.exe -databank action=list project=Builder
sqcli.exe -databank action=count project=Builder name=Results
sqcli.exe -databank action=save project=Builder name=Results folder=test
sqcli.exe -databank action=save project=Builder name=Results folder=test strategies="Strategy 0.1487,Strategy 0.1488"
sqcli.exe -databank action=load project=Builder name=Results folder=test
sqcli.exe -databank action=load project=Builder name=Results folder=test strategies="Strategy 0.1487,Strategy 0.1488"
sqcli.exe -databank action=delete project=Builder name=Results strategies="Strategy 0.1487,Strategy 0.1488"
sqcli.exe -databank action=clear project=Builder name=Results
sqcli.exe -databank action=create project=Retester name=Custom
sqcli.exe -databank action=remove project=Retester name=Custom
sqcli.exe -databank action=copy project=Builder name=Results destproject=Retester destdatabank=Results
sqcli.exe -databank action=synctofiles project=Builder name=Results
sqcli.exe -databank action=syncfromfiles project=Builder name=Results
sqcli.exe -databank action=move project=Builder name=Results destproject=Retester destdatabank=Results
sqcli.exe -databank action=export project=Builder name=Results file=C:/data/DatabankExport.csv
sqcli.exe -databank action=export project=Builder name=Results file=C:/data/DatabankExport.xlsx view=Custom
```
