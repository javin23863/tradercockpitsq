import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const webRoot = path.dirname(fileURLToPath(import.meta.url));
const port = Number(process.env.PORT || 4173);
const contentTypes = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
};

function safeFilePath(requestUrl) {
  const requestPath = decodeURIComponent(new URL(requestUrl, "http://localhost").pathname);
  const relativePath = requestPath === "/" || !path.extname(requestPath)
    ? "index.html"
    : requestPath.replace(/^\/+/, "");
  const resolved = path.resolve(webRoot, relativePath);
  return resolved.startsWith(webRoot + path.sep) ? resolved : null;
}

const server = createServer(async (request, response) => {
  try {
    const filePath = safeFilePath(request.url || "/");
    if (!filePath) {
      response.writeHead(400, { "content-type": "text/plain; charset=utf-8" });
      response.end("Invalid path");
      return;
    }

    const body = await readFile(filePath);
    response.writeHead(200, {
      "cache-control": "no-store",
      "content-type": contentTypes[path.extname(filePath)] || "application/octet-stream",
    });
    response.end(body);
  } catch (error) {
    const status = error.code === "ENOENT" ? 404 : 500;
    response.writeHead(status, { "content-type": "text/plain; charset=utf-8" });
    response.end(status === 404 ? "Not found" : "Server error");
  }
});

server.listen(port, "127.0.0.1", () => {
  console.log(`TraderCockpit shell listening on http://127.0.0.1:${port}`);
});
