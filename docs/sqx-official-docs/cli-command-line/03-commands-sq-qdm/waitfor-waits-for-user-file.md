# -waitfor Waits for user/file

- Source: <https://strategyquant.com/doc/cli-command-line/waitfor-waits-for-user-file/>
- Section: CLI (command line) › Commands (SQ & QDM)
- Fetched: 2026-09-04

---

Waits for user pressing a key or for a file to be created on a given path. This can be used for example when calling external script and waiting until it produces some result.

Arguments:

**action**: Waits for action [user,file]

**file**: Path of the file or folder

Example:

```
sqcli.exe -waitfor action=user
sqcli.exe -waitfor action=file file=C:/reports/controlfile.txt
```
