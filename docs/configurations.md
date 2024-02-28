
## Generic

### Executable

For the plugin to work, you need to define at least one executable file
using either the <b>Global</b> or <b>Project</b> panel.

Such a file is typically named `pyright.exe` and can be found in:

* Global: Your Python interpreter directory
* Virtual environment: `/venv/Scripts/` (Windows) or `/venv/bin` (Linux)

You can also use a relative path.
It would be interpreted as relative to the project directory.


### Configuration file

Despite being called "file", you can specify a path to a directory
containing `pyright-config.json` and/or `pyproject.toml`.
This path will be passed to the executable via [the `-p` option][1].

* If the executable is local, only the local path is used.
* If the executable is global, the local path is used if it is specified,
  falling back to the global one.

If neither paths are specified, the project directory is used.


## Global

### Always use global

Check this option to always use the global executable and configuration file.


  [1]: https://microsoft.github.io/pyright/#/command-line