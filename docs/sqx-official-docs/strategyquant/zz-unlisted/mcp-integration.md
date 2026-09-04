# MCP Integration

- Source: <https://strategyquant.com/doc/strategyquant/mcp-integration/>
- Section: StrategyQuant X
- Fetched: 2026-09-04

---

## What is MCP

The **Model Context Protocol (MCP)** is an open standard that lets AI assistants — such as Claude, OpenAI, or Gemini — connect to external applications and interact with them through well-defined tools. **StrategyQuant X ships a built-in MCP server** that exposes your projects, strategies, and databanks directly to any MCP-compatible AI client.

This is the **first release** of the SQX MCP integration. The current toolset covers project management and strategy inspection; more tools will be added in future builds.

## Enable MCP in SQX

The MCP server starts automatically with SQX. It shares the port with Remote Access — by default **8080**, or the first available port on your machine. The exact URL is always shown in the MCP Server dialog: open the application menu (gear icon, top-right) and click **MCP Server…**

![](https://strategyquant.com/wp-content/uploads/2026/05/mcp-sqx.jpg)

Application menu — click MCP Server… to open the connection dialog.

The dialog shows the server URL and the exact commands needed to connect your AI client.

![](https://strategyquant.com/wp-content/uploads/2026/05/mcp-dialog.jpg)

MCP Server dialog — copy the JSON snippet for your AI client config.

## Connect an AI client

### Claude Desktop

Add the following block to your

```
claude_desktop_config.json
```

 file (find it under *Settings → Developer* inside Claude Desktop):

```
{
  "mcpServers": {
    "sqx": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

Restart Claude Desktop after saving the file. The SQX tools will appear automatically in the next conversation.

## Available tools

Once connected, your AI client has access to the following SQX tools. You can list them at any time by typing list tools of sqx mcp in Claude Code.

![](https://strategyquant.com/wp-content/uploads/2026/05/mcp-tools.jpg)

Claude Code showing all tools exposed by the SQX MCP server.

| Tool | Description |
| --- | --- |
| list\_projects | List all available SQX projects. |
| list\_strategies | List strategies inside a given project. |
| list\_databanks | List all databanks in a project. |
| get\_strategy\_stats | Return performance statistics for a specific strategy (fitness, net profit, number of trades, Sharpe ratio, drawdown, and more). |
| run\_project | Start running a project (e.g. launch the Builder). |
| stop\_project | Stop a currently running project. |

## Usage examples

### Running a project

Ask Claude to find and start a project by name. Claude will list the available projects, identify the right one, and call run\_project — all in a single conversation turn.

![](https://strategyquant.com/wp-content/uploads/2026/05/mcp-run-project.jpg)

Claude locates the Builder project and starts it. SQX begins generating strategies in the background.

### Inspecting strategy statistics

Ask Claude to list strategies from a databank and retrieve detailed stats for any of them. The results are presented as a structured table directly in the conversation.

![](https://strategyquant.com/wp-content/uploads/2026/05/mcp-strategy-stats.jpg)

Claude lists strategies from the Results databank and returns in-sample statistics for a selected strategy.
