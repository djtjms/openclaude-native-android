# OpenClaude Plugins

Plugins extend OpenClaude with custom skills, task hooks, and tool integrations. Each plugin is a directory with a `plugin.json` manifest and optional scripts.

## Plugin Structure

```
plugins/
└── my-plugin/
    ├── plugin.json          # Manifest (required)
    ├── run.sh               # Entry script (required by manifest)
    └── assets/              # Optional assets
```

## Plugin Manifest (`plugin.json`)

```json
{
  "name": "hello-world",
  "version": "1.0.0",
  "description": "A simple hello-world plugin",
  "commands": ["hello"],
  "entry": "run.sh"
}
```

- `name` — unique plugin identifier
- `version` — semver string
- `description` — short description
- `commands` — list of command names this plugin handles
- `entry` — script to execute (relative to plugin directory)

## Installation

Copy or symlink plugins into the OpenClaude skills directory:

```bash
cp -r plugins/* ~/.openclaude/skills/
```

Or install individually:

```bash
ln -s $(pwd)/plugins/hello-world ~/.openclaude/skills/hello-world
```

## Writing a Plugin

The entry script receives the command and arguments as parameters. It can read from stdin and write to stdout. Example:

```bash
#!/usr/bin/env bash
echo "Hello from $(basename $0)! You said: $@"
```

## Sample Plugin

See `hello-world/` for a minimal working example.